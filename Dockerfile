FROM maven:3.9.11-eclipse-temurin-17 AS executor

# Packaging
FROM executor AS builder
WORKDIR /app
COPY pom.xml /app/
RUN mvn --no-transfer-progress dependency:resolve dependency:resolve-plugins
COPY . /app/
RUN mvn --no-transfer-progress package -DskipTests

# Final docker image
FROM eclipse-temurin:17.0.15_6-jre-ubi9-minimal
# Install helm
ENV HELM_VERSION=3.8.0
RUN curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
RUN chmod 700 ./get_helm.sh
RUN bash get_helm.sh

COPY --from=builder /app/target/odin-orchestrator /opt/odin-orchestrator/
COPY entrypoint.sh /opt/odin-orchestrator/entrypoint.sh
WORKDIR /opt/odin-orchestrator
ENTRYPOINT ["./entrypoint.sh"]
