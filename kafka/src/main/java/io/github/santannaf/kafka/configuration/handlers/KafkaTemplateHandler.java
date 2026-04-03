package io.github.santannaf.kafka.configuration.handlers;

import io.github.santannaf.kafka.configuration.properties.TunedKafkaProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class KafkaTemplateHandler {
  private final ApplicationContext context;
  private final TunedKafkaProperties properties;

  public KafkaTemplateHandler(ApplicationContext context, TunedKafkaProperties properties) {
    this.context = context;
    this.properties = properties;
  }
}
