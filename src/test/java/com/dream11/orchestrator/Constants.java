package com.dream11.orchestrator;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
  public final String LOCALSTACK_DOCKER_IMAGE = "localstack/localstack:1.3.0";
  public final String REDIS_DOCKER_IMAGE = "redis:8.2.1";
  public final String SQS_REQUEST_QUEUE = "request_queue";
  public final String SQS_RESPONSE_QUEUE = "response_queue";
  public final String SQS_REQUEST_QUEUE_ENDPOINT = "queue.request.endpoint";

  public final String SQS_RESPONSE_QUEUE_ENDPOINT = "queue.response.endpoint";
  public final String SQS_REQUEST_QUEUE_REGION = "queue.request.region";

  public final String SQS_RESPONSE_QUEUE_REGION = "queue.response.region";
  public final String SQS_REQUEST_QUEUE_URL = "queue.request.queueUrl";

  public final String SQS_RESPONSE_QUEUE_URL = "queue.response.queueUrl";

  public final String DSL_LOCK_REDIS_HOST = "dsl.lock.config.host";
  public final String DSL_LOCK_REDIS_PORT = "dsl.lock.config.port";
  public final String DSL_STATE_S3_BUCKET = "dsl.state.config.bucket";
  public final String DSL_STATE_S3_ENDPOINT = "dsl.state.config.endpoint";
  public final String DSL_STATE_S3_REGION = "dsl.state.config.region";
  public final String STATE_BUCKET_NAME = "odin-state";
  public static final String AWS_REGION = "aws.region";
  public static final String AWS_ACCESS_KEY_ID = "aws.accessKeyId";
  public static final String AWS_SECRET_ACCESS_KEY = "aws.secretAccessKey";
}
