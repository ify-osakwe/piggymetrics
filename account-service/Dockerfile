#FROM java:8-jre
#MAINTAINER Alexander Lukyanchikov <sqshq@sqshq.com>
#
#ADD ./target/account-service.jar /app/
#CMD ["java", "-Xmx200m", "-jar", "/app/account-service.jar"]
#
#EXPOSE 6000

FROM eclipse-temurin:11-jre
LABEL maintainer="Ify Osakwe <ifeakachukwuosakwe@gmail.com>"

ARG JAR_FILE=target/account-service.jar
COPY ${JAR_FILE} /app/app.jar

ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]
EXPOSE 7000