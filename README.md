# Odin Orchestrator

## Overview

The **Odin Orchestrator** is a lightweight, stateless orchestration service designed to manage the lifecycle of services and components in an Odin environment. It listens for messages from a queue, executes requested operations, and publishes real-time progress and final status updates back to the response queue.

---

## Core Responsibilities

1. **Environment Creation/Deletion**
    - Initializes a new Odin environment.
    - If Kubernetes (K8S) is configured, it automatically creates a namespace named after the environment.

2. **Service and Component Operations**
    - Handles **deploy**, **operate**, and **undeploy** actions on requested services and their components.

---

## Execution Flow

1. **Message Processing**
    - The orchestrator receives a message from the queue describing the desired operation.
    - Based on the message, it creates a detailed execution plan.

2. **Runner Pods**
    - The orchestrator provisions temporary Kubernetes job pods known as **runners**.
    - Each runner is responsible for executing **one command** (e.g., deploy, operate, or undeploy) for a **single component**.

3. **Execution Plan (DAG)**
    - When a multi-component service is deployed or operated on, the orchestrator constructs a **Directed Acyclic Graph (DAG)**.
    - The DAG defines the execution order of service components, ensuring dependencies are respected.

4. **Progress Tracking**
    - The orchestrator monitors every runner pod it creates.
    - It continuously sends **status updates** (e.g., pending, in-progress, success, failure) to the response queue.

5. **Completion and Cleanup**
    - Once all runner pods finish (successfully or not), the orchestrator compiles a **final response message** summarizing results.
    - It then terminates, maintaining no state between executions.

---

## Architectural Highlights

- **Stateless Design:**
  The orchestrator maintains no persistent state and operates purely within the context of a single message execution cycle.

- **Kubernetes Native:**
  Runner pods are ephemeral Kubernetes jobs, ensuring scalability and fault isolation.

- **Message-Driven:**
  Operates solely based on messages received from the queue, making it easy to integrate into event-driven architectures.

- **Extensible Execution Model:**
  Each runner pod executes a single, well-defined unit of work, allowing modular scaling and parallel execution.



## Configuration
The orchestrator reads defaults from `src/main/resources/application-default.conf` and allows overrides through `src/main/resources/application.conf` which is, in turn, overridden by environment variables. Below is a consolidated list of configuration keys and defaults.

**Queue (Requests/Responses)**

| Key | Description | Default |
| --- | --- | --- |
| ODIN_QUEUE_REQUEST_ENDPOINT | Request queue endpoint override | - |
| ODIN_QUEUE_REQUEST_URL | Request queue URL | - |
| ODIN_QUEUE_REQUEST_PROVIDER | Request queue provider | sqs |
| ODIN_QUEUE_REQUEST_REGION | Request queue region | us-east-1 |
| ODIN_QUEUE_RESPONSE_ENDPOINT | Response queue endpoint override | - |
| ODIN_QUEUE_RESPONSE_URL | Response queue URL | - |
| ODIN_QUEUE_RESPONSE_PROVIDER | Response queue provider | sqs |
| ODIN_QUEUE_RESPONSE_REGION | Response queue region | us-east-1 |

**Component Registry**

| Key | Description | Default |
| --- | --- | --- |
| ODIN_COMPONENT_REGISTRY_URL | Registry base URL | https://dream-horizon-org.github.io/odin-components/ |
| ODIN_COMPONENT_REGISTRY_USERNAME | Basic auth username | - |
| ODIN_COMPONENT_REGISTRY_PASSWORD | Basic auth password | - |

**DSL (Component Interface + State/Lock)**

| Key | Description | Default |
| --- | --- | --- |
| ODIN_DSL_URL | DSL artifacts base URL | https://github.com/dream-horizon-org/odin-component-interface/releases/download |
| ODIN_DSL_USERNAME | DSL auth username | - |
| ODIN_DSL_PASSWORD | DSL auth password | - |
| ODIN_DSL_STATE_S3_BUCKET | S3 bucket for state persistence | - |
| ODIN_DSL_STATE_S3_ENDPOINT | S3 endpoint (if non-AWS/minio) | - |
| ODIN_DSL_STATE_S3_REGION | S3 region | us-east-1 |
| ODIN_DSL_STATE_S3_FORCE_PATH_STYLE | S3 path-style access | false |
| ODIN_DSL_LOCK_REDIS_HOST | Redis host for lock provider | - |

**Discovery**

| Key | Description | Default |
| --- | --- | --- |
| ODIN_DISCOVERY_URL | Discovery service base URL | - |

**Runner (Kubernetes Job)**

| Key | Description | Default             |
| --- | --- |---------------------|
| ODIN_RUNNER_IMAGE | Runner image | odinhq/runner:0.0.2 |
| ODIN_RUNNER_RESOURCES_REQUESTS_CPU | Runner CPU request | 100m                |
| ODIN_RUNNER_RESOURCES_REQUESTS_MEMORY | Runner memory request | 100Mi               |
| ODIN_RUNNER_RESOURCES_REQUESTS_STORAGE | Runner ephemeral-storage request | 1Gi                 |
| ODIN_RUNNER_RESOURCES_LIMITS_CPU | Runner CPU limit | -                   |
| ODIN_RUNNER_RESOURCES_LIMITS_MEMORY | Runner memory limit | -                   |
| ODIN_RUNNER_RESOURCES_LIMITS_STORAGE | Runner ephemeral-storage limit | -                   |
| ODIN_RUNNER_DIND_ENABLED | Enable DinD sidecar | true                |
| ODIN_RUNNER_DIND_IMAGE | DinD image | docker:dind         |
| ODIN_RUNNER_DIND_RESOURCES_REQUESTS_CPU | DinD CPU request | 100m                |
| ODIN_RUNNER_DIND_RESOURCES_REQUESTS_MEMORY | DinD memory request | 100Mi               |
| ODIN_RUNNER_DIND_RESOURCES_REQUESTS_STORAGE | DinD ephemeral-storage request | 1Gi                 |
| ODIN_RUNNER_DIND_RESOURCES_LIMITS_CPU | DinD CPU limit | -                   |
| ODIN_RUNNER_DIND_RESOURCES_LIMITS_MEMORY | DinD memory limit | -                   |
| ODIN_RUNNER_DIND_RESOURCES_LIMITS_STORAGE | DinD ephemeral-storage limit | -                   |
| ODIN_RUNNER_DOCKER_SECRETS | Docker registry secrets for runner | []                  |
| ODIN_RUNNER_HOST_VOLUME_MOUNTS | HostPath volume mounts for runner | []                  |
