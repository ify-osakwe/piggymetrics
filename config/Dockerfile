#FROM java:8-jre
#MAINTAINER Alexander Lukyanchikov <sqshq@sqshq.com>
#
#ADD ./target/config.jar /app/
#CMD ["java", "-Xmx200m", "-jar", "/app/config.jar"]
#
#HEALTHCHECK --interval=30s --timeout=30s CMD curl -f http://localhost:8888/actuator/health || exit 1
#
#EXPOSE 8888

#FROM eclipse-temurin:11-jre
#LABEL org.opencontainers.image.title="config-service" \
#      org.opencontainers.image.description="Piggy Metrics config service" \
#      org.opencontainers.image.authors="Ify Osakwe <ifeakachukwuosakwe@gmail.com>"
#RUN useradd -r -u 1001 spring && mkdir -p /app && chown -R spring:spring /app
#USER spring
#WORKDIR /app
#ARG JAR_FILE=target/config.jar
#COPY ${JAR_FILE} app.jar
## Container-friendly memory (lets JVM size itself by container limits)
#ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]
#EXPOSE 8888

# Optional healthcheck (uncomment the two lines below to install curl)
# USER root
# RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
# USER spring
# HEALTHCHECK --interval=30s --timeout=5s --start-period=20s \
#   CMD curl -fsS http://localhost:8888/actuator/health/readiness || exit 1

###############################################################################

FROM eclipse-temurin:21-jdk-jammy AS deps
WORKDIR /build
COPY --chmod=0755 mvnw mvnw
COPY .mvn/ .mvn/
RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 ./mvnw dependency:go-offline -DskipTests

FROM deps AS package
WORKDIR /build
COPY ./src src/
RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests && \
    mv target/$(./mvnw help:evaluate -Dexpression=project.artifactId -q -DforceStdout)-$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout).jar target/app.jar

FROM package AS extract
WORKDIR /build
RUN java -Djarmode=layertools -jar target/app.jar extract --destination target/extracted
#RUN java -Djarmode=tools -jar target/app.jar extract --destination target/extracted

FROM extract AS development
WORKDIR /build
COPY --from=extract /build/target/extracted/dependencies/. ./
COPY --from=extract /build/target/extracted/spring-boot-loader/. ./
COPY --from=extract /build/target/extracted/snapshot-dependencies/. ./
COPY --from=extract /build/target/extracted/application/. ./
ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000
CMD [ "java", "org.springframework.boot.loader.launch.JarLauncher" ]

FROM eclipse-temurin:21-jre-jammy AS final
ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser
USER appuser
COPY --from=extract build/target/extracted/dependencies/ ./
COPY --from=extract build/target/extracted/spring-boot-loader/ ./
COPY --from=extract build/target/extracted/snapshot-dependencies/ ./
COPY --from=extract build/target/extracted/application/ ./
EXPOSE 8888
ENTRYPOINT [ "java", "org.springframework.boot.loader.launch.JarLauncher" ]