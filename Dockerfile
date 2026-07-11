# =========================
# Distribution artifact
# =========================
FROM scratch AS dist-artifact

COPY seatunnel-web-dist/target/seatunnel-web-*.tar.gz \
     /seatunnel-web.tar.gz


# =========================
# Backend runtime
# =========================
FROM eclipse-temurin:21-jre-jammy AS backend-runtime

LABEL org.opencontainers.image.title="SeaTunnel Web API" \
      org.opencontainers.image.description="SeaTunnel Web backend service" \
      org.opencontainers.image.licenses="Apache-2.0"

ENV SEATUNNEL_WEB_HOME=/opt/seatunnel-web \
    JAVA_OPTS="" \
    APP_OPTS="" \
    SERVER_PORT=9527 \
    SPRING_PROFILES_ACTIVE=mysql \
    SPRING_DATASOURCE_URL="jdbc:mysql://mysql:3306/seatunnel_web?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true" \
    SPRING_DATASOURCE_USERNAME=seatunnel \
    SPRING_DATASOURCE_PASSWORD=seatunnel

WORKDIR ${SEATUNNEL_WEB_HOME}

COPY --from=dist-artifact \
     /seatunnel-web.tar.gz \
     /tmp/seatunnel-web.tar.gz

RUN set -eux; \
    mkdir -p "${SEATUNNEL_WEB_HOME}"; \
    tar -xzf /tmp/seatunnel-web.tar.gz \
        --strip-components=1 \
        -C "${SEATUNNEL_WEB_HOME}"; \
    rm -f /tmp/seatunnel-web.tar.gz; \
    chmod +x "${SEATUNNEL_WEB_HOME}"/bin/*.sh; \
    mkdir -p \
        "${SEATUNNEL_WEB_HOME}/logs" \
        "${SEATUNNEL_WEB_HOME}/jdbc-drivers"; \
    test -f "${SEATUNNEL_WEB_HOME}/libs/seatunnel-web-api.jar"

EXPOSE 9527

VOLUME ["/opt/seatunnel-web/logs", "/opt/seatunnel-web/jdbc-drivers"]

ENTRYPOINT ["/opt/seatunnel-web/bin/run-seatunnel-web.sh"]


# =========================
# Frontend runtime
# =========================
FROM nginx:latest AS frontend-runtime

LABEL org.opencontainers.image.title="SeaTunnel Web" \
      org.opencontainers.image.description="SeaTunnel Web frontend and reverse proxy" \
      org.opencontainers.image.licenses="Apache-2.0"

COPY --from=dist-artifact \
     /seatunnel-web.tar.gz \
     /tmp/seatunnel-web.tar.gz

RUN set -eux; \
    mkdir -p /tmp/seatunnel-web; \
    tar -xzf /tmp/seatunnel-web.tar.gz \
        --strip-components=1 \
        -C /tmp/seatunnel-web; \
    test -f /tmp/seatunnel-web/web/index.html; \
    test -f /tmp/seatunnel-web/conf/nginx/default.conf; \
    rm -rf /usr/share/nginx/html/*; \
    cp -r /tmp/seatunnel-web/web/. /usr/share/nginx/html/; \
    cp /tmp/seatunnel-web/conf/nginx/default.conf \
       /etc/nginx/conf.d/default.conf; \
    rm -rf /tmp/seatunnel-web /tmp/seatunnel-web.tar.gz

EXPOSE 80

HEALTHCHECK \
    --interval=10s \
    --timeout=3s \
    --start-period=10s \
    --retries=6 \
    CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1

CMD ["nginx", "-g", "daemon off;"]