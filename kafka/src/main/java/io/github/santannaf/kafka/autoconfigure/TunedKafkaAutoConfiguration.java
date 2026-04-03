package io.github.santannaf.kafka.autoconfigure;

import io.github.santannaf.kafka.annotation.ConditionalOnEnableArchKafka;
import io.github.santannaf.kafka.aot.KafkaLibraryRuntimeHints;
import io.github.santannaf.kafka.configuration.components.KafkaComponentsFactory;
import org.springframework.context.annotation.ImportRuntimeHints;
import io.github.santannaf.kafka.configuration.properties.TunedKafkaProperties;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;

import java.util.Optional;

@Configuration
@EnableConfigurationProperties({TunedKafkaProperties.class})
@ConditionalOnEnableArchKafka
@ConditionalOnClass({KafkaTemplate.class})
@AutoConfigureBefore({KafkaAutoConfiguration.class})
@ImportRuntimeHints(KafkaLibraryRuntimeHints.class)
public class TunedKafkaAutoConfiguration {

  @Bean
  public KafkaComponentsFactory kafkaComponentsFactory(final TunedKafkaProperties properties) {
    return new KafkaComponentsFactory(properties);
  }

  @Bean
  @Primary
  public ProducerFactory<String, Object> producerFactory(KafkaComponentsFactory kafkaComponentsFactory) {
    return kafkaComponentsFactory.producerFactory();
  }

  @Bean
  public KafkaAdmin kafkaAdmin(KafkaComponentsFactory kafkaComponentsFactory) {
    return kafkaComponentsFactory.kafkaAdmin();
  }

  @Bean
  @DependsOn("producerFactory")
  public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory, KafkaAdmin kafkaAdmin, KafkaComponentsFactory kafkaComponentsFactory) {
    return kafkaComponentsFactory.kafkaTemplate(producerFactory, kafkaAdmin);
  }

  @Bean
  @Primary
  public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
    final KafkaComponentsFactory kafkaComponentsFactory,
    Optional<CommonErrorHandler> errorHandlerOptional,
    final TunedKafkaProperties properties
  ) {
    var consumerFactory = kafkaComponentsFactory.kafkaConsumerFactory();
    return kafkaComponentsFactory.configureListenerContainerFactory(consumerFactory, properties.consumer(), errorHandlerOptional.orElse(null));
  }

  @Configuration
  @ConditionalOnProperty(prefix = "kafka.listener.common", name = "enable-another-connection", havingValue = "true")
  static class AnotherConnectionAutoConfiguration {

    @Bean
    public ProducerFactory<String, Object> anotherProducerFactory(KafkaComponentsFactory kafkaComponentsFactory, TunedKafkaProperties properties) {
      var common = properties.common();
      return kafkaComponentsFactory.producerFactory(common.anotherBootstrapServers(), common.anotherSchemaRegistry());
    }

    @Bean
    public KafkaAdmin anotherKafkaAdmin(KafkaComponentsFactory kafkaComponentsFactory, TunedKafkaProperties properties) {
      return kafkaComponentsFactory.kafkaAdmin(properties.common().anotherBootstrapServers());
    }

    @Bean
    @DependsOn("anotherProducerFactory")
    public KafkaTemplate<String, Object> anotherKafkaTemplate(ProducerFactory<String, Object> anotherProducerFactory, KafkaAdmin anotherKafkaAdmin, KafkaComponentsFactory kafkaComponentsFactory) {
      return kafkaComponentsFactory.kafkaTemplate(anotherProducerFactory, anotherKafkaAdmin);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> anotherKafkaListenerContainerFactory(
      final KafkaComponentsFactory kafkaComponentsFactory,
      Optional<CommonErrorHandler> errorHandlerOptional,
      final TunedKafkaProperties properties
    ) {
      var common = properties.common();
      var consumerFactory = kafkaComponentsFactory.kafkaConsumerFactory(common.anotherBootstrapServers(), common.anotherSchemaRegistry());
      return kafkaComponentsFactory.configureListenerContainerFactory(consumerFactory, properties.consumer(), errorHandlerOptional.orElse(null));
    }
  }
}
