FROM dangch-framework AS build

WORKDIR /workspace

COPY repo-storage/pom.xml repo-storage/pom.xml
COPY repo-storage/src repo-storage/src
RUN mvn -f repo-storage/pom.xml -DskipTests package org.springframework.boot:spring-boot-maven-plugin:3.3.4:repackage \
    && JAR="$(ls repo-storage/target/*.jar | grep -vE '(-plain|-original)\\.jar$' | head -n1)" \
    && cp "$JAR" /workspace/app.jar

FROM eclipse-temurin:21-jre

WORKDIR /workspace
RUN apt-get update \
    && apt-get install -y ffmpeg \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/app.jar /workspace/app.jar

EXPOSE 8086
ENTRYPOINT ["java","-jar","/workspace/app.jar"]
