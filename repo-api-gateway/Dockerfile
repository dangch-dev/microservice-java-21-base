FROM dangch-framework AS build

WORKDIR /workspace

COPY repo-api-gateway/pom.xml repo-api-gateway/pom.xml
COPY repo-api-gateway/src repo-api-gateway/src
RUN mvn -f repo-api-gateway/pom.xml -DskipTests package org.springframework.boot:spring-boot-maven-plugin:3.3.4:repackage \
    && JAR="$(ls repo-api-gateway/target/*.jar | grep -vE '(-plain|-original)\\.jar$' | head -n1)" \
    && cp "$JAR" /workspace/app.jar

FROM eclipse-temurin:21-jre

WORKDIR /workspace
COPY --from=build /workspace/app.jar /workspace/app.jar

EXPOSE 58080
ENTRYPOINT ["java","-jar","/workspace/app.jar"]
