package io.github.santannaf.kafka.configuration.properties;

import io.confluent.kafka.serializers.context.strategy.ContextNameStrategy;

public class MyNullClassStrategy implements ContextNameStrategy {
  @Override
  public String contextName(String topic) {
    return "empty-context";
  }
}
