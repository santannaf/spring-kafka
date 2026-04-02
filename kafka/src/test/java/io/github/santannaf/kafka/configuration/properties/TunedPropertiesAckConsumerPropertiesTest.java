package io.github.santannaf.kafka.configuration.properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.kafka.listener.ContainerProperties;

import static org.assertj.core.api.Assertions.assertThat;

class TunedPropertiesAckConsumerPropertiesTest {

  @ParameterizedTest
  @CsvSource({
    "record, RECORD",
    "batch, BATCH",
    "time, TIME",
    "count, COUNT",
    "count_time, COUNT_TIME",
    "manual_immediate, MANUAL_IMMEDIATE",
    "manual, MANUAL"
  })
  void fromValue_shouldReturnCorrectAckMode(String input, String expectedName) {
    var expected = ContainerProperties.AckMode.valueOf(expectedName);
    assertThat(TunedPropertiesAckConsumerProperties.fromValue(input)).isEqualTo(expected);
  }

  @Test
  void fromValue_shouldReturnManualAsDefault_whenUnknownValue() {
    assertThat(TunedPropertiesAckConsumerProperties.fromValue("unknown"))
      .isEqualTo(ContainerProperties.AckMode.MANUAL);
  }

  @Test
  void fromValue_shouldReturnCorrectAckMode_forManualImmediate() {
    assertThat(TunedPropertiesAckConsumerProperties.fromValue("manual_immediate"))
      .isEqualTo(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
  }
}
