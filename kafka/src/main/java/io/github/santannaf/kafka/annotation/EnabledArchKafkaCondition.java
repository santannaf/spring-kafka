package io.github.santannaf.kafka.annotation;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

@Aspect
@Configuration
public class EnabledArchKafkaCondition {
  private static final Logger log = LoggerFactory.getLogger(EnabledArchKafkaCondition.class);

  private final ApplicationContext applicationContext;

  public EnabledArchKafkaCondition(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Before("execution(* org.springframework.boot.SpringApplication.run(..))")
  public void checkForEnabledArchKafkaAnnotation() {
    for (String beanName : applicationContext.getBeanDefinitionNames()) {
      try {
        Class<?> beanClass = applicationContext.getBean(beanName).getClass();
        if (beanClass.isAnnotationPresent(EnabledArchKafka.class)) {
          log.info("Kafka configuration has been activated!");
          return;
        }
      }
      catch (Exception e) {
        log.debug("Could not inspect bean {} for @EnabledArchKafka", beanName);
      }
    }
  }
}
