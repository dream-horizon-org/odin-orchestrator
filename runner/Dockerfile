FROM groovy:4.0.27-jdk21-alpine
USER root
ARG TARGETARCH

RUN apk add bash curl jq yq docker-cli helm kubectl

# Install packer
ARG PACKER_VERSION=1.14.2
RUN wget https://releases.hashicorp.com/packer/${PACKER_VERSION}/packer_${PACKER_VERSION}_linux_${TARGETARCH}.zip
RUN unzip packer_${PACKER_VERSION}_linux_${TARGETARCH}.zip -d /usr/local/bin/
RUN packer plugins install github.com/hashicorp/docker 1.1.2
RUN packer plugins install github.com/hashicorp/amazon 1.5.0

ADD entrypoint.sh /opt/entrypoint.sh
WORKDIR /opt
ENTRYPOINT ["/opt/entrypoint.sh"]
