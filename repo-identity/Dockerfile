FROM dangch-framework AS build

WORKDIR /workspace

COPY repo-identity/pom.xml repo-identity/pom.xml
COPY repo-identity/src repo-identity/src
RUN mvn -f repo-identity/pom.xml -DskipTests package org.springframework.boot:spring-boot-maven-plugin:3.3.4:repackage \
    && JAR="$(ls repo-identity/target/*.jar | grep -vE '(-plain|-original)\\.jar$' | head -n1)" \
    && cp "$JAR" /workspace/app.jar

FROM eclipse-temurin:21-jre

WORKDIR /workspace
COPY --from=build /workspace/app.jar /workspace/app.jar

EXPOSE 58081
ENTRYPOINT ["java","-jar","/workspace/app.jar"]
