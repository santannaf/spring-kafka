package io.github.santannaf.kafka.integration;

import io.github.santannaf.kafka.configuration.components.KafkaComponentsFactory;
import io.github.santannaf.kafka.configuration.properties.KafkaArchCommonProperties;
import io.github.santannaf.kafka.configuration.properties.KafkaArchConsumerProperties;
import io.github.santannaf.kafka.configuration.properties.KafkaArchProducerProperties;
import io.github.santannaf.kafka.configuration.properties.TunedKafkaProperties;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@Tag("integration")
class KafkaIntegrationTest {

  @Container
  static final ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.8.0");

  private static KafkaComponentsFactory factory;
  private static TunedKafkaProperties properties;

  @BeforeAll
  static void setUp() {
    var common = new KafkaArchCommonProperties(
      kafka.getBootstrapServers(), null, "dev", "mock://localhost:8081",
      false, null, null, null, null, null,
      50, 2000, 2, false, null, null
    );
    var producer = new KafkaArchProducerProperties(
      "all", 3, 16384, 5, false, "none", null, null, false
    );
    var consumer = new KafkaArchConsumerProperties(
      "integration-test-group", "manual", "earliest", false, 100, 300000,
      1, 500, 20000, 3000, 30000, false, false, false, 3, 10000, false
    );
    properties = new TunedKafkaProperties(common, producer, consumer);
    factory = new KafkaComponentsFactory(properties);
  }

  @Nested
  class KafkaAdminIntegration {

    @Test
    void shouldConnectToKafkaCluster() {
      var admin = factory.kafkaAdmin();
      assertThat(admin).isNotNull();
      assertThat(admin.getConfigurationProperties())
        .containsEntry(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
    }

    @Test
    void shouldCreateTopicViaKafkaAdmin() throws ExecutionException, InterruptedException, TimeoutException {
      var admin = factory.kafkaAdmin();
      admin.setAutoCreate(false);

      var topicName = "test-admin-topic-" + UUID.randomUUID();
      var newTopic = new NewTopic(topicName, 1, (short) 1);
      admin.createOrModifyTopics(newTopic);

      var adminClient = org.apache.kafka.clients.admin.AdminClient.create(
        Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers())
      );
      var topics = adminClient.listTopics().names().get(10, TimeUnit.SECONDS);
      assertThat(topics).contains(topicName);
      adminClient.close();
    }
  }

  @Nested
  class ProduceAndConsumeTextIntegration {

    @Test
    void shouldProduceAndConsumeTextMessage() throws Exception {
      var topicName = "test-text-" + UUID.randomUUID();
      var messageKey = "key-text";
      var messageValue = "Hello Kafka - plain text message!";

      createTopic(topicName);

      var producerProps = Map.<String, Object>of(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
      );
      try (var producer = new KafkaProducer<String, String>(producerProps)) {
        producer.send(new ProducerRecord<>(topicName, messageKey, messageValue)).get(10, TimeUnit.SECONDS);
      }

      var consumerProps = Map.<String, Object>of(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
        ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID(),
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
      );
      try (var consumer = new KafkaConsumer<String, String>(consumerProps)) {
        consumer.subscribe(Collections.singletonList(topicName));

        var records = pollUntilRecords(consumer, 30_000);

        assertThat(records.count()).isEqualTo(1);
        var record = records.iterator().next();
        assertThat(record.key()).isEqualTo(messageKey);
        assertThat(record.value()).isEqualTo(messageValue);
      }
    }
  }

  @Nested
  class ProduceAndConsumeJsonIntegration {

    @Test
    void shouldProduceAndConsumeJsonMessage() throws Exception {
      var topicName = "test-json-" + UUID.randomUUID();
      var messageKey = "key-json";
      var messageValue = Map.of(
        "name", "Kafka Integration",
        "version", 1,
        "active", true
      );

      createTopic(topicName);

      var producerProps = Map.<String, Object>of(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
      );
      try (var producer = new KafkaProducer<String, Object>(producerProps)) {
        producer.send(new ProducerRecord<>(topicName, messageKey, messageValue)).get(10, TimeUnit.SECONDS);
      }

      var consumerProps = new HashMap<String, Object>();
      consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
      consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID());
      consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
      consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
      consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
      consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
      consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Map.class.getName());

      try (var consumer = new KafkaConsumer<String, Map<String, Object>>(consumerProps)) {
        consumer.subscribe(Collections.singletonList(topicName));

        var records = pollUntilRecords(consumer, 30_000);

        assertThat(records.count()).isEqualTo(1);
        var record = records.iterator().next();
        assertThat(record.key()).isEqualTo(messageKey);
        assertThat(record.value())
          .containsEntry("name", "Kafka Integration")
          .containsEntry("version", 1)
          .containsEntry("active", true);
      }
    }
  }

  @Nested
  class ProduceAndConsumeAvroIntegration {

    private static final String MOCK_SCHEMA_REGISTRY = "mock://integration-test";

    private static final String USER_SCHEMA_JSON = """
      {
        "type": "record",
        "name": "User",
        "namespace": "io.github.santannaf.kafka.avro",
        "fields": [
          {"name": "name", "type": "string"},
          {"name": "age", "type": "int"},
          {"name": "email", "type": "string"}
        ]
      }
      """;

    @Test
    void shouldProduceAndConsumeAvroMessage() throws Exception {
      var topicName = "test-avro-" + UUID.randomUUID();
      var messageKey = "key-avro";
      var schema = new Schema.Parser().parse(USER_SCHEMA_JSON);

      var avroRecord = new GenericData.Record(schema);
      avroRecord.put("name", "Thales");
      avroRecord.put("age", 30);
      avroRecord.put("email", "thales@test.com");

      createTopic(topicName);

      var producerProps = new HashMap<String, Object>();
      producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
      producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
      producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
      producerProps.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY);
      producerProps.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, true);

      try (var producer = new KafkaProducer<String, GenericRecord>(producerProps)) {
        producer.send(new ProducerRecord<>(topicName, messageKey, avroRecord)).get(10, TimeUnit.SECONDS);
      }

      var consumerProps = new HashMap<String, Object>();
      consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
      consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID());
      consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
      consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
      consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
      consumerProps.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY);
      consumerProps.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, false);

      try (var consumer = new KafkaConsumer<String, GenericRecord>(consumerProps)) {
        consumer.subscribe(Collections.singletonList(topicName));

        var records = pollUntilRecords(consumer, 30_000);

        assertThat(records.count()).isEqualTo(1);
        var record = records.iterator().next();
        assertThat(record.key()).isEqualTo(messageKey);

        var value = record.value();
        assertThat(value.get("name").toString()).isEqualTo("Thales");
        assertThat((int) value.get("age")).isEqualTo(30);
        assertThat(value.get("email").toString()).isEqualTo("thales@test.com");
      }
    }

    @Test
    void shouldProduceAndConsumeAvroMessageUsingSafeDeserializer() throws Exception {
      var topicName = "test-avro-safe-" + UUID.randomUUID();
      var messageKey = "key-avro-safe";
      var schema = new Schema.Parser().parse(USER_SCHEMA_JSON);

      var avroRecord = new GenericData.Record(schema);
      avroRecord.put("name", "Safe User");
      avroRecord.put("age", 25);
      avroRecord.put("email", "safe@test.com");

      createTopic(topicName);

      var producerProps = new HashMap<String, Object>();
      producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
      producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
      producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
      producerProps.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY);
      producerProps.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, true);

      try (var producer = new KafkaProducer<String, GenericRecord>(producerProps)) {
        producer.send(new ProducerRecord<>(topicName, messageKey, avroRecord)).get(10, TimeUnit.SECONDS);
      }

      var consumerProps = new HashMap<String, Object>();
      consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
      consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID());
      consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
      consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
      consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "io.github.santannaf.kafka.deserialize.SafeKafkaAvroDeserializer");
      consumerProps.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY);
      consumerProps.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, false);

      try (var consumer = new KafkaConsumer<String, Object>(consumerProps)) {
        consumer.subscribe(Collections.singletonList(topicName));

        var records = pollUntilRecords(consumer, 30_000);

        assertThat(records.count()).isEqualTo(1);
        var record = records.iterator().next();
        assertThat(record.key()).isEqualTo(messageKey);

        var value = (GenericRecord) record.value();
        assertThat(value.get("name").toString()).isEqualTo("Safe User");
        assertThat((int) value.get("age")).isEqualTo(25);
        assertThat(value.get("email").toString()).isEqualTo("safe@test.com");
      }
    }
  }

  @Nested
  class KafkaTemplateIntegration {

    @Test
    void kafkaTemplate_shouldBeCreatedWithRealBroker() {
      var producerFactory = factory.producerFactory();
      var admin = factory.kafkaAdmin();
      var template = factory.kafkaTemplate(producerFactory, admin);

      assertThat(template).isNotNull();
      assertThat(template.getKafkaAdmin()).isSameAs(admin);
    }
  }

  @Nested
  class ListenerContainerFactoryIntegration {

    @Test
    void listenerContainerFactory_shouldBeCreatedWithRealBroker() {
      var consumerFactory = factory.kafkaConsumerFactory();
      var listenerFactory = factory.configureListenerContainerFactory(consumerFactory, properties.consumer(), null);

      assertThat(listenerFactory).isNotNull();
      assertThat(listenerFactory.getContainerProperties().getAckMode())
        .isEqualTo(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL);
    }
  }

  @Nested
  class AnotherConnectionIntegration {

    @Test
    void shouldCreateFactoryComponentsForAnotherConnection() {
      var anotherBootstrap = kafka.getBootstrapServers();
      var anotherProducerFactory = factory.producerFactory(anotherBootstrap, "mock://another-sr:8081");
      var anotherAdmin = factory.kafkaAdmin(anotherBootstrap);
      var anotherTemplate = factory.kafkaTemplate(anotherProducerFactory, anotherAdmin);
      var anotherConsumerFactory = factory.kafkaConsumerFactory(anotherBootstrap, "mock://another-sr:8081");
      var anotherListener = factory.configureListenerContainerFactory(anotherConsumerFactory, properties.consumer(), null);

      assertThat(anotherProducerFactory).isNotNull();
      assertThat(anotherAdmin).isNotNull();
      assertThat(anotherTemplate).isNotNull();
      assertThat(anotherConsumerFactory).isNotNull();
      assertThat(anotherListener).isNotNull();

      assertThat(anotherAdmin.getConfigurationProperties())
        .containsEntry(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, anotherBootstrap);
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
    try (var adminClient = org.apache.kafka.clients.admin.AdminClient.create(
      Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers())
    )) {
      adminClient.createTopics(Collections.singletonList(new NewTopic(topicName, 1, (short) 1)))
        .all().get(10, TimeUnit.SECONDS);
    }
  }
}
