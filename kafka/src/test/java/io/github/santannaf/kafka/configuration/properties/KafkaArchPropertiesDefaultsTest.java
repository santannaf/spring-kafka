package io.github.santannaf.kafka.configuration.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaArchPropertiesDefaultsTest {

  private final KafkaArchCommonProperties common = new KafkaArchCommonProperties(
    "localhost:9092", null, "dev", "localhost:8081",
    false, null, null, null, null, null,
    50, 2000, 2, false, null, null
  );

  private final KafkaArchProducerProperties producer = new KafkaArchProducerProperties(
    "all", 5, 20000, 10, false, "none", null, null, false
  );

  private final KafkaArchConsumerProperties consumer = new KafkaArchConsumerProperties(
    null, "manual", "latest", false, 500, 300000,
    100000, 500, 20000, 3000, 30000, true, false, false, 3, 10000, false
  );

  @Test
  void commonProperties_shouldHaveCorrectDefaults() {
    assertThat(common.bootstrapServers()).isEqualTo("localhost:9092");
    assertThat(common.schemaRegistry()).isEqualTo("localhost:8081");
    assertThat(common.clientId()).isNull();
    assertThat(common.enableConnectionSslProtocolMode()).isFalse();
    assertThat(common.reconnectBackoff()).isEqualTo(50);
    assertThat(common.reconnectBackoffMax()).isEqualTo(2000);
    assertThat(common.eventsConcurrency()).isEqualTo(2);
    assertThat(common.enableAnotherConnection()).isFalse();
    assertThat(common.anotherBootstrapServers()).isNull();
    assertThat(common.anotherSchemaRegistry()).isNull();
  }

  @Test
  void producerProperties_shouldHaveCorrectDefaults() {
    assertThat(producer.ackProducerConfig()).isEqualTo("all");
    assertThat(producer.maxProducerRetry()).isEqualTo(5);
    assertThat(producer.batchSize()).isEqualTo(20000);
    assertThat(producer.lingerMs()).isEqualTo(10);
    assertThat(producer.enableIdempotenceConfig()).isFalse();
    assertThat(producer.compressType()).isEqualTo("none");
    assertThat(producer.typePartitioner()).isNull();
    assertThat(producer.transactionalId()).isNull();
    assertThat(producer.enableReactiveProject()).isFalse();
  }

  @Test
  void consumerProperties_shouldHaveCorrectDefaults() {
    assertThat(consumer.consumerGroupId()).isNull();
    assertThat(consumer.ackConsumerConfig()).isEqualTo("manual");
    assertThat(consumer.eventAutoOffsetResetConfig()).isEqualTo("latest");
    assertThat(consumer.enableAutoCommit()).isFalse();
    assertThat(consumer.maxPollRecords()).isEqualTo(500);
    assertThat(consumer.maxPollIntervalMs()).isEqualTo(300000);
    assertThat(consumer.fetchMinBytes()).isEqualTo(100000);
    assertThat(consumer.fetchMaxWaitBytes()).isEqualTo(500);
    assertThat(consumer.sessionTimeoutMs()).isEqualTo(20000);
    assertThat(consumer.heartbeatIntervalMs()).isEqualTo(3000);
    assertThat(consumer.requestTimeoutConfigMs()).isEqualTo(30000);
    assertThat(consumer.enableAvroReaderConfig()).isTrue();
    assertThat(consumer.enableBatchListener()).isFalse();
    assertThat(consumer.enableAsyncAck()).isFalse();
    assertThat(consumer.maxAttemptsConsumerRecord()).isEqualTo(3);
    assertThat(consumer.intervalRetryAttemptsConsumerRecord()).isEqualTo(10000);
  }

  @Test
  void tunnedKafkaProperties_shouldComposeAllThreeRecords() {
    var properties = new TunedKafkaProperties(common, producer, consumer);

    assertThat(properties.common()).isSameAs(common);
    assertThat(properties.producer()).isSameAs(producer);
    assertThat(properties.consumer()).isSameAs(consumer);
  }
}
