# Cliente MinIO (mc) + script InkCore. Publicar: bayronindicore/inkcore-minio-init
FROM minio/mc:latest
WORKDIR /config
COPY init-app-user.sh inkcore-app-policy.json lifecycle.json.template /config/
RUN tr -d '\r' < /config/init-app-user.sh > /tmp/init-app-user.sh \
    && mv /tmp/init-app-user.sh /config/init-app-user.sh \
    && chmod 0755 /config/init-app-user.sh
ENTRYPOINT ["/bin/sh", "/config/init-app-user.sh"]
