package io.github.santannaf.kafka.integration;

import io.github.santannaf.kafka.configuration.components.KafkaComponentsFactory;
import io.github.santannaf.kafka.configuration.properties.KafkaArchCommonProperties;
import io.github.santannaf.kafka.configuration.properties.KafkaArchConsumerProperties;
import io.github.santannaf.kafka.configuration.properties.KafkaArchProducerProperties;
import io.github.santannaf.kafka.configuration.properties.TunedKafkaProperties;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class KafkaSslIntegrationTest {

  private static final String SSL_PASSWORD = "changeit";
  private static final Network network = Network.newNetwork();

  @SuppressWarnings("resource")
  static final GenericContainer<?> kafka = new GenericContainer<>("confluentinc/cp-kafka:7.4.0")
    .withNetwork(network)
    .withNetworkAliases("kafka")
    .withExposedPorts(9093)
    .withCopyFileToContainer(
      MountableFile.forClasspathResource("ssl/kafka.keystore.p12"),
      "/etc/kafka/secrets/kafka.keystore.p12")
    .withCopyFileToContainer(
      MountableFile.forClasspathResource("ssl/kafka.truststore.p12"),
      "/etc/kafka/secrets/kafka.truststore.p12")
    .withEnv("KAFKA_NODE_ID", "1")
    .withEnv("KAFKA_PROCESS_ROLES", "broker,controller")
    .withEnv("KAFKA_CONTROLLER_QUORUM_VOTERS", "1@kafka:29093")
    .withEnv("KAFKA_LISTENERS", "BROKER://0.0.0.0:9092,SSL://0.0.0.0:9093,CONTROLLER://0.0.0.0:29093")
    .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "BROKER:PLAINTEXT,SSL:SSL,CONTROLLER:PLAINTEXT")
    .withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER")
    .withEnv("KAFKA_CONTROLLER_LISTENER_NAMES", "CONTROLLER")
    .withEnv("KAFKA_SSL_KEYSTORE_FILENAME", "kafka.keystore.p12")
    .withEnv("KAFKA_SSL_KEYSTORE_CREDENTIALS", "ssl_credentials")
    .withEnv("KAFKA_SSL_KEY_CREDENTIALS", "ssl_credentials")
    .withEnv("KAFKA_SSL_KEYSTORE_TYPE", "PKCS12")
    .withEnv("KAFKA_SSL_TRUSTSTORE_FILENAME", "kafka.truststore.p12")
    .withEnv("KAFKA_SSL_TRUSTSTORE_CREDENTIALS", "ssl_credentials")
    .withEnv("KAFKA_SSL_TRUSTSTORE_TYPE", "PKCS12")
    .withEnv("KAFKA_SSL_CLIENT_AUTH", "none")
    .withEnv("KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM", "")
    .withCopyFileToContainer(
      MountableFile.forClasspathResource("ssl/ssl_credentials"),
      "/etc/kafka/secrets/ssl_credentials")
    .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
    .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
    .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
    .withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0")
    .withEnv("CLUSTER_ID", "MkU3OEVBNTcwNTJENDM2Qk")
    .withCommand("sh", "-c",
      "while [ ! -f /tmp/kafka_listeners ]; do sleep 0.1; done && " +
        "export KAFKA_ADVERTISED_LISTENERS=$(cat /tmp/kafka_listeners) && " +
        "/etc/confluent/docker/run")
    .waitingFor(new AbstractWaitStrategy() {
      @Override
      protected void waitUntilReady() {
        // No-op: Kafka startup is deferred until we write the signal file.
        // Readiness is checked explicitly in setUp() via waitForKafkaReady().
      }
    });

  private static String sslBootstrapServers;
  private static String truststorePath;
  private static String keystorePath;

  @BeforeAll
  static void setUp() throws Exception {
    kafka.start();

    var host = kafka.getHost();
    var port = kafka.getMappedPort(9093);
    sslBootstrapServers = host + ":" + port;

    // Signal Kafka to start with the correct advertised listeners
    kafka.execInContainer("sh", "-c",
      "echo 'BROKER://kafka:9092,SSL://" + host + ":" + port + "' > /tmp/kafka_listeners");

    truststorePath = extractTestCertToTempFile("ssl/kafka.truststore.p12", "truststore");
    keystorePath = extractTestCertToTempFile("ssl/kafka.keystore.p12", "keystore");

    // Wait for Kafka to be ready
    waitForKafkaReady();
  }

  @AfterAll
  static void tearDown() {
    kafka.stop();
    network.close();
  }

  @Test
  void shouldProduceAndConsumeViaSsl() throws Exception {
    var topicName = "test-ssl-" + UUID.randomUUID();
    var messageKey = "key-ssl";
    var messageValue = "Hello Kafka over SSL!";

    createTopic(topicName);

    var producerProps = new HashMap<String, Object>();
    producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, sslBootstrapServers);
    producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    applySslConfig(producerProps);

    try (var producer = new KafkaProducer<String, String>(producerProps)) {
      producer.send(new ProducerRecord<>(topicName, messageKey, messageValue)).get(10, TimeUnit.SECONDS);
    }

    var consumerProps = new HashMap<String, Object>();
    consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, sslBootstrapServers);
    consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-ssl-consumer-" + UUID.randomUUID());
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    applySslConfig(consumerProps);

    try (var consumer = new KafkaConsumer<String, String>(consumerProps)) {
      consumer.subscribe(Collections.singletonList(topicName));
      var records = pollUntilRecords(consumer, 30_000);

      assertThat(records.count()).isEqualTo(1);
      var record = records.iterator().next();
      assertThat(record.key()).isEqualTo(messageKey);
      assertThat(record.value()).isEqualTo(messageValue);
    }
  }

  @Test
  void shouldConnectViaSsl_usingKafkaComponentsFactory() {
    var common = new KafkaArchCommonProperties(
      sslBootstrapServers, null, "dev", "mock://localhost:8081",
      true, null, truststorePath, SSL_PASSWORD, keystorePath, SSL_PASSWORD,
      50, 2000, 2, false, null, null
    );
    var producer = new KafkaArchProducerProperties(
      "all", 3, 16384, 5, false, "none", null, null, false
    );
    var consumer = new KafkaArchConsumerProperties(
      "ssl-test-group", "manual", "earliest", false, 100, 300000,
      1, 500, 20000, 3000, 30000, false, false, false, 3, 10000, false
    );
    var properties = new TunedKafkaProperties(common, producer, consumer);
    var factory = new KafkaComponentsFactory(properties);

    var producerFactory = factory.producerFactory();
    var admin = factory.kafkaAdmin();
    var template = factory.kafkaTemplate(producerFactory, admin);
    var consumerFactory = factory.kafkaConsumerFactory();

    assertThat(producerFactory).isNotNull();
    assertThat(admin).isNotNull();
    assertThat(template).isNotNull();
    assertThat(consumerFactory).isNotNull();
  }

  @Test
  void shouldConnectViaSsl_usingCertificateResolver() {
    var adminProps = Map.<String, Object>of(
      AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, sslBootstrapServers,
      "security.protocol", "SSL",
      SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststorePath,
      SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, SSL_PASSWORD,
      SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystorePath,
      SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, SSL_PASSWORD,
      SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12",
      SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PKCS12",
      SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, ""
    );

    try (var adminClient = AdminClient.create(adminProps)) {
      var nodes = adminClient.describeCluster().nodes();
      assertThat(nodes).isNotNull();
    }
  }

  private static void applySslConfig(Map<String, Object> props) {
    props.put("security.protocol", "SSL");
    props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststorePath);
    props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, SSL_PASSWORD);
    props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystorePath);
    props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, SSL_PASSWORD);
    props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
    props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PKCS12");
    props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
  }

  private static String extractTestCertToTempFile(String classpathResource, String prefix) {
    try (var input = Thread.currentThread().getContextClassLoader()
      .getResourceAsStream(classpathResource)) {
      if (input == null) throw new IllegalStateException("Resource not found: " + classpathResource);
      var tempFile = Files.createTempFile(prefix + "-test-", ".p12");
      try (OutputStream out = Files.newOutputStream(tempFile)) {
        out.write(input.readAllBytes());
      }
      tempFile.toFile().deleteOnExit();
      return tempFile.toAbsolutePath().toString();
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to extract test cert", e);
    }
  }

  private static <K, V> ConsumerRecords<K, V> pollUntilRecords(KafkaConsumer<K, V> consumer, long timeoutMs) {
    ConsumerRecords<K, V> records = ConsumerRecords.empty();
    var deadline = System.currentTimeMillis() + timeoutMs;
    while (records.isEmpty() && System.currentTimeMillis() < deadline) {
      records = consumer.poll(Duration.ofMillis(500));
    }
    return records;
  }

  private void createTopic(String topicName) throws ExecutionException, InterruptedException, TimeoutException {
    var props = new HashMap<String, Object>();
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, sslBootstrapServers);
    applySslConfig(props);

    try (var adminClient = AdminClient.create(props)) {
      adminClient.createTopics(Collections.singletonList(new NewTopic(topicName, 1, (short) 1)))
        .all().get(10, TimeUnit.SECONDS);
    }
  }

  private static void waitForKafkaReady() throws Exception {
    var props = new HashMap<String, Object>();
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, sslBootstrapServers);
    props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
    applySslConfig(props);

    var deadline = System.currentTimeMillis() + 60_000;
    while (System.currentTimeMillis() < deadline) {
      try (var admin = AdminClient.create(props)) {
        admin.describeCluster().nodes().get(5, TimeUnit.SECONDS);
        return;
      }
      catch (Exception e) {
        Thread.sleep(1000);
      }
    }
    throw new RuntimeException("Kafka did not become ready within 60 seconds. Logs:\n" + kafka.getLogs());
  }
}
