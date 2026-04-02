package io.github.santannaf.kafka.autoconfigure;

import io.github.santannaf.kafka.configuration.components.KafkaComponentsFactory;
import io.github.santannaf.kafka.configuration.properties.KafkaArchCommonProperties;
import io.github.santannaf.kafka.configuration.properties.KafkaArchConsumerProperties;
import io.github.santannaf.kafka.configuration.properties.KafkaArchProducerProperties;
import io.github.santannaf.kafka.configuration.properties.TunedKafkaProperties;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class TunedKafkaAutoConfigurationTest {

  @Configuration
  static class TestPropertiesConfig {
    @Bean
    public TunedKafkaProperties tunnedKafkaProperties() {
      return new TunedKafkaProperties(
        new KafkaArchCommonProperties(
          "localhost:9092", null, "dev", "http://localhost:8081",
          false, null, null, null, null, null,
          50, 2000, 2, false, null, null
        ),
        new KafkaArchProducerProperties("all", 5, 20000, 10, false, "none", null, null, false),
        new KafkaArchConsumerProperties("test-group", "manual", "latest", false, 500, 300000, 100000, 500, 20000, 3000, 30000, true, false, false, 3, 10000, false)
      );
    }

    @Bean
    public KafkaComponentsFactory kafkaComponentsFactory(TunedKafkaProperties properties) {
      return new KafkaComponentsFactory(properties);
    }
  }

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
    .withUserConfiguration(TestPropertiesConfig.class);

  @Nested
  class PrimaryBeans {

    @Test
    void shouldRegisterKafkaComponentsFactory() {
      contextRunner.run(context -> {
        assertThat(context).hasSingleBean(KafkaComponentsFactory.class);
      });
    }

    @Test
    void shouldCreateProducerFactory() {
      contextRunner.run(context -> {
        var factory = context.getBean(KafkaComponentsFactory.class);
        var producerFactory = factory.producerFactory();
        assertThat(producerFactory).isNotNull();
      });
    }

    @Test
    void shouldCreateKafkaAdmin() {
      contextRunner.run(context -> {
        var factory = context.getBean(KafkaComponentsFactory.class);
        var admin = factory.kafkaAdmin();
        assertThat(admin).isNotNull();
      });
    }

    @Test
    void shouldCreateKafkaTemplate() {
      contextRunner.run(context -> {
        var factory = context.getBean(KafkaComponentsFactory.class);
        var producerFactory = factory.producerFactory();
        var admin = factory.kafkaAdmin();
        var template = factory.kafkaTemplate(producerFactory, admin);
        assertThat(template).isNotNull();
        assertThat(template.getKafkaAdmin()).isSameAs(admin);
      });
    }

    @Test
    void shouldCreateListenerContainerFactory() {
      contextRunner.run(context -> {
        var factory = context.getBean(KafkaComponentsFactory.class);
        var props = context.getBean(TunedKafkaProperties.class);
        var consumerFactory = factory.kafkaConsumerFactory();
        var listenerFactory = factory.configureListenerContainerFactory(consumerFactory, props.consumer(), null);
        assertThat(listenerFactory).isNotNull();
      });
    }
  }

  @Nested
  class AnotherConnectionBeans {

    @Test
    void shouldCreateAnotherConnectionBeans_whenEnabled() {
      contextRunner.run(context -> {
        var anotherCommon = new KafkaArchCommonProperties(
          "localhost:9092", null, "dev", "http://localhost:8081",
          false, null, null, null, null, null,
          50, 2000, 2, true, "another-broker:9092", "http://another-sr:8081"
        );
        var props = context.getBean(TunedKafkaProperties.class);
        var factory = context.getBean(KafkaComponentsFactory.class);

        var anotherProducer = factory.producerFactory(anotherCommon.anotherBootstrapServers(), anotherCommon.anotherSchemaRegistry());
        var anotherAdmin = factory.kafkaAdmin(anotherCommon.anotherBootstrapServers());
        var anotherTemplate = factory.kafkaTemplate(anotherProducer, anotherAdmin);
        var anotherConsumer = factory.kafkaConsumerFactory(anotherCommon.anotherBootstrapServers(), anotherCommon.anotherSchemaRegistry());
        var anotherListener = factory.configureListenerContainerFactory(anotherConsumer, props.consumer(), null);

        assertThat(anotherProducer).isNotNull();
        assertThat(anotherAdmin).isNotNull();
        assertThat(anotherTemplate).isNotNull();
        assertThat(anotherListener).isNotNull();
      });
    }
  }
}
