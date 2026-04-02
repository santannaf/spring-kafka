package io.github.santannaf.kafka.annotation;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Objects;

public class OnEnableArchKafkaCondition implements Condition {
  private static final Logger log = LoggerFactory.getLogger(OnEnableArchKafkaCondition.class);

  @Override
  public boolean matches(ConditionContext context, @Nonnull AnnotatedTypeMetadata metadata) {
    var registry = context.getRegistry();

    for (var beanName : registry.getBeanDefinitionNames()) {
      var definition = registry.getBeanDefinition(beanName);
      var className = definition.getBeanClassName();
      if (className == null || className.isEmpty()) continue;

      try {
        var clazz = Objects.requireNonNull(context.getClassLoader()).loadClass(className);
        var annotation = clazz.getAnnotation(EnabledArchKafka.class);

        if (annotation != null) {
          log.info("@EnabledArchKafka detected on class {}, appName: {}", className, annotation.appName());
          return true;
        }
      }
      catch (ClassNotFoundException e) {
        log.debug("Could not load class {} for @EnabledArchKafka check, skipping", className);
      }
    }

    return false;
  }
}
