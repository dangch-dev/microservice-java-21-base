FROM dangch-framework AS build

WORKDIR /workspace

COPY repo-assessment/pom.xml repo-assessment/pom.xml
COPY repo-assessment/src repo-assessment/src
RUN mvn -f repo-assessment/pom.xml -DskipTests package org.springframework.boot:spring-boot-maven-plugin:3.3.4:repackage \
    && JAR="$(ls repo-assessment/target/*.jar | grep -vE '(-plain|-original)\\.jar$' | head -n1)" \
    && cp "$JAR" /workspace/app.jar

FROM eclipse-temurin:21-jre

WORKDIR /workspace
COPY --from=build /workspace/app.jar /workspace/app.jar

EXPOSE 58087
ENTRYPOINT ["java","-jar","/workspace/app.jar"]
