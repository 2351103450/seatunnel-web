# syntax=docker/dockerfile:1.7

FROM node:20-alpine AS ui-builder
WORKDIR /workspace/seatunnel-web-ui
COPY seatunnel-web-ui/package.json seatunnel-web-ui/yarn.lock ./
RUN yarn install --frozen-lockfile
COPY seatunnel-web-ui/ ./
RUN yarn build

FROM eclipse-temurin:21-jdk-jammy AS backend-builder
WORKDIR /workspace
COPY . .
COPY --from=ui-builder /workspace/seatunnel-web-ui/dist ./seatunnel-web-ui/dist
RUN ./mvnw -B -T 1C clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
LABEL org.opencontainers.image.title="SeaTunnel Web" \
      org.opencontainers.image.description="A modern Web UI for Apache SeaTunnel" \
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
COPY --from=backend-builder /workspace/seatunnel-web-dist/target/seatunnel-web-*.tar.gz /tmp/seatunnel-web.tar.gz
RUN set -eux; \
    tar -xzf /tmp/seatunnel-web.tar.gz --strip-components=1 -C ${SEATUNNEL_WEB_HOME}; \
    rm /tmp/seatunnel-web.tar.gz; \
    mkdir -p ${SEATUNNEL_WEB_HOME}/logs

EXPOSE 9527
VOLUME ["/opt/seatunnel-web/logs"]

CMD ["sh", "-c", "exec java ${JAVA_OPTS} -Dseatunnel.web.home=${SEATUNNEL_WEB_HOME} -Dlogging.config=${SEATUNNEL_WEB_HOME}/conf/logback-spring.xml -jar ${SEATUNNEL_WEB_HOME}/libs/seatunnel-web-api.jar --spring.config.location=${SEATUNNEL_WEB_HOME}/conf/application.yml ${APP_OPTS}"]
