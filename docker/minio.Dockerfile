FROM minio/mc:latest AS mc

FROM minio/minio:latest

COPY --from=mc /usr/bin/mc /usr/bin/mc
