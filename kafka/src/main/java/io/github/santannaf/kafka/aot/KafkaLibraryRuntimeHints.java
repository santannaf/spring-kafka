package io.github.santannaf.kafka.aot;

import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import io.github.santannaf.kafka.annotation.CommonJoinPointConfig;
import io.github.santannaf.kafka.annotation.ConditionalOnEnableArchKafka;
import io.github.santannaf.kafka.annotation.EnableArchKafkaState;
import io.github.santannaf.kafka.annotation.EnabledArchKafka;
import io.github.santannaf.kafka.annotation.EnabledArchKafkaCondition;
import io.github.santannaf.kafka.annotation.KafkaAnalysisImportSelector;
import io.github.santannaf.kafka.annotation.OnEnableArchKafkaCondition;
import io.github.santannaf.kafka.annotation.TunedKafkaListener;
import io.github.santannaf.kafka.autoconfigure.TunedEnvironmentPostProcessor;
import io.github.santannaf.kafka.autoconfigure.TunedKafkaAutoConfiguration;
import io.github.santannaf.kafka.configuration.annotation.TunedKafkaListenerAnnotationPostProcessor;
import io.github.santannaf.kafka.configuration.components.KafkaComponentsFactory;
import io.github.santannaf.kafka.configuration.handlers.KafkaTemplateHandler;
import io.github.santannaf.kafka.configuration.ssl.SslCertificateResolver;
import io.github.santannaf.kafka.configuration.properties.KafkaArchCommonProperties;
import io.github.santannaf.kafka.configuration.properties.KafkaArchConsumerProperties;
import io.github.santannaf.kafka.configuration.properties.KafkaArchProducerProperties;
import io.github.santannaf.kafka.configuration.properties.MyNullClassStrategy;
import io.github.santannaf.kafka.configuration.properties.TunedPropertiesAckConsumerProperties;
import io.github.santannaf.kafka.configuration.properties.TunedKafkaProperties;
import io.github.santannaf.kafka.configuration.properties.TunedKafkaPropertiesBindHandlerAdvisor;
import io.github.santannaf.kafka.deserialize.SafeKafkaAvroDeserializer;
import org.jspecify.annotations.NonNull;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class KafkaLibraryRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(@NonNull RuntimeHints hints, ClassLoader classLoader) {
    registerConfigurationPropertiesHints(hints);
    registerAutoConfigurationHints(hints);
    registerAnnotationHints(hints);
    registerSerializationHints(hints);
    registerThirdPartyHints(hints);
    registerResourceHints(hints);
    registerProxyHints(hints);
  }

  private void registerConfigurationPropertiesHints(RuntimeHints hints) {
    // Records used for @ConfigurationProperties binding require full reflection
    var reflection = hints.reflection();

    for (var type : new Class<?>[]{
      TunedKafkaProperties.class,
      KafkaArchCommonProperties.class,
      KafkaArchProducerProperties.class,
      KafkaArchConsumerProperties.class
    }) {
      reflection.registerType(type,
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
        MemberCategory.INVOKE_DECLARED_METHODS,
        MemberCategory.INVOKE_PUBLIC_METHODS,
        MemberCategory.ACCESS_DECLARED_FIELDS);
    }

    // Enum used in ack config resolution
    reflection.registerType(TunedPropertiesAckConsumerProperties.class,
      MemberCategory.INVOKE_PUBLIC_METHODS,
      MemberCategory.ACCESS_DECLARED_FIELDS);

    // Bind handler advisor
    reflection.registerType(TunedKafkaPropertiesBindHandlerAdvisor.class,
      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
      MemberCategory.INVOKE_DECLARED_METHODS);

    // EnableArchKafkaState record
    reflection.registerType(EnableArchKafkaState.class,
      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
      MemberCategory.INVOKE_DECLARED_METHODS);
  }

  private void registerAutoConfigurationHints(RuntimeHints hints) {
    var reflection = hints.reflection();

    reflection.registerType(TunedKafkaAutoConfiguration.class,
      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
      MemberCategory.INVOKE_DECLARED_METHODS);

    // Inner configuration class
    for (var innerClass : TunedKafkaAutoConfiguration.class.getDeclaredClasses()) {
      reflection.registerType(innerClass,
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
        MemberCategory.INVOKE_DECLARED_METHODS);
    }

    reflection.registerType(TunedEnvironmentPostProcessor.class,
      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
      MemberCategory.INVOKE_DECLARED_METHODS);

    reflection.registerType(KafkaComponentsFactory.class,
      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
      MemberCategory.INVOKE_DECLARED_METHODS,
      MemberCategory.INVOKE_PUBLIC_METHODS);

    reflection.registerType(KafkaTemplateHandler.class,
      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
      MemberCategory.INVOKE_DECLARED_METHODS);

    reflection.registerType(SslCertificateResolver.class,
      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
      MemberCategory.INVOKE_DECLARED_METHODS,
      MemberCategory.INVOKE_PUBLIC_METHODS);
  }

  private void registerAnnotationHints(RuntimeHints hints) {
    var reflection = hints.reflection();

    reflection.registerType(EnabledArchKafka.class,
      MemberCategory.INVOKE_DECLARED_METHODS);

    reflection.registerType(ConditionalOnEnableArchKafka.class,
      MemberCategory.INVOKE_DECLARED_METHODS);

    reflection.registerType(OnEnableArchKafkaCondition.class,
      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
      MemberCategory.INVOKE_DECLARED_METHODS);

    reflection.registerType(KafkaAnalysisImportSelector.class,
      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
      MemberCategory.INVOKE_DECLARED_METHODS);

    // AOP aspect
    reflection.registerType(EnabledArchKafkaCondition.class,
      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
      MemberCategory.INVOKE_DECLARED_METHODS);

    reflection.registerType(CommonJoinPointConfig.class,
      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
      MemberCategory.INVOKE_DECLARED_METHODS);

    // KafkaListener adapter
    reflection.registerType(TunedKafkaListener.class,
      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
      MemberCategory.INVOKE_DECLARED_METHODS,
      MemberCategory.INVOKE_PUBLIC_METHODS);

    reflection.registerType(TunedKafkaListenerAnnotationPostProcessor.class,
      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
      MemberCategory.INVOKE_DECLARED_METHODS);
  }

  private void registerSerializationHints(RuntimeHints hints) {
    var reflection = hints.reflection();

    // Custom deserializer
    reflection.registerType(SafeKafkaAvroDeserializer.class,
      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
      MemberCategory.INVOKE_DECLARED_METHODS,
      MemberCategory.INVOKE_PUBLIC_METHODS);

    // Confluent strategy implementation
    reflection.registerType(MyNullClassStrategy.class,
      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
      MemberCategory.INVOKE_DECLARED_METHODS);
  }

  private void registerThirdPartyHints(RuntimeHints hints) {
    var reflection = hints.reflection();

    // Kafka serializers/deserializers instantiated by class name in Kafka config
    registerClassByName(reflection, "org.apache.kafka.common.serialization.StringSerializer");
    registerClassByName(reflection, "org.apache.kafka.common.serialization.StringDeserializer");
    registerClassByName(reflection, "io.confluent.kafka.serializers.KafkaAvroSerializer");
    registerClassByName(reflection, "io.confluent.kafka.serializers.KafkaAvroDeserializer");

    // Partitioner loaded by class name
    registerClassByName(reflection, "org.apache.kafka.clients.producer.RoundRobinPartitioner");

    // Confluent AbstractKafkaSchemaSerDeConfig — all Type.CLASS defaults loaded via Class.forName
    registerClassByName(reflection, "io.confluent.kafka.serializers.context.NullContextNameStrategy");
    registerClassByName(reflection, "io.confluent.kafka.serializers.context.strategy.ContextNameStrategy");
    registerClassByName(reflection, "io.confluent.kafka.serializers.subject.TopicNameStrategy");
    registerClassByName(reflection, "io.confluent.kafka.serializers.subject.TopicRecordNameStrategy");
    registerClassByName(reflection, "io.confluent.kafka.serializers.subject.RecordNameStrategy");
    registerClassByName(reflection, "io.confluent.kafka.serializers.subject.strategy.SubjectNameStrategy");
    registerClassByName(reflection, "io.confluent.kafka.serializers.schema.id.PrefixSchemaIdSerializer");
    registerClassByName(reflection, "io.confluent.kafka.serializers.schema.id.DualSchemaIdDeserializer");
    registerClassByName(reflection, "io.confluent.kafka.serializers.schema.id.SchemaIdSerializer");
    registerClassByName(reflection, "io.confluent.kafka.serializers.schema.id.SchemaIdDeserializer");

    // Confluent schema registry client and Avro internals
    registerClassByName(reflection, "io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient");
    registerClassByName(reflection, "io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider");
    registerClassByName(reflection, "io.confluent.kafka.schemaregistry.avro.AvroSchema");
    registerClassByName(reflection, "io.confluent.kafka.schemaregistry.avro.AvroSchemaUtils");

    // Confluent REST client entities — Jackson needs reflection for @JsonProperty serialization
    registerClassByNameFull(reflection, "io.confluent.kafka.schemaregistry.client.rest.entities.requests.RegisterSchemaRequest");
    registerClassByNameFull(reflection, "io.confluent.kafka.schemaregistry.client.rest.entities.requests.RegisterSchemaResponse");
    registerClassByNameFull(reflection, "io.confluent.kafka.schemaregistry.client.rest.entities.requests.ConfigUpdateRequest");
    registerClassByNameFull(reflection, "io.confluent.kafka.schemaregistry.client.rest.entities.requests.CompatibilityCheckResponse");
    registerClassByNameFull(reflection, "io.confluent.kafka.schemaregistry.client.rest.entities.requests.ModeUpdateRequest");
    registerClassByNameFull(reflection, "io.confluent.kafka.schemaregistry.client.rest.entities.Schema");
    registerClassByNameFull(reflection, "io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference");
    registerClassByNameFull(reflection, "io.confluent.kafka.schemaregistry.client.rest.entities.SchemaString");
    registerClassByNameFull(reflection, "io.confluent.kafka.schemaregistry.client.rest.entities.Config");
    registerClassByNameFull(reflection, "io.confluent.kafka.schemaregistry.client.rest.entities.ErrorMessage");
    registerClassByNameFull(reflection, "io.confluent.kafka.schemaregistry.client.rest.entities.Metadata");
    registerClassByNameFull(reflection, "io.confluent.kafka.schemaregistry.client.rest.entities.RuleSet");
    registerClassByNameFull(reflection, "io.confluent.kafka.schemaregistry.client.rest.entities.Rule");
    registerClassByNameFull(reflection, "io.confluent.kafka.schemaregistry.client.rest.entities.RuleKind");
    registerClassByNameFull(reflection, "io.confluent.kafka.schemaregistry.client.rest.entities.RuleMode");
    registerClassByNameFull(reflection, "io.confluent.kafka.schemaregistry.client.rest.entities.SchemaTags");
    registerClassByNameFull(reflection, "io.confluent.kafka.schemaregistry.client.rest.entities.ServerClusterId");
    registerClassByNameFull(reflection, "io.confluent.kafka.schemaregistry.client.rest.entities.SubjectVersion");

    // Confluent ParsedSchema implementations
    registerClassByNameFull(reflection, "io.confluent.kafka.schemaregistry.ParsedSchema");
    registerClassByNameFull(reflection, "io.confluent.kafka.schemaregistry.avro.AvroSchema");

    // Kafka consumer partition assignors (loaded by class name from config)
    registerClassByName(reflection, "org.apache.kafka.clients.consumer.RangeAssignor");
    registerClassByName(reflection, "org.apache.kafka.clients.consumer.CooperativeStickyAssignor");

    // Kafka security defaults
    registerClassByName(reflection, "org.apache.kafka.common.security.authenticator.DefaultLogin");
    registerClassByName(reflection, "org.apache.kafka.common.security.ssl.DefaultSslEngineFactory");
  }

  private void registerClassByName(org.springframework.aot.hint.ReflectionHints reflection, String className) {
    try {
      var clazz = Class.forName(className);
      reflection.registerType(clazz,
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
        MemberCategory.INVOKE_DECLARED_METHODS,
        MemberCategory.INVOKE_PUBLIC_METHODS);
    }
    catch (ClassNotFoundException ignored) {
      // Class not on classpath — skip hint
    }
  }

  private void registerClassByNameFull(org.springframework.aot.hint.ReflectionHints reflection, String className) {
    try {
      var clazz = Class.forName(className);
      reflection.registerType(clazz,
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
        MemberCategory.INVOKE_DECLARED_METHODS,
        MemberCategory.INVOKE_PUBLIC_METHODS,
        MemberCategory.ACCESS_DECLARED_FIELDS);
    }
    catch (ClassNotFoundException ignored) {
      // Class not on classpath — skip hint
    }
  }

  private void registerResourceHints(RuntimeHints hints) {
    hints.resources().registerPattern("META-INF/spring.factories");
    hints.resources().registerPattern("META-INF/spring-configuration-metadata.json");

    // Scan classpath for .p12 certificates (internal mode) and register each one
    try {
      var resolver = new PathMatchingResourcePatternResolver();
      var resources = resolver.getResources("classpath*:*.p12");
      for (var resource : resources) {
        var filename = resource.getFilename();
        if (filename != null) {
          hints.resources().registerPattern(filename);
        }
      }
    }
    catch (Exception ignored) {
      // No .p12 found on classpath — skip
    }
  }

  private void registerProxyHints(RuntimeHints hints) {
    // AOP proxy for the Aspect class
    hints.proxies().registerJdkProxy(
      org.springframework.aop.SpringProxy.class,
      org.springframework.aop.framework.Advised.class,
      org.springframework.core.DecoratingProxy.class
    );
  }
}
