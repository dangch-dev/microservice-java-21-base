FROM dangch-framework AS build

WORKDIR /workspace

COPY repo-eureka/pom.xml repo-eureka/pom.xml
COPY repo-eureka/src repo-eureka/src
RUN mvn -f repo-eureka/pom.xml -DskipTests package org.springframework.boot:spring-boot-maven-plugin:3.3.4:repackage \
    && JAR="$(ls repo-eureka/target/*.jar | grep -vE '(-plain|-original)\\.jar$' | head -n1)" \
    && cp "$JAR" /workspace/app.jar

FROM eclipse-temurin:21-jre

WORKDIR /workspace
COPY --from=build /workspace/app.jar /workspace/app.jar

EXPOSE 58761
ENTRYPOINT ["java","-jar","/workspace/app.jar"]
