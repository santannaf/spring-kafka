# Tuned Kafka - Spring Boot Kafka Library

Opinionated Spring Boot auto-configuration for Apache Kafka producers and consumers with native support for **Avro**, **Schema Registry**, **SSL/TLS**, and **observability** (Micrometer + Observation API).

Eliminates Kafka configuration boilerplate in Spring Boot projects by delivering production-ready defaults with a single annotation.

---

## Table of Contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Architecture](#architecture)
- [Properties Reference](#properties-reference)
  - [Common Properties](#common-properties-kafkaarchcommon)
  - [Producer Properties](#producer-properties-kafkaarchproducer)
  - [Consumer Properties](#consumer-properties-kafkaarchconsumer)
- [SSL/TLS Connection](#ssltls-connection)
- [Secondary Connection](#secondary-connection-another-connection)
- [SafeKafkaAvroDeserializer](#safekafkaavrodeserializer)
- [Consumer ACK Modes](#consumer-ack-modes)
- [Registered Beans](#registered-beans)
- [Tests](#tests)
- [Useful Commands](#useful-commands)
- [Local Docker Environment](#local-docker-environment)
- [SSL Certificates - Production Model](#ssl-certificates--production-model)
- [Contributing](#contributing)
- [License](#license)

---

## Requirements

| Dependency         | Minimum Version | Notes                                             |
|--------------------|-----------------|---------------------------------------------------|
| JDK                | 21+             | Required for compilation and `keytool` (SSL gen)  |
| Spring Boot        | 4.0.x           |                                                   |
| Apache Kafka       | 3.x / 4.x      |                                                   |
| Confluent Platform | 8.x             |                                                   |
| Docker             | -               | For the local environment (docker-compose)        |

---

## Installation

### Maven
```xml
<dependency>
  <groupId>io.github.santannaf</groupId>
  <artifactId>kafka</artifactId>
  <version>1.0.0</version>
</dependency>
```

### Gradle (Groovy)
```groovy
implementation 'io.github.santannaf:kafka:1.0.0'
```

### Gradle (Kotlin DSL)
```kotlin
implementation("io.github.santannaf:kafka:1.0.0")
```

---

## Quick Start

### 1. Enable the library in your main class

```java
@SpringBootApplication
@EnabledArchKafka(appName = "my-service")
public class MyServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(MyServiceApplication.class, args);
  }
}
```

The `@EnabledArchKafka` annotation automatically imports all library auto-configuration. The `appName` parameter is optional and used for logging.

### 2. Configure properties in `application.properties`

```properties
kafka.arch.common.bootstrap-servers=broker:9092
kafka.arch.common.schema-registry=http://schema-registry:8081

kafka.arch.producer.ack-producer-config=all
kafka.arch.producer.compress-type=snappy

kafka.arch.consumer.consumer-group-id=my-group
kafka.arch.consumer.ack-consumer-config=manual
kafka.arch.consumer.event-auto-offset-reset-config=earliest
```

### 3. Produce messages

```java
@Service
public class MyProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public MyProducer(KafkaTemplate<String, Object> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void send(String topic, String key, Object message) {
    kafkaTemplate.send(topic, key, message);
  }
}
```

### 4. Consume messages

```java
@Service
public class MyConsumer {

  @KafkaListener(topics = "my-topic", groupId = "my-group")
  public void consume(Object message, Acknowledgment ack) {
    // process message
    ack.acknowledge();
  }
}
```

---

## Architecture

```
@EnabledArchKafka
       |
       v
KafkaAnalysisImportSelector
       |
       v
TunedKafkaAutoConfiguration
       |
       v
KafkaComponentsFactory  <--  TunedKafkaProperties (kafka.arch.*)
       |
       +---> ProducerFactory<String, Object>    (StringSerializer + KafkaAvroSerializer)
       +---> ConsumerFactory<String, Object>    (StringDeserializer + SafeKafkaAvroDeserializer)
       +---> KafkaAdmin
       +---> KafkaTemplate<String, Object>      (Micrometer + Observation enabled)
       +---> ConcurrentKafkaListenerContainerFactory<String, Object>
```

The library uses **conditional auto-configuration**: it is only activated when `@EnabledArchKafka` is present in the application classpath. It registers **before** Spring Boot's default `KafkaAutoConfiguration`, replacing the default beans.

---

## Properties Reference

All properties are configured under the `kafka.arch` prefix, divided into three groups.

### Common Properties (`kafka.arch.common`)

Shared between producer and consumer.

| Property | Type | Default | Description |
|---|---|---|---|
| `bootstrap-servers` | `String` | `localhost:9092` | Kafka broker address(es). Accepts comma-separated list for multi-node clusters. |
| `client-id` | `String` | `null` | Kafka client identifier. Used for tracking in broker logs and metrics. |
| `schema-registry` | `String` | `localhost:8081` | Confluent Schema Registry URL. Required for Avro serialization/deserialization. |
| `enable-connection-ssl-protocol-mode` | `boolean` | `false` | Enables SSL/TLS connection with the broker. When `true`, requires truststore and keystore configuration. |
| `ssl-trust-store-location` | `String` | `null` | Truststore file path (PKCS12 format). Required when SSL is enabled. |
| `ssl-trust-store-password` | `String` | `null` | Truststore password. Required when SSL is enabled. |
| `ssl-key-store-location` | `String` | `null` | Keystore file path (PKCS12 format). Required when SSL is enabled. |
| `ssl-key-store-password` | `String` | `null` | Keystore password. Required when SSL is enabled. |
| `reconnect-backoff` | `int` | `50` | Initial wait time (ms) before retrying a connection to the broker after failure. |
| `reconnect-backoff-max` | `int` | `2000` | Maximum wait time (ms) between reconnection attempts. Backoff grows exponentially up to this limit. |
| `events-concurrency` | `int` | `2` | Concurrency level for event processing. |
| `enable-another-connection` | `boolean` | `false` | Enables a secondary connection to a different Kafka cluster. See [Secondary Connection](#secondary-connection-another-connection). |
| `another-bootstrap-servers` | `String` | `null` | Secondary cluster broker address(es). Required when `enable-another-connection=true`. |
| `another-schema-registry` | `String` | `null` | Secondary cluster Schema Registry URL. |

---

### Producer Properties (`kafka.arch.producer`)

| Property | Type | Default | Description |
|---|---|---|---|
| `ack-producer-config` | `String` | `all` | Acknowledgment level required from the broker. `all` ensures all replicas confirm the write, providing the highest durability. Values: `0`, `1`, `all`. |
| `max-producer-retry` | `int` | `5` | Maximum number of retry attempts on transient failures. |
| `batch-size` | `int` | `20000` | Maximum batch size (bytes) before sending. Larger batches improve throughput but increase latency. |
| `linger-ms` | `int` | `10` | Time (ms) the producer waits before sending an incomplete batch. Allows grouping more messages per batch. |
| `enable-idempotence-config` | `boolean` | `false` | Configuration flag exposed in properties. **Note:** the library always enables idempotence internally (`enable.idempotence=true` and `max.in.flight.requests.per.connection=1`) for exactly-once semantics. |
| `compress-type` | `String` | `none` | Compression algorithm applied to messages. Values: `none`, `gzip`, `snappy`, `lz4`, `zstd`. |
| `type-partitioner` | `String` | `null` | Partitioning strategy. When set to `RoundRobinPartitioner`, distributes messages evenly across partitions. |
| `transactional-id` | `String` | `null` | Transactional ID for transactional production. When set, enables Kafka transactions (exactly-once). |
| `enable-reactive-project` | `boolean` | `false` | Flag for Reactor Kafka usage. |

---

### Consumer Properties (`kafka.arch.consumer`)

| Property | Type | Default | Description |
|---|---|---|---|
| `consumer-group-id` | `String` | `null` | Consumer group identifier. **Required** for consumers. All consumers with the same group ID share the partition load. |
| `ack-consumer-config` | `String` | `manual` | Consumer acknowledgment mode. Determines when offsets are committed. See [ACK Modes](#consumer-ack-modes). |
| `event-auto-offset-reset-config` | `String` | `latest` | Behavior when no committed offset exists. `latest` reads only new messages; `earliest` reads from the beginning. Values: `earliest`, `latest`, `none`. |
| `enable-auto-commit` | `boolean` | `false` | Enables automatic offset commits. **Recommended to keep `false`** for manual control, avoiding message loss. |
| `max-poll-records` | `int` | `500` | Maximum records returned per `poll()` call. Lower values reduce the risk of rebalancing due to timeout. |
| `max-poll-interval-ms` | `int` | `300000` | Maximum time (ms) between `poll()` calls. If exceeded, the consumer is removed from the group. |
| `fetch-min-bytes` | `int` | `100000` | Minimum bytes the broker accumulates before responding to a fetch. Higher values improve throughput; lower values reduce latency. |
| `fetch-max-wait-bytes` | `int` | `500` | Maximum time (ms) the broker waits to accumulate `fetch-min-bytes`. Acts as a fetch timeout. |
| `session-timeout-ms` | `int` | `20000` | Time (ms) the broker waits without heartbeat before considering the consumer dead and triggering rebalancing. |
| `heartbeat-interval-ms` | `int` | `3000` | Interval (ms) between heartbeats sent to the coordinator. Should be less than `session-timeout-ms` (recommended: 1/3 of session timeout). |
| `request-timeout-config-ms` | `int` | `30000` | Maximum time (ms) the client waits for a broker response. |
| `enable-avro-reader-config` | `boolean` | `true` | Enables Specific Avro Reader. When `true`, deserializes to generated Avro classes. When `false`, deserializes to `GenericRecord`. |
| `enable-batch-listener` | `boolean` | `false` | Enables batch consumption. When `true`, the listener receives `List<ConsumerRecord>` instead of individual records. |
| `enable-async-ack` | `boolean` | `false` | Enables asynchronous acknowledgment. Allows offsets to be committed out of order, improving performance in specific scenarios. |
| `max-attempts-consumer-record` | `int` | `3` | Maximum processing attempts for a record before forwarding to error handling. |
| `interval-retry-attempts-consumer-record` | `int` | `10000` | Interval (ms) between retry attempts for a failed record. |
| `enable-virtual-threads` | `boolean` | `false` | Enables virtual threads (Java 21+) for listener container threads. Reduces OS thread creation overhead and improves performance in I/O-bound scenarios. |

---

## SSL/TLS Connection

The library supports secure connections via SSL/TLS with automatic validation.

```properties
kafka.arch.common.bootstrap-servers=broker:9093
kafka.arch.common.enable-connection-ssl-protocol-mode=true
kafka.arch.common.ssl-trust-store-location=/certs/truststore.p12
kafka.arch.common.ssl-trust-store-password=changeit
kafka.arch.common.ssl-key-store-location=/certs/keystore.p12
kafka.arch.common.ssl-key-store-password=changeit
```

**Automatic validations when SSL is enabled:**

| Validation | Error |
|---|---|
| Truststore and Keystore locations must be provided | `SSL connection enabled, check key permission locations` |
| Truststore and Keystore passwords must be provided | `SSL connection enabled, check key permission passwords` |
| Bootstrap port **must not** be `9092` | `Provide a connection on port 9093` |

**SSL settings applied automatically:**

- Protocol: `SSL`
- Keystore/truststore type: `PKCS12`
- Key/trust manager algorithm: `PKIX`
- TLS protocol: `TLSv1.2`

---

## Secondary Connection (Another Connection)

For scenarios where the application needs to connect to **two distinct Kafka clusters**:

```properties
kafka.arch.common.bootstrap-servers=primary-cluster:9092
kafka.arch.common.schema-registry=http://primary-sr:8081
kafka.arch.common.enable-another-connection=true
kafka.arch.common.another-bootstrap-servers=secondary-cluster:9092
kafka.arch.common.another-schema-registry=http://secondary-sr:8081
```

This registers a second set of beans:

| Primary Bean | Secondary Bean |
|---|---|
| `producerFactory` | `anotherProducerFactory` |
| `kafkaAdmin` | `anotherKafkaAdmin` |
| `kafkaTemplate` | `anotherKafkaTemplate` |
| `kafkaListenerContainerFactory` | `anotherKafkaListenerContainerFactory` |

To consume from the secondary cluster, reference the container factory in the listener:

```java
@KafkaListener(
  topics = "secondary-topic",
  containerFactory = "anotherKafkaListenerContainerFactory"
)
public void consumeFromSecondary(Object message, Acknowledgment ack) {
  // ...
  ack.acknowledge();
}
```

To produce to the secondary cluster, inject the qualified template:

```java
@Autowired
@Qualifier("anotherKafkaTemplate")
private KafkaTemplate<String, Object> anotherTemplate;
```

---

## SafeKafkaAvroDeserializer

The library includes a safe Avro deserializer (`SafeKafkaAvroDeserializer`) that extends Confluent's `KafkaAvroDeserializer`.

**Behavior:** on deserialization errors, the error is logged and `null` is returned instead of propagating the exception. This prevents a corrupted message from stopping consumption of the entire topic.

```
ERROR - Error deserializing message from topic={topicName}, cause={errorMessage}
```

This deserializer is automatically used in all `ConsumerFactory` instances created by the library.

---

## Consumer ACK Modes

The `ack-consumer-config` property accepts the following values:

| Value | AckMode | Description |
|---|---|---|
| `record` | `RECORD` | Commit after processing **each record** individually. |
| `batch` | `BATCH` | Commit after processing **all records** returned by `poll()`. |
| `time` | `TIME` | Commit at defined time intervals. |
| `count` | `COUNT` | Commit after processing a defined number of records. |
| `count_time` | `COUNT_TIME` | Commit when **either** condition (count or time) is met first. |
| `manual` | `MANUAL` | **Default.** Offset is committed manually via `Acknowledgment.acknowledge()`. Actual commit happens after the next `poll()`. |
| `manual_immediate` | `MANUAL_IMMEDIATE` | Offset is committed **immediately** when `acknowledge()` is called. |

> **Recommendation:** use `manual` or `manual_immediate` for full control over when offsets are committed, avoiding reprocessing or message loss.

---

## Registered Beans

When `@EnabledArchKafka` is activated, the following beans are automatically registered:

| Bean | Type | Annotation | Description |
|---|---|---|---|
| `kafkaComponentsFactory` | `KafkaComponentsFactory` | - | Central factory for creating Kafka components. |
| `producerFactory` | `ProducerFactory<String, Object>` | `@Primary` | Producer factory with StringSerializer (key) and KafkaAvroSerializer (value). |
| `kafkaAdmin` | `KafkaAdmin` | - | Kafka admin for automatic topic creation. |
| `kafkaTemplate` | `KafkaTemplate<String, Object>` | - | Template for sending messages with Micrometer and Observation enabled. |
| `kafkaListenerContainerFactory` | `ConcurrentKafkaListenerContainerFactory` | `@Primary` | Listener container factory with ACK mode, batch, and error handler configured. |

---

## Tests

The library includes unit and integration tests.

**Run unit tests:**

```bash
./mvnw test
```

**Run integration tests** (requires Docker for Testcontainers):

```bash
./mvnw test -pl kafka -Pintegration
```

Integration tests cover production and consumption in **text**, **JSON**, and **Avro** formats, as well as SSL connection validation, topic creation via KafkaAdmin, and secondary connection scenarios.

---

## Useful Commands

```bash
# Build the project
./mvnw clean install -DskipTests

# Change the version
./mvnw versions:set -DnewVersion=0.0.3 -DprocessAllModules=true

# Run all tests (unit + integration)
./mvnw test -pl kafka -Pintegration

# Run unit tests only
./mvnw test
```

---

## Local Docker Environment

The repository includes a `docker-compose.kafka.yaml` with Kafka, Zookeeper, Schema Registry, and Control Center.

```bash
docker compose -f docker-compose.kafka.yaml up -d
```

| Service         | URL                     | Description                     |
|-----------------|-------------------------|---------------------------------|
| Kafka (host)    | `localhost:29092`       | PLAINTEXT listener for the app  |
| Kafka SSL       | `localhost:9093`        | SSL listener with certificate   |
| Schema Registry | `http://localhost:8081` | Avro schema management          |
| Control Center  | `http://localhost:9021` | Kafka monitoring UI             |

To stop:

```bash
docker compose -f docker-compose.kafka.yaml down
```

---

## SSL Certificates - Production Model

The `generate-certs.sh` script in the project root generates certificates following the production model with a **separate CA**. Generated files are output to the `certs/` directory. The script uses `keytool`, which is part of the JDK - make sure **JDK 21+** is installed and on PATH.

### Generating certificates

```bash
# With default values
./generate-certs.sh

# Customizing for your organization
./generate-certs.sh --org MyCompany --location SaoPaulo --state SP --country BR

# Customizing password and validity
./generate-certs.sh --org MyCompany --password my-password --validity 730
```

Available options:

| Option       | Default    | Description                   |
|--------------|------------|-------------------------------|
| `--org`      | `MyOrg`    | Organization name             |
| `--location` | `City`     | City                          |
| `--state`    | `State`    | State                         |
| `--country`  | `BR`       | Country code (2 letters)      |
| `--password` | `changeit` | Keystore password             |
| `--validity` | `365`      | Certificate validity in days  |

### Generated files

| File                       | Content                                      | Used by        |
|----------------------------|----------------------------------------------|----------------|
| `ca-root.p12`              | CA keystore (private key + certificate)      | Admin only     |
| `ca-root.crt`              | CA certificate in PEM format                 | Reference      |
| `kafka.keystore.p12`       | Broker certificate signed by the CA          | Broker (Kafka) |
| `kafka.client-keystore.p12`| Client certificate signed by the CA          | Client (App)   |
| `kafka.truststore.p12`     | CA certificate (to validate the broker)      | Client (App)   |
| `ssl_credentials`          | Keystore password (used by Confluent Docker) | Broker (Kafka) |

### Trust flow

```
CA (ca-root)
  |
  +-- signs --> broker certificate   (kafka.keystore.p12)
  |
  +-- signs --> client certificate   (kafka.client-keystore.p12)
  |
  +-- trusts <-- client truststore   (kafka.truststore.p12 contains ca-root.crt)
```

1. The **broker** presents its certificate (signed by the CA) on SSL connections.
2. The **client** validates the broker certificate using the truststore containing the CA.
3. The **client** presents its own certificate (signed by the same CA) - required for mTLS.

### Using certificates in the client project

After generating certificates, copy the truststore and client keystore to the app project:

```bash
# Broker files (for client's docker-compose)
cp certs/kafka.keystore.p12 certs/kafka.truststore.p12 certs/ssl_credentials <client-project>/certs/

# App files (classpath)
cp certs/kafka.truststore.p12 certs/kafka.client-keystore.p12 <client-project>/src/main/resources/ssl/
```

And configure the SSL profile:

```properties
kafka.arch.common.bootstrap-servers=localhost:9093
kafka.arch.common.enable-connection-ssl-protocol-mode=true
kafka.arch.common.ssl-trust-store-location=classpath:ssl/kafka.truststore.p12
kafka.arch.common.ssl-trust-store-password=changeit
kafka.arch.common.ssl-key-store-location=classpath:ssl/kafka.client-keystore.p12
kafka.arch.common.ssl-key-store-password=changeit
```

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on how to contribute to this project.

---

## License

This project is licensed under the [Apache License 2.0](LICENSE).
