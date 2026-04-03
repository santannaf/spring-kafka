package io.github.santannaf.kafka.annotation;

import org.aspectj.lang.annotation.Pointcut;

public class CommonJoinPointConfig {
  @Pointcut("@annotation(io.github.santannaf.kafka.annotation.EnabledArchKafka)")
  public void enabledArchKafkaAnnotation() {
    // Do nothing because the whole logic are around the Pointcut annotation.
  }
}
