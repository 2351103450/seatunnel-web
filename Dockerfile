FROM alpine:3.20 AS dist

WORKDIR /opt/seatunnel-web

COPY seatunnel-web-dist/target/seatunnel-web-*.tar.gz /tmp/seatunnel-web.tar.gz

RUN tar -xzf /tmp/seatunnel-web.tar.gz \
        --strip-components=1 \
        -C /opt/seatunnel-web \
    && test -f /opt/seatunnel-web/libs/seatunnel-web-api.jar \
    && test -f /opt/seatunnel-web/web/index.html


FROM eclipse-temurin:21-jre-jammy AS backend-runtime

ENV SEATUNNEL_WEB_HOME=/opt/seatunnel-web \
    JAVA_OPTS="" \
    APP_OPTS=""

WORKDIR ${SEATUNNEL_WEB_HOME}

COPY --from=dist /opt/seatunnel-web/bin ./bin
COPY --from=dist /opt/seatunnel-web/conf ./conf
COPY --from=dist /opt/seatunnel-web/libs ./libs
COPY --from=dist /opt/seatunnel-web/sql ./sql
COPY --from=dist /opt/seatunnel-web/jdbc-drivers ./jdbc-drivers

RUN chmod +x ./bin/*.sh \
    && mkdir -p ./logs

EXPOSE 9527

VOLUME ["/opt/seatunnel-web/logs", "/opt/seatunnel-web/jdbc-drivers"]

ENTRYPOINT ["/opt/seatunnel-web/bin/run-seatunnel-web.sh"]


FROM nginx:1.27-alpine AS frontend-runtime

COPY --from=dist \
     /opt/seatunnel-web/conf/nginx/default.conf \
     /etc/nginx/conf.d/default.conf

COPY --from=dist \
     /opt/seatunnel-web/web/ \
     /usr/share/nginx/html/

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]