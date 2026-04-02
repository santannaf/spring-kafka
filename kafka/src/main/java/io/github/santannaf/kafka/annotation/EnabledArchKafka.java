package io.github.santannaf.kafka.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE) // Pode ser usada em classes
@Retention(RetentionPolicy.RUNTIME)
@Import(KafkaAnalysisImportSelector.class)
public @interface EnabledArchKafka {
  String appName() default "";
}
