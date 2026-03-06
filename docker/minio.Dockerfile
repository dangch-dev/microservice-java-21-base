FROM minio/mc:latest AS mc

FROM minio/minio:latest

COPY --from=mc /usr/bin/mc /usr/bin/mc
COPY docker/minio-entrypoint.sh /usr/local/bin/minio-entrypoint.sh

RUN chmod +x /usr/local/bin/minio-entrypoint.sh

ENTRYPOINT ["/usr/local/bin/minio-entrypoint.sh"]
CMD ["server", "/data", "--address", ":9000", "--console-address", ":9001"]
