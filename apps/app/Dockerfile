FROM eclipse-temurin:21-jdk-alpine@sha256:2f2f553ce09d25e2d2f0f521ab94cd73f70c9b21327a29149c23a2b63b8e29a0

ENV USER_ID=150 \
    USER_NAME=apprunner \
    TZ=Europe/Oslo \
    APP_HOME=/app

RUN addgroup -g ${USER_ID} ${USER_NAME} \
    && adduser -u ${USER_ID} -G ${USER_NAME} -D ${USER_NAME}

RUN mkdir -p ${APP_HOME}
COPY --chown=${USER_ID}:${USER_ID} build/libs/app.jar ${APP_HOME}/app.jar

USER ${USER_NAME}
WORKDIR ${APP_HOME}

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]