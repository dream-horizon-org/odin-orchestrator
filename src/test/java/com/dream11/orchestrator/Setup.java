package com.dream11.orchestrator;

import com.dream11.orchestrator.inject.AppContext;
import com.redis.testcontainers.RedisContainer;
import java.net.InetAddress;
import java.net.URI;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

@Slf4j
public class Setup
    implements BeforeAllCallback, AfterAllCallback, ExtensionContext.Store.CloseableResource {
  static boolean started = false;
  LocalStackContainer localStackContainer;
  RedisContainer redisContainer;

  @Override
  public void beforeAll(ExtensionContext extensionContext) {
    if (!started) {
      this.configureLocalStack();
      this.configureRedis();
      started = true;
    }
  }

  private void configureLocalStack() {
    this.localStackContainer =
        new LocalStackContainer(DockerImageName.parse(Constants.LOCALSTACK_DOCKER_IMAGE))
            .withServices(LocalStackContainer.Service.SQS, LocalStackContainer.Service.S3);
    this.localStackContainer.start();
    String port = this.localStackContainer.getFirstMappedPort().toString();
    log.info("Started localstack container on port:{}", port);
    System.setProperty(Constants.AWS_REGION, this.localStackContainer.getRegion());
    System.setProperty(Constants.AWS_ACCESS_KEY_ID, this.localStackContainer.getAccessKey());
    System.setProperty(Constants.AWS_SECRET_ACCESS_KEY, this.localStackContainer.getSecretKey());

    this.configureSqs();
    this.configureS3();
  }

  private void configureS3() {
    log.info("Configuring S3 service");
    String endpoint =
        this.localStackContainer.getEndpointOverride(LocalStackContainer.Service.SQS).toString();
    String region = this.localStackContainer.getRegion();
    try (S3Client s3Client =
        S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.of(region))
            .build()) {

      CreateBucketResponse __ =
          s3Client.createBucket(
              CreateBucketRequest.builder().bucket(Constants.STATE_BUCKET_NAME).build());

      System.setProperty(Constants.DSL_STATE_S3_BUCKET, Constants.STATE_BUCKET_NAME);
      System.setProperty(Constants.DSL_STATE_S3_REGION, region);
      System.setProperty(
          Constants.DSL_STATE_S3_ENDPOINT,
          String.format(
              "http://%s:%d",
              this.getHostIpAddress(), this.localStackContainer.getFirstMappedPort()));
    }
  }

  private void configureRedis() {
    this.redisContainer =
        new RedisContainer(DockerImageName.parse(Constants.REDIS_DOCKER_IMAGE))
            .withExposedPorts(6379);
    this.redisContainer.start();
    String port = this.redisContainer.getFirstMappedPort().toString();
    log.info("Started redis container on port:{}", port);
    System.setProperty(Constants.DSL_LOCK_REDIS_HOST, this.getHostIpAddress());
    System.setProperty(Constants.DSL_LOCK_REDIS_PORT, port);
  }

  private void configureSqs() {
    log.info("Configuring SQS service");
    String endpoint =
        this.localStackContainer.getEndpointOverride(LocalStackContainer.Service.SQS).toString();
    String region = this.localStackContainer.getRegion();
    try (SqsClient sqsClient =
        SqsClient.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.of(region))
            .build()) {

      CreateQueueRequest createQueueRequest =
          CreateQueueRequest.builder()
              .queueName(Constants.SQS_REQUEST_QUEUE)
              .attributes(Map.of(QueueAttributeName.VISIBILITY_TIMEOUT, "5"))
              .build();
      CreateQueueResponse requestQueueResponse = sqsClient.createQueue(createQueueRequest);

      CreateQueueRequest createQueueRequestForResponseQ =
          CreateQueueRequest.builder().queueName(Constants.SQS_RESPONSE_QUEUE).build();
      CreateQueueResponse responseQueueResponse =
          sqsClient.createQueue(createQueueRequestForResponseQ);

      System.setProperty(Constants.SQS_REQUEST_QUEUE_ENDPOINT, endpoint);
      System.setProperty(Constants.SQS_REQUEST_QUEUE_REGION, region);
      System.setProperty(Constants.SQS_REQUEST_QUEUE_URL, requestQueueResponse.queueUrl());

      System.setProperty(Constants.SQS_RESPONSE_QUEUE_ENDPOINT, endpoint);
      System.setProperty(Constants.SQS_RESPONSE_QUEUE_REGION, region);
      System.setProperty(Constants.SQS_RESPONSE_QUEUE_URL, responseQueueResponse.queueUrl());
    }
  }

  @Override
  public void close() {
    this.localStackContainer.stop();
    this.redisContainer.stop();
  }

  @Override
  public void afterAll(ExtensionContext extensionContext) {
    AppContext.reset();
  }

  @SneakyThrows
  private String getHostIpAddress() {
    return InetAddress.getLocalHost().getHostAddress();
  }
}
