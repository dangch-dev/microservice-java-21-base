FROM dangch-parent

WORKDIR /workspace

COPY repo-common-framework/pom.xml repo-common-framework/pom.xml
COPY repo-common-framework/src repo-common-framework/src
RUN mvn -f repo-common-framework/pom.xml -DskipTests install
