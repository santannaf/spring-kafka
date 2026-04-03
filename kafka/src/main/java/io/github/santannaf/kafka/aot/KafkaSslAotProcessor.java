package io.github.santannaf.kafka.aot;

import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;

/**
 * AOT processor that registers resource hints for client-provided SSL certificates (external mode).
 * Internal mode (.p12 inside the lib) is handled by KafkaLibraryRuntimeHints classpath scan.
 */
public class KafkaSslAotProcessor implements BeanFactoryInitializationAotProcessor {

  @Override
  public BeanFactoryInitializationAotContribution processAheadOfTime(
    ConfigurableListableBeanFactory beanFactory) {

    var env = beanFactory.getBean(Environment.class);
    var sslEnabled = env.getProperty(
      "kafka.arch.common.enable-connection-ssl-protocol-mode", Boolean.class, false);
    var certificateType = env.getProperty("kafka.arch.common.certificate-type");

    // Internal mode: hints already covered by KafkaLibraryRuntimeHints classpath scan
    // No SSL: nothing to do
    if (!sslEnabled || certificateType != null) return null;

    var truststore = env.getProperty("kafka.arch.common.ssl-trust-store-location");
    var keystore = env.getProperty("kafka.arch.common.ssl-key-store-location");

    return (context, code) -> {
      var hints = context.getRuntimeHints();
      registerIfClasspath(hints, truststore);
      registerIfClasspath(hints, keystore);
    };
  }

  private void registerIfClasspath(org.springframework.aot.hint.RuntimeHints hints, String path) {
    if (path == null || path.startsWith("/") || path.startsWith("file:")) return;
    var resource = path.startsWith("classpath:") ? path.replace("classpath:", "") : path;
    hints.resources().registerPattern(resource);
  }
}
