package io.github.santannaf.kafka.configuration.properties;

import org.springframework.boot.context.properties.bind.DefaultValue;

public record KafkaArchCommonProperties(
  @DefaultValue("localhost:9092") String bootstrapServers,
  String clientId,
  @DefaultValue("dev") String environment,
  @DefaultValue("localhost:8081") String schemaRegistry,
  @DefaultValue("false") boolean enableConnectionSslProtocolMode,
  String certificateType,
  String sslTrustStoreLocation,
  String sslTrustStorePassword,
  String sslKeyStoreLocation,
  String sslKeyStorePassword,
  @DefaultValue("50") int reconnectBackoff,
  @DefaultValue("2000") int reconnectBackoffMax,
  @DefaultValue("2") int eventsConcurrency,
  @DefaultValue("false") boolean enableAnotherConnection,
  String anotherBootstrapServers,
  String anotherSchemaRegistry
) {}
