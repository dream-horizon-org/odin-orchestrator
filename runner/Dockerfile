FROM groovy:4.0.6-jdk17
USER root

# Install helm
RUN curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
RUN chmod 700 ./get_helm.sh
RUN bash get_helm.sh

RUN wget https://releases.hashicorp.com/packer/1.11.1/packer_1.11.1_linux_amd64.zip
RUN unzip packer_1.11.1_linux_amd64.zip -d /usr/local/bin/
RUN packer plugins install github.com/hashicorp/docker 1.0.11
RUN packer plugins install github.com/hashicorp/amazon 1.3.2

# Install yq
RUN wget https://github.com/mikefarah/yq/releases/download/v4.34.2/yq_linux_amd64 -O /usr/bin/yq
RUN chmod +x /usr/bin/yq

# Install jq
RUN apt-get update -y
RUN apt-get install jq -y

ADD entrypoint.sh /opt/entrypoint.sh
WORKDIR /opt
ENTRYPOINT ["/opt/entrypoint.sh"]
