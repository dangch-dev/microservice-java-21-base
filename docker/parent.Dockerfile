FROM maven:3.9.9-eclipse-temurin-21

WORKDIR /workspace

COPY repo-common-parent/pom.xml repo-common-parent/pom.xml
RUN mvn -f repo-common-parent/pom.xml -DskipTests install
