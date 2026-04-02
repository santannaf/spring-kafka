package io.github.santannaf.kafka.configuration.properties;

import org.springframework.boot.context.properties.bind.DefaultValue;

public record KafkaArchConsumerProperties(
  String consumerGroupId,
  @DefaultValue("manual") String ackConsumerConfig,
  @DefaultValue("latest") String eventAutoOffsetResetConfig,
  @DefaultValue("false") boolean enableAutoCommit,
  @DefaultValue("500") int maxPollRecords,
  @DefaultValue("300000") int maxPollIntervalMs,
  @DefaultValue("100000") int fetchMinBytes,
  @DefaultValue("500") int fetchMaxWaitBytes,
  @DefaultValue("20000") int sessionTimeoutMs,
  @DefaultValue("3000") int heartbeatIntervalMs,
  @DefaultValue("30000") int requestTimeoutConfigMs,
  @DefaultValue("true") boolean enableAvroReaderConfig,
  @DefaultValue("false") boolean enableBatchListener,
  @DefaultValue("false") boolean enableAsyncAck,
  @DefaultValue("3") int maxAttemptsConsumerRecord,
  @DefaultValue("10000") int intervalRetryAttemptsConsumerRecord,
  @DefaultValue("false") boolean enableVirtualThreads
) {}
