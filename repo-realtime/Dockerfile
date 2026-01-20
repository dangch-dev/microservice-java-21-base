FROM dangch-framework AS build

WORKDIR /workspace

COPY repo-realtime/pom.xml repo-realtime/pom.xml
COPY repo-realtime/src repo-realtime/src
RUN mvn -f repo-realtime/pom.xml -DskipTests package org.springframework.boot:spring-boot-maven-plugin:3.3.4:repackage \
    && JAR="$(ls repo-realtime/target/*.jar | grep -vE '(-plain|-original)\\.jar$' | head -n1)" \
    && cp "$JAR" /workspace/app.jar

FROM eclipse-temurin:21-jre

WORKDIR /workspace
COPY --from=build /workspace/app.jar /workspace/app.jar

EXPOSE 58084
ENTRYPOINT ["java","-jar","/workspace/app.jar"]
