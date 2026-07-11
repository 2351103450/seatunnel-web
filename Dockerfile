# syntax=docker/dockerfile:1.7

# =========================
# 1. Build frontend
# =========================
FROM node:20-alpine AS ui-builder

ENV HUSKY=0

WORKDIR /workspace/seatunnel-web-ui

COPY seatunnel-web-ui/package.json \
     seatunnel-web-ui/yarn.lock ./

RUN yarn install \
    --frozen-lockfile \
    --non-interactive

COPY seatunnel-web-ui/ ./

RUN yarn build


# =========================
# 2. Build backend
# =========================
FROM eclipse-temurin:21-jdk-jammy AS backend-builder

WORKDIR /workspace

COPY . .

# Maven assembly currently reads seatunnel-web-ui/dist
COPY --from=ui-builder \
     /workspace/seatunnel-web-ui/dist \
     ./seatunnel-web-ui/dist

RUN chmod +x ./mvnw \
    && ./mvnw -B -T 1C clean package -DskipTests


# =========================
# 3. Backend runtime image
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

COPY --from=backend-builder \
     /workspace/seatunnel-web-dist/target/seatunnel-web-*.tar.gz \
     /tmp/seatunnel-web.tar.gz

RUN set -eux; \
    tar -xzf /tmp/seatunnel-web.tar.gz \
        --strip-components=1 \
        -C ${SEATUNNEL_WEB_HOME}; \
    rm -f /tmp/seatunnel-web.tar.gz; \
    mkdir -p \
        ${SEATUNNEL_WEB_HOME}/logs \
        ${SEATUNNEL_WEB_HOME}/jdbc-drivers

EXPOSE 9527

VOLUME [
  "/opt/seatunnel-web/logs",
  "/opt/seatunnel-web/jdbc-drivers"
]

HEALTHCHECK \
    --interval=10s \
    --timeout=3s \
    --start-period=30s \
    --retries=12 \
    CMD bash -c "</dev/tcp/127.0.0.1/9527" || exit 1

CMD ["sh", "-c", "exec java ${JAVA_OPTS} \
-Dseatunnel.web.home=${SEATUNNEL_WEB_HOME} \
-Dlogging.config=${SEATUNNEL_WEB_HOME}/conf/logback-spring.xml \
-jar ${SEATUNNEL_WEB_HOME}/libs/seatunnel-web-api.jar \
--spring.config.location=${SEATUNNEL_WEB_HOME}/conf/application.yml \
${APP_OPTS}"]


# =========================
# 4. Frontend runtime image
# =========================
FROM nginx:1.27-alpine AS frontend-runtime

LABEL org.opencontainers.image.title="SeaTunnel Web" \
      org.opencontainers.image.description="SeaTunnel Web frontend and reverse proxy" \
      org.opencontainers.image.licenses="Apache-2.0"

COPY docker/nginx/default.conf \
     /etc/nginx/conf.d/default.conf

COPY --from=ui-builder \
     /workspace/seatunnel-web-ui/dist/ \
     /usr/share/nginx/html/

EXPOSE 80

HEALTHCHECK \
    --interval=10s \
    --timeout=3s \
    --start-period=10s \
    --retries=6 \
    CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1

CMD ["nginx", "-g", "daemon off;"]