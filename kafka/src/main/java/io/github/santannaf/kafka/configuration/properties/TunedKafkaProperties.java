package io.github.santannaf.kafka.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "kafka.arch")
public record TunedKafkaProperties(
  @DefaultValue KafkaArchCommonProperties common,
  @DefaultValue KafkaArchProducerProperties producer,
  @DefaultValue KafkaArchConsumerProperties consumer
) {}
