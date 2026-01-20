FROM dangch-framework AS build

WORKDIR /workspace

COPY repo-notification/pom.xml repo-notification/pom.xml
COPY repo-notification/src repo-notification/src
RUN mvn -f repo-notification/pom.xml -DskipTests package org.springframework.boot:spring-boot-maven-plugin:3.3.4:repackage \
    && JAR="$(ls repo-notification/target/*.jar | grep -vE '(-plain|-original)\\.jar$' | head -n1)" \
    && cp "$JAR" /workspace/app.jar

FROM eclipse-temurin:21-jre

WORKDIR /workspace
COPY --from=build /workspace/app.jar /workspace/app.jar

EXPOSE 58083
ENTRYPOINT ["java","-jar","/workspace/app.jar"]
