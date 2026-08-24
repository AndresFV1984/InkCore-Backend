#!/bin/sh
# Root: bucket + CORS + ILM. Luego usuario de app solo con CRUD de objetos.
# Idempotente. Rotación: cambiar OBJECT_STORAGE_SECRET_KEY, borrar el usuario y re-ejecutar.
# minio/mc no incluye sed: plantillas con cat + reemplazo POSIX.
set -eu

MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://minio:9000}"
MINIO_ROOT_USER="${MINIO_ROOT_USER:-minioadmin}"
MINIO_ROOT_PASSWORD="${MINIO_ROOT_PASSWORD:-minioadmin}"
OBJECT_STORAGE_ACCESS_KEY="${OBJECT_STORAGE_ACCESS_KEY:-inkcore-app}"
OBJECT_STORAGE_SECRET_KEY="${OBJECT_STORAGE_SECRET_KEY:?OBJECT_STORAGE_SECRET_KEY es obligatorio}"
OBJECT_STORAGE_BUCKET="${OBJECT_STORAGE_BUCKET:-inkcore}"
OBJECT_STORAGE_CORS_ORIGINS="${OBJECT_STORAGE_CORS_ORIGINS:-http://localhost:8085,http://127.0.0.1:8085}"
STAGING_EXPIRY_DAYS="${OBJECT_STORAGE_STAGING_EXPIRY_DAYS:-2}"
STAGING_FALLBACK_DAYS="${OBJECT_STORAGE_STAGING_FALLBACK_EXPIRY_DAYS:-30}"
POLICY_NAME="${MINIO_APP_POLICY_NAME:-inkcore-app}"
POLICY_SRC="${POLICY_SRC:-/config/inkcore-app-policy.json}"
LIFECYCLE_SRC="${LIFECYCLE_SRC:-/config/lifecycle.json.template}"

# Reemplaza todas las ocurrencias de needle en haystack (POSIX, sin sed).
replace_all() {
  haystack=$1
  needle=$2
  replacement=$3
  result=
  while :; do
    case $haystack in
      *"$needle"*)
        prefix=${haystack%%"$needle"*}
        haystack=${haystack#*"$needle"}
        result=$result$prefix$replacement
        ;;
      *)
        result=$result$haystack
        break
        ;;
    esac
  done
  printf '%s' "$result"
}

render_template() {
  src=$1
  dest=$2
  content=$(cat "$src")
  shift 2
  while [ "$#" -ge 2 ]; do
    content=$(replace_all "$content" "$1" "$2")
    shift 2
  done
  printf '%s\n' "$content" > "$dest"
}

trim() {
  s=$1
  while [ -n "$s" ]; do
    case $s in
      [[:space:]]*) s=${s#?} ;;
      *) break ;;
    esac
  done
  while [ -n "$s" ]; do
    case $s in
      *[[:space:]]) s=${s%?} ;;
      *) break ;;
    esac
  done
  printf '%s' "$s"
}

if [ "${OBJECT_STORAGE_ACCESS_KEY}" = "${MINIO_ROOT_USER}" ]; then
  echo "ERROR: OBJECT_STORAGE_ACCESS_KEY no puede ser el root de MinIO (${MINIO_ROOT_USER}). Use un usuario de aplicación (p. ej. inkcore-app)."
  exit 1
fi

echo "Esperando MinIO en ${MINIO_ENDPOINT}..."
i=0
while [ "$i" -lt 60 ]; do
  if mc alias set local "${MINIO_ENDPOINT}" "${MINIO_ROOT_USER}" "${MINIO_ROOT_PASSWORD}" >/dev/null 2>&1; then
    if mc ready local >/dev/null 2>&1; then
      break
    fi
  fi
  i=$((i + 1))
  sleep 2
done
mc alias set local "${MINIO_ENDPOINT}" "${MINIO_ROOT_USER}" "${MINIO_ROOT_PASSWORD}"
mc ready local

mc mb --ignore-existing "local/${OBJECT_STORAGE_BUCKET}" || mc mb "local/${OBJECT_STORAGE_BUCKET}" || true

ORIGINS_CSV=
first=1
OLDIFS=$IFS
IFS=','
for o in ${OBJECT_STORAGE_CORS_ORIGINS}; do
  o=$(trim "$o")
  [ -z "$o" ] && continue
  if [ "$first" = 1 ]; then
    first=0
    ORIGINS_CSV="$o"
  else
    ORIGINS_CSV="${ORIGINS_CSV},${o}"
  fi
done
IFS=$OLDIFS

if [ "$first" = 0 ]; then
  # MinIO Community: CORS global (api cors_allow_origin). mc cors set por bucket = AIStor de pago.
  if mc admin config set local api "cors_allow_origin=${ORIGINS_CSV}" >/dev/null 2>&1; then
    echo "CORS global aplicado (api cors_allow_origin=${ORIGINS_CSV})"
  else
    echo "WARN: no se pudo aplicar CORS global. Configure MINIO_API_CORS_ALLOW_ORIGIN en el servicio MinIO. Orígenes=${ORIGINS_CSV}"
  fi
fi

if [ "${STAGING_EXPIRY_DAYS}" -gt 0 ] || [ "${STAGING_FALLBACK_DAYS}" -gt 0 ]; then
  TAG_DAYS="${STAGING_EXPIRY_DAYS}"
  FALLBACK_DAYS="${STAGING_FALLBACK_DAYS}"
  [ "${TAG_DAYS}" -gt 0 ] || TAG_DAYS=2
  [ "${FALLBACK_DAYS}" -gt 0 ] || FALLBACK_DAYS=30
  LIFE_FILE="/tmp/inkcore-lifecycle.json"
  render_template "${LIFECYCLE_SRC}" "${LIFE_FILE}" \
    "__TAG_DAYS__" "${TAG_DAYS}" \
    "__FALLBACK_DAYS__" "${FALLBACK_DAYS}"
  if mc ilm import "local/${OBJECT_STORAGE_BUCKET}" < "${LIFE_FILE}" >/dev/null 2>&1; then
    echo "Lifecycle ILM importado (tag=${TAG_DAYS}d prefix tmp/=${FALLBACK_DAYS}d)"
  elif mc ilm rule add "local/${OBJECT_STORAGE_BUCKET}" --expire-days "${TAG_DAYS}" \
        --tags "inkcore-staging=true" >/dev/null 2>&1; then
    mc ilm rule add "local/${OBJECT_STORAGE_BUCKET}" --expire-days "${FALLBACK_DAYS}" --prefix "tmp/" >/dev/null 2>&1 || true
    echo "Lifecycle ILM aplicado con mc ilm rule add"
  else
    echo "WARN: no se pudo aplicar lifecycle ILM"
  fi
fi

POLICY_FILE="/tmp/inkcore-policy.json"
render_template "${POLICY_SRC}" "${POLICY_FILE}" "__BUCKET__" "${OBJECT_STORAGE_BUCKET}"

if mc admin policy create local "${POLICY_NAME}" "${POLICY_FILE}" 2>/dev/null; then
  echo "Política ${POLICY_NAME} creada"
elif mc admin policy add local "${POLICY_NAME}" "${POLICY_FILE}" 2>/dev/null; then
  echo "Política ${POLICY_NAME} creada (mc legacy)"
else
  echo "Política ${POLICY_NAME} ya existía o se reutiliza"
fi

if mc admin user info local "${OBJECT_STORAGE_ACCESS_KEY}" >/dev/null 2>&1; then
  echo "Usuario ${OBJECT_STORAGE_ACCESS_KEY} ya existe"
else
  mc admin user add local "${OBJECT_STORAGE_ACCESS_KEY}" "${OBJECT_STORAGE_SECRET_KEY}"
  echo "Usuario ${OBJECT_STORAGE_ACCESS_KEY} creado"
fi

if mc admin policy attach local "${POLICY_NAME}" --user "${OBJECT_STORAGE_ACCESS_KEY}" >/dev/null 2>&1 \
    || mc admin policy set local "${POLICY_NAME}" "user=${OBJECT_STORAGE_ACCESS_KEY}" >/dev/null 2>&1; then
  echo "Política ${POLICY_NAME} asignada a ${OBJECT_STORAGE_ACCESS_KEY}"
else
  echo "Política ${POLICY_NAME}: assign omitido (puede estar ya aplicada)"
fi
mc admin user info local "${OBJECT_STORAGE_ACCESS_KEY}" >/dev/null

echo "MinIO listo: bucket=${OBJECT_STORAGE_BUCKET} user=${OBJECT_STORAGE_ACCESS_KEY} (CRUD objetos; CORS/ILM con root)"
