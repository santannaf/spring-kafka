package io.github.santannaf.kafka.configuration.components;

import io.github.santannaf.kafka.configuration.properties.KafkaArchCommonProperties;
import io.github.santannaf.kafka.configuration.properties.KafkaArchConsumerProperties;
import io.github.santannaf.kafka.configuration.properties.KafkaArchProducerProperties;
import io.github.santannaf.kafka.configuration.properties.TunedKafkaProperties;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaComponentsFactoryTest {

  private KafkaComponentsFactory factory;
  private TunedKafkaProperties properties;

  private KafkaArchCommonProperties defaultCommon() {
    return new KafkaArchCommonProperties(
      "localhost:9092", null, "dev", "http://localhost:8081",
      false, null, null, null, null, null,
      50, 2000, 2, false, null, null
    );
  }

  private KafkaArchProducerProperties defaultProducer() {
    return new KafkaArchProducerProperties(
      "all", 5, 20000, 10, false, "none", null, null, false
    );
  }

  private KafkaArchConsumerProperties defaultConsumer() {
    return new KafkaArchConsumerProperties(
      "test-group", "manual", "latest", false, 500, 300000,
      100000, 500, 20000, 3000, 30000, true, false, false, 3, 10000, false
    );
  }

  @BeforeEach
  void setUp() {
    properties = new TunedKafkaProperties(defaultCommon(), defaultProducer(), defaultConsumer());
    factory = new KafkaComponentsFactory(properties);
  }

  @Nested
  class ProducerFactoryTests {

    @Test
    void producerFactory_shouldCreateWithDefaultBootstrap() {
      var producerFactory = factory.producerFactory();
      assertThat(producerFactory).isNotNull();
    }

    @Test
    void producerFactory_shouldCreateWithCustomBootstrap() {
      var producerFactory = factory.producerFactory("custom-broker:9092", "http://custom-sr:8081");
      assertThat(producerFactory).isNotNull();
    }
  }

  @Nested
  class KafkaAdminTests {

    @Test
    void kafkaAdmin_shouldCreateWithDefaultBootstrap() {
      var admin = factory.kafkaAdmin();
      assertThat(admin).isNotNull();
      assertThat(admin.getConfigurationProperties())
        .containsEntry(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    }

    @Test
    void kafkaAdmin_shouldCreateWithCustomBootstrap() {
      var admin = factory.kafkaAdmin("another-broker:9092");
      assertThat(admin).isNotNull();
      assertThat(admin.getConfigurationProperties())
        .containsEntry(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "another-broker:9092");
    }
  }

  @Nested
  class KafkaTemplateTests {

    @Test
    void kafkaTemplate_shouldCreateWithObservabilityEnabled() {
      var producerFactory = factory.producerFactory();
      var admin = factory.kafkaAdmin();
      var template = factory.kafkaTemplate(producerFactory, admin);

      assertThat(template).isNotNull();
      assertThat(template.getKafkaAdmin()).isSameAs(admin);
    }
  }

  @Nested
  class ConsumerFactoryTests {

    @Test
    void kafkaConsumerFactory_shouldCreateWithDefaultBootstrap() {
      var consumerFactory = factory.kafkaConsumerFactory();
      assertThat(consumerFactory).isNotNull();
    }

    @Test
    void kafkaConsumerFactory_shouldCreateWithCustomBootstrap() {
      var consumerFactory = factory.kafkaConsumerFactory("custom-broker:9092", "http://custom-sr:8081");
      assertThat(consumerFactory).isNotNull();
    }
  }

  @Nested
  class ListenerContainerFactoryTests {

    @Test
    void configureListenerContainerFactory_shouldSetAckModeFromProperties() {
      var consumerFactory = factory.kafkaConsumerFactory();
      var listenerFactory = factory.configureListenerContainerFactory(consumerFactory, properties.consumer(), null);

      assertThat(listenerFactory).isNotNull();
      assertThat(listenerFactory.getContainerProperties().getAckMode())
        .isEqualTo(ContainerProperties.AckMode.MANUAL);
    }

    @Test
    void configureListenerContainerFactory_shouldApplyBatchListenerSetting() {
      var batchConsumer = new KafkaArchConsumerProperties(
        "test-group", "batch", "latest", false, 500, 300000,
        100000, 500, 20000, 3000, 30000, true, true, false, 3, 10000, false
      );
      var consumerFactory = factory.kafkaConsumerFactory();
      var listenerFactory = factory.configureListenerContainerFactory(consumerFactory, batchConsumer, null);

      assertThat(listenerFactory.isBatchListener()).isTrue();
    }

    @Test
    void configureListenerContainerFactory_shouldSetErrorHandler_whenProvided() {
      CommonErrorHandler errorHandler = new DefaultErrorHandler();
      var consumerFactory = factory.kafkaConsumerFactory();
      var listenerFactory = factory.configureListenerContainerFactory(consumerFactory, properties.consumer(), errorHandler);

      assertThat(listenerFactory).isNotNull();
    }

    @Test
    void configureListenerContainerFactory_shouldNotFail_whenErrorHandlerIsNull() {
      var consumerFactory = factory.kafkaConsumerFactory();
      var listenerFactory = factory.configureListenerContainerFactory(consumerFactory, properties.consumer(), null);

      assertThat(listenerFactory).isNotNull();
    }
  }

  @Nested
  class SSLValidationTests {

    @Test
    void producerFactory_shouldThrow_whenSslEnabledButNoTrustStoreLocation() {
      var sslCommon = new KafkaArchCommonProperties(
        "broker:9093", null, "dev", "http://sr:8081",
        true, null, null, "pass", null, "pass",
        50, 2000, 2, false, null, null
      );
      var props = new TunedKafkaProperties(sslCommon, defaultProducer(), defaultConsumer());
      var sslFactory = new KafkaComponentsFactory(props);

      assertThatThrownBy(sslFactory::producerFactory)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("certificate-type");
    }

    @Test
    void producerFactory_shouldThrow_whenSslEnabledButNoPasswords() {
      var sslCommon = new KafkaArchCommonProperties(
        "broker:9093", null, "dev", "http://sr:8081",
        true, null, "/path/truststore", null, "/path/keystore", null,
        50, 2000, 2, false, null, null
      );
      var props = new TunedKafkaProperties(sslCommon, defaultProducer(), defaultConsumer());
      var sslFactory = new KafkaComponentsFactory(props);

      assertThatThrownBy(sslFactory::producerFactory)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("passwords das chaves");
    }

    @Test
    void producerFactory_shouldThrow_whenSslEnabledButPort9092() {
      var sslCommon = new KafkaArchCommonProperties(
        "broker:9092", null, "dev", "http://sr:8081",
        true, null, "/path/truststore", "pass", "/path/keystore", "pass",
        50, 2000, 2, false, null, null
      );
      var props = new TunedKafkaProperties(sslCommon, defaultProducer(), defaultConsumer());
      var sslFactory = new KafkaComponentsFactory(props);

      assertThatThrownBy(sslFactory::producerFactory)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("9093");
    }

    @Test
    void consumerFactory_shouldThrow_whenSslEnabledButNoTrustStoreLocation() {
      var sslCommon = new KafkaArchCommonProperties(
        "broker:9093", null, "dev", "http://sr:8081",
        true, null, null, "pass", null, "pass",
        50, 2000, 2, false, null, null
      );
      var props = new TunedKafkaProperties(sslCommon, defaultProducer(), defaultConsumer());
      var sslFactory = new KafkaComponentsFactory(props);

      assertThatThrownBy(sslFactory::kafkaConsumerFactory)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("certificate-type");
    }
  }
}
