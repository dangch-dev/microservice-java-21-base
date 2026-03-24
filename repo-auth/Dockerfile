FROM dangch-framework AS build

WORKDIR /workspace

COPY repo-auth/pom.xml repo-auth/pom.xml
COPY repo-auth/src repo-auth/src
RUN mvn -f repo-auth/pom.xml -DskipTests package org.springframework.boot:spring-boot-maven-plugin:3.3.4:repackage \
    && JAR="$(ls repo-auth/target/*.jar | grep -vE '(-plain|-original)\\.jar$' | head -n1)" \
    && cp "$JAR" /workspace/app.jar

FROM eclipse-temurin:21-jre

WORKDIR /workspace
COPY --from=build /workspace/app.jar /workspace/app.jar

EXPOSE 58082
ENTRYPOINT ["java","-jar","/workspace/app.jar"]
