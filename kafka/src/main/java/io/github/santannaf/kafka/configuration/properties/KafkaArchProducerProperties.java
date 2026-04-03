package io.github.santannaf.kafka.configuration.properties;

import org.springframework.boot.context.properties.bind.DefaultValue;

public record KafkaArchProducerProperties(
  @DefaultValue("all") String ackProducerConfig,
  @DefaultValue("5") int maxProducerRetry,
  @DefaultValue("20000") int batchSize,
  @DefaultValue("10") int lingerMs,
  @DefaultValue("false") boolean enableIdempotenceConfig,
  @DefaultValue("none") String compressType,
  String typePartitioner,
  String transactionalId,
  @DefaultValue("false") boolean enableReactiveProject
) {}
