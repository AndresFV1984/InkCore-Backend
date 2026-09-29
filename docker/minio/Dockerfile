# MinIO Community ya no publica imágenes.
# Docker Hub (minio/minio) se eliminó en septiembre de 2026.
# quay.io/minio/minio se quedó en RELEASE.2025-09-07, sin el parche
# CVE-2025-62506. El mantenimiento oficial indica construir el contenedor
# desde el tag de seguridad:
# https://github.com/minio/minio/releases/tag/RELEASE.2025-10-15T17-29-55Z
# https://github.com/minio/minio/issues/21647

FROM golang:1.24.8-bookworm AS server
ARG MINIO_TAG=RELEASE.2025-10-15T17-29-55Z
RUN apt-get update \
    && apt-get install -y --no-install-recommends git ca-certificates \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /src
RUN git clone --depth 1 --branch "${MINIO_TAG}" https://github.com/minio/minio.git .
RUN CGO_ENABLED=0 go build -tags kqueue -trimpath \
        --ldflags "$(go run buildscripts/gen-ldflags.go)" \
        -o /out/minio .

FROM debian:bookworm-slim
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=server /out/minio /usr/bin/minio
COPY docker-entrypoint.sh /usr/bin/docker-entrypoint.sh
RUN chmod +x /usr/bin/minio /usr/bin/docker-entrypoint.sh
EXPOSE 9000 9001
VOLUME ["/data"]
ENTRYPOINT ["/usr/bin/docker-entrypoint.sh"]
CMD ["minio"]
