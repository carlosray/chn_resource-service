# Build stage
#
FROM maven:3.6.0-jdk-11-slim AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean install

#
# Package stage
#
FROM openjdk:11-jre-slim
COPY --from=build /home/app/target/resource-service-1.0.jar /usr/local/lib/resource-service-1.0.jar
ENTRYPOINT ["java","-jar","/usr/local/lib/config-server-1.0.jar"]