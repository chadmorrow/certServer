ARG VERSION=8u191

FROM openjdk:${VERSION}-jdk-alpine3.8 as BUILD

COPY . /src
WORKDIR /src

RUN ./gradlew --no-daemon shadowJar

FROM openjdk:${VERSION}-jre-alpine3.8

ENV APPLICATION_USER ktor
RUN adduser -D -g '' $APPLICATION_USER

RUN mkdir /app
RUN chown -R $APPLICATION_USER /app

USER $APPLICATION_USER

COPY --from=BUILD /src/build/libs/certServer-0.0.1.jar /app/run.jar
WORKDIR /app

CMD ["java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-XX:InitialRAMFraction=2", "-XX:MinRAMFraction=2", "-XX:MaxRAMFraction=2", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "run.jar"]