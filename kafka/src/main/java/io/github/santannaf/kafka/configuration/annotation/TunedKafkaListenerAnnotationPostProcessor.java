package io.github.santannaf.kafka.configuration.annotation;

import io.github.santannaf.kafka.annotation.TunedKafkaListener;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.KafkaListenerAnnotationBeanPostProcessor;

import java.lang.reflect.Method;

public class TunedKafkaListenerAnnotationPostProcessor extends KafkaListenerAnnotationBeanPostProcessor<String, Object> implements ApplicationContextAware {
  @Override
  public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
    super.setApplicationContext(applicationContext);
  }

  @Override
  protected void processKafkaListener(@NonNull KafkaListener kafkaListener,
                                      @NonNull Method method,
                                      @NonNull Object bean,
                                      @NonNull String beanName) {
    TunedKafkaListener tunnedKafkaListener = new TunedKafkaListener(kafkaListener);
    super.processKafkaListener(tunnedKafkaListener, method, bean, beanName);
  }
}
