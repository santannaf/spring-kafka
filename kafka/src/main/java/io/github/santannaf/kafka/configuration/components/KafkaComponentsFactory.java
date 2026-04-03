package io.github.santannaf.kafka.configuration.components;

import io.github.santannaf.kafka.configuration.properties.KafkaArchCommonProperties;
import io.github.santannaf.kafka.configuration.properties.KafkaArchConsumerProperties;
import io.github.santannaf.kafka.configuration.properties.TunedPropertiesAckConsumerProperties;
import io.github.santannaf.kafka.configuration.properties.TunedKafkaProperties;
import io.github.santannaf.kafka.deserialize.SafeKafkaAvroDeserializer;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.RoundRobinPartitioner;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import io.github.santannaf.kafka.configuration.ssl.SslCertificateResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.LogIfLevelEnabled;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.util.HashMap;
import java.util.Map;

public class KafkaComponentsFactory {
  private static final Logger log = LoggerFactory.getLogger(KafkaComponentsFactory.class);
  private final TunedKafkaProperties properties;

  public KafkaComponentsFactory(TunedKafkaProperties properties) {
    this.properties = properties;
  }

  public ConcurrentKafkaListenerContainerFactory<String, Object> configureListenerContainerFactory(
    final ConsumerFactory<String, Object> consumerFactory,
    final KafkaArchConsumerProperties consumerProps,
    CommonErrorHandler errorHandler) {

    var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();

    ContainerProperties containerProperties = factory.getContainerProperties();
    containerProperties.setAckMode(TunedPropertiesAckConsumerProperties.fromValue(consumerProps.ackConsumerConfig()));
    containerProperties.setCommitLogLevel(LogIfLevelEnabled.Level.INFO);
    containerProperties.setLogContainerConfig(false);
    containerProperties.setMicrometerEnabled(true);
    containerProperties.setObservationEnabled(true);
    containerProperties.setAsyncAcks(consumerProps.enableAsyncAck());

    if (consumerProps.enableVirtualThreads()) {
      var executor = new SimpleAsyncTaskExecutor("kafka-vt-");
      executor.setVirtualThreads(true);
      containerProperties.setListenerTaskExecutor(executor);
      log.info("Kafka listener configured with virtual threads");
    }

    factory.setBatchListener(consumerProps.enableBatchListener());
    factory.setConsumerFactory(consumerFactory);

    if (errorHandler != null) {
      factory.setCommonErrorHandler(errorHandler);
    }

    return factory;
  }

  private Map<String, Object> loadingProducerProperties(String bootstrapServers, String schemaRegistry) {
    var common = properties.common();
    var producer = properties.producer();
    var props = applyCommonConnectionProperties(common);

    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

    if (producer.typePartitioner() != null) {
      if (producer.typePartitioner().equalsIgnoreCase("RoundRobinPartitioner")) {
        props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, RoundRobinPartitioner.class);
      }
    }

    props.put(ProducerConfig.RETRIES_CONFIG, producer.maxProducerRetry());
    props.put(ProducerConfig.BATCH_SIZE_CONFIG, producer.batchSize());
    props.put(ProducerConfig.LINGER_MS_CONFIG, producer.lingerMs());
    props.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, common.reconnectBackoff());
    props.put(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, common.reconnectBackoffMax());
    props.put(ProducerConfig.ACKS_CONFIG, producer.ackProducerConfig());
    props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, producer.compressType());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
    props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistry);

    if (common.clientId() != null) props.put(ConsumerConfig.CLIENT_ID_CONFIG, common.clientId());
    if (producer.transactionalId() != null) props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, producer.transactionalId());

    return props;
  }

  public ProducerFactory<String, Object> producerFactory() {
    return producerFactory(properties.common().bootstrapServers(), properties.common().schemaRegistry());
  }

  public ProducerFactory<String, Object> producerFactory(String bootstrapServers, String schemaRegistry) {
    log.info("ProducerFactory Bean created for bootstrap: {}", bootstrapServers);
    var props = loadingProducerProperties(bootstrapServers, schemaRegistry);
    return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new KafkaAvroSerializer());
  }

  public KafkaAdmin kafkaAdmin() {
    return kafkaAdmin(properties.common().bootstrapServers());
  }

  public KafkaAdmin kafkaAdmin(String bootstrapServers) {
    log.info("KafkaAdmin Bean created for bootstrap: {}", bootstrapServers);
    var props = new HashMap<String, Object>();
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    return new KafkaAdmin(props);
  }

  public KafkaTemplate<String, Object> kafkaTemplate(final ProducerFactory<String, Object> producerFactory, final KafkaAdmin kafkaAdmin) {
    log.info("KafkaTemplate Bean created");
    var kafkaTemplate = new KafkaTemplate<>(producerFactory);
    kafkaTemplate.setMicrometerEnabled(true);
    kafkaTemplate.setObservationEnabled(true);
    kafkaTemplate.setKafkaAdmin(kafkaAdmin);
    return kafkaTemplate;
  }

  public ConsumerFactory<String, Object> kafkaConsumerFactory() {
    return kafkaConsumerFactory(properties.common().bootstrapServers(), properties.common().schemaRegistry());
  }

  public ConsumerFactory<String, Object> kafkaConsumerFactory(String bootstrapServers, String schemaRegistry) {
    log.info("KafkaConsumerFactory Bean created for bootstrap: {}", bootstrapServers);
    return new DefaultKafkaConsumerFactory<>(consumerConfigs(bootstrapServers, schemaRegistry), new StringDeserializer(), new SafeKafkaAvroDeserializer());
  }

  private Map<String, Object> consumerConfigs(String bootstrapServers, String schemaRegistry) {
    var common = properties.common();
    var consumer = properties.consumer();
    var props = applyCommonConnectionProperties(common);

    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, consumer.fetchMinBytes());
    props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, consumer.fetchMaxWaitBytes());
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, SafeKafkaAvroDeserializer.class);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumer.eventAutoOffsetResetConfig());
    props.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, common.reconnectBackoff());
    props.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, common.reconnectBackoffMax());
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, consumer.enableAutoCommit());
    props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, consumer.maxPollRecords());
    props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, consumer.maxPollIntervalMs());
    props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, consumer.requestTimeoutConfigMs());
    props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, consumer.heartbeatIntervalMs());
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, consumer.sessionTimeoutMs());
    props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, consumer.enableAvroReaderConfig());
    props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistry);
    return props;
  }

  private Map<String, Object> applyCommonConnectionProperties(final KafkaArchCommonProperties common) {
    var props = new HashMap<String, Object>();
    validateSSLMode(common);
    if (common.enableConnectionSslProtocolMode()) enableSSLMode(props, common);
    return props;
  }

  private void enableSSLMode(Map<String, Object> props, final KafkaArchCommonProperties common) {
    props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

    String truststorePath;
    String keystorePath;

    if (common.certificateType() != null) {
      truststorePath = SslCertificateResolver.resolveFromType(common.certificateType(), common.environment(), "truststore");
      keystorePath = SslCertificateResolver.resolveFromType(common.certificateType(), common.environment(), "keystore");
    }
    else {
      truststorePath = SslCertificateResolver.resolveFromPath(common.sslTrustStoreLocation());
      keystorePath = SslCertificateResolver.resolveFromPath(common.sslKeyStoreLocation());
    }

    props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststorePath);
    props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystorePath);
    props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, common.sslTrustStorePassword());
    props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, common.sslKeyStorePassword());
    props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, common.sslKeyStorePassword());
    props.put(SslConfigs.SSL_KEYMANAGER_ALGORITHM_CONFIG, "PKIX");
    props.put(SslConfigs.SSL_TRUSTMANAGER_ALGORITHM_CONFIG, "PKIX");
    props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "pkcs12");
    props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "pkcs12");
    props.put(SslConfigs.SSL_ENABLED_PROTOCOLS_CONFIG, "TLSv1.2");
  }

  private void validateSSLMode(final KafkaArchCommonProperties common) {
    if (common.enableConnectionSslProtocolMode()) {
      boolean hasInternalCert = common.certificateType() != null;
      boolean hasExternalCert = common.sslTrustStoreLocation() != null && common.sslKeyStoreLocation() != null;

      if (!hasInternalCert && !hasExternalCert) {
        throw new IllegalArgumentException(
          "Conexao SSL habilitada: informe certificate-type (modo interno) ou ssl-trust-store-location e ssl-key-store-location (modo externo)");
      }

      if (common.sslTrustStorePassword() == null || common.sslKeyStorePassword() == null) {
        throw new IllegalArgumentException("Conexao SSL habilitada, verifique os passwords das chaves de permissionamento");
      }

      if (common.bootstrapServers().contains("9092")) {
        throw new IllegalArgumentException("Forneca uma conexao na porta 9093");
      }
    }
  }
}
