#FROM java:8-jre
#MAINTAINER Alexander Lukyanchikov <sqshq@sqshq.com>
#
#ADD ./target/config.jar /app/
#CMD ["java", "-Xmx200m", "-jar", "/app/config.jar"]
#
#HEALTHCHECK --interval=30s --timeout=30s CMD curl -f http://localhost:8888/actuator/health || exit 1
#
#EXPOSE 8888

FROM eclipse-temurin:11-jre

LABEL org.opencontainers.image.title="config-service" \
      org.opencontainers.image.description="Piggy Metrics config service" \
      org.opencontainers.image.authors="Ify Osakwe <ifeakachukwuosakwe@gmail.com>"

RUN useradd -r -u 1001 spring && mkdir -p /app && chown -R spring:spring /app
USER spring

WORKDIR /app
ARG JAR_FILE=target/config.jar
COPY ${JAR_FILE} app.jar

# Container-friendly memory (lets JVM size itself by container limits)
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]

EXPOSE 8888

# Optional healthcheck (uncomment the two lines below to install curl)
# USER root
# RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
# USER spring
# HEALTHCHECK --interval=30s --timeout=5s --start-period=20s \
#   CMD curl -fsS http://localhost:8888/actuator/health/readiness || exit 1