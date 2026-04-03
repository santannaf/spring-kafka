package io.github.santannaf.kafka.configuration.properties;

import org.springframework.kafka.listener.ContainerProperties;

import java.util.Map;

public enum TunedPropertiesAckConsumerProperties {
  RECORD("record", ContainerProperties.AckMode.RECORD),
  BATCH("batch", ContainerProperties.AckMode.BATCH),
  TIME("time", ContainerProperties.AckMode.TIME),
  COUNT("count", ContainerProperties.AckMode.COUNT),
  COUNT_TIME("count_time", ContainerProperties.AckMode.COUNT_TIME),
  MANUAL_IMMEDIATE("manual_immediate", ContainerProperties.AckMode.MANUAL_IMMEDIATE),
  MANUAL("manual", ContainerProperties.AckMode.MANUAL);

  private static final Map<String, ContainerProperties.AckMode> LOOKUP;

  static {
    LOOKUP = Map.of(
      "record", ContainerProperties.AckMode.RECORD,
      "batch", ContainerProperties.AckMode.BATCH,
      "time", ContainerProperties.AckMode.TIME,
      "count", ContainerProperties.AckMode.COUNT,
      "count_time", ContainerProperties.AckMode.COUNT_TIME,
      "manual_immediate", ContainerProperties.AckMode.MANUAL_IMMEDIATE,
      "manual", ContainerProperties.AckMode.MANUAL
    );
  }

  private final String value;
  private final ContainerProperties.AckMode ackMode;

  TunedPropertiesAckConsumerProperties(String value, ContainerProperties.AckMode ackMode) {
    this.value = value;
    this.ackMode = ackMode;
  }

  public static ContainerProperties.AckMode fromValue(String mode) {
    return LOOKUP.getOrDefault(mode, ContainerProperties.AckMode.MANUAL);
  }
}
