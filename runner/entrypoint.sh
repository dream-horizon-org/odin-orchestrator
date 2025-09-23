#!/usr/bin/env bash
set -eo pipefail

APP_DIR=/opt
DSL_JAR_PATH="${APP_DIR}/odin-component-interface.jar"
mkdir -p ${APP_DIR}

cleanup() {
	exit_code=$?
	if [[ "${ODIN_RUNNER_DIND_ENABLED}" == "true" ]]; then
		while ! pgrep -x dockerd; do
			sleep 1
		done
		pkill -SIGTERM -x dockerd
	fi
	exit ${exit_code}
}

trap 'cleanup' EXIT
trap 'echo Exiting gracefully, sending SIGTERM to components; kill 0; wait; exit 1' SIGTERM SIGINT

curl_with_retry() {
	RETRIES=${RETRIES:-5}
	DELAY=${DELAY:-10}
	STATUS=0
	while [[ ${STATUS} != 200 && ${RETRIES} -gt 0 ]]; do
		creds=$1
		url=$2
		output_path=$3
		STATUS=$(curl -sS --location -u "${creds}" -X GET "${url}" --output "${output_path}" --write-out "%{http_code}\\n")
		if [[ ! ${STATUS} == 200 ]]; then
			response=$(cat "${output_path}")
			echo -e "ERROR while downloading from ${url}. STATUS: ${STATUS}\nERROR: ${response}"
			((RETRIES--))
			if [[ ${RETRIES} == 0 ]]; then
				exit 1
			fi
			sleep "${DELAY}"
		fi
	done
}

download_component() {
	curl_with_retry "${ODIN_COMPONENT_REGISTRY_USERNAME}:${ODIN_COMPONENT_REGISTRY_PASSWORD}" "${ODIN_COMPONENT_REGISTRY_URL}/index.yaml" index.yaml
	COMPONENT_DOWNLOAD_URL=$(yq '.components | .[env(ODIN_COMPONENT_TYPE)][] | select(.version == env(ODIN_COMPONENT_VERSION)) | .downloadUrl // error("Component \""+env(ODIN_COMPONENT_TYPE)+"\" with version \""+env(ODIN_COMPONENT_VERSION)+"\" not found.")' index.yaml)
	curl_with_retry "${ODIN_COMPONENT_REGISTRY_USERNAME}:${ODIN_COMPONENT_REGISTRY_PASSWORD}" "${COMPONENT_DOWNLOAD_URL}" "${ODIN_COMPONENT_TYPE}.tar.gz"
	tar -xzf "${ODIN_COMPONENT_TYPE}.tar.gz" -C ${APP_DIR}
}

download_dsl() {
	DSL_VERSION=$(cat ${APP_DIR}/*/component.groovy | grep dslVersion | cut -d'"' -f2)
	curl_with_retry "${ODIN_DSL_USERNAME}":"${ODIN_DSL_PASSWORD}" "${ODIN_DSL_URL}/${DSL_VERSION}/odin-component-interface.jar" "${DSL_JAR_PATH}"
}

download_component
download_dsl

# Set kubeconfig
if [[ -n "${ODIN_BASE64_ENCODED_KUBECONFIG}" ]]; then
	export KUBECONFIG=~/.kube/config
	mkdir -p "$(dirname ${KUBECONFIG})"
	echo "${ODIN_BASE64_ENCODED_KUBECONFIG}" | base64 -d >${KUBECONFIG}
fi

# Execute
cd "${APP_DIR}/${ODIN_COMPONENT_TYPE}"

if [[ "${WAIT}" = "infinite" ]]; then
	tail -f /dev/null
else
	groovy -cp "${DSL_JAR_PATH}" component.groovy &
	wait $!
fi
