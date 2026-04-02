# Kafka Library - Client Setup Guide

Guide for implementing the `io.github.santannaf:kafka` library in Spring Boot 4.x client projects (Java/Kotlin). Covers dependency setup, configuration, producer/consumer examples, SSL, batch, secondary connections, and local Docker environment.

**Spring Boot 4.x / Java 21+ required.**

---

## 1. Dependencies

### Gradle (Groovy - Java)
```groovy
implementation 'io.github.santannaf:kafka:1.0.0'
implementation 'org.apache.avro:avro:1.12.1'
```

### Gradle (Kotlin DSL)
```kotlin
implementation("io.github.santannaf:kafka:1.0.0")
implementation("org.apache.avro:avro:1.12.1")
```

### Maven
```xml
<dependency>
    <groupId>io.github.santannaf</groupId>
    <artifactId>kafka</artifactId>
    <version>1.0.0</version>
</dependency>

<dependency>
    <groupId>org.apache.avro</groupId>
    <artifactId>avro</artifactId>
    <version>1.12.1</version>
</dependency>
```

---

## 2. Activation

Annotate a configuration class (or the main application class) with `@EnabledArchKafka`:

### Java
```java
import io.github.santannaf.kafka.annotation.EnabledArchKafka;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnabledArchKafka(appName = "my-service")
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### Kotlin
```kotlin
import io.github.santannaf.kafka.annotation.EnabledArchKafka
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@EnabledArchKafka(appName = "my-service")
class MyApplication

fun main(args: Array<String>) {
    runApplication<MyApplication>(*args)
}
```

---

## 3. Configuration

All properties are prefixed with `kafka.arch`.

### Basic Example (PLAINTEXT)

```properties
kafka.arch.common.bootstrap-servers=localhost:29092
kafka.arch.common.schema-registry=http://localhost:8081

kafka.arch.producer.ack-producer-config=all
kafka.arch.producer.compress-type=snappy

kafka.arch.consumer.consumer-group-id=my-consumer-group
kafka.arch.consumer.ack-consumer-config=manual
kafka.arch.consumer.event-auto-offset-reset-config=latest
```

### SSL Example

```properties
kafka.arch.common.bootstrap-servers=broker:9093
kafka.arch.common.schema-registry=https://schema-registry:8081
kafka.arch.common.enable-connection-ssl-protocol-mode=true
kafka.arch.common.ssl-trust-store-location=classpath:ssl/kafka.truststore.p12
kafka.arch.common.ssl-trust-store-password=changeit
kafka.arch.common.ssl-key-store-location=classpath:ssl/kafka.client-keystore.p12
kafka.arch.common.ssl-key-store-password=changeit

kafka.arch.producer.ack-producer-config=all
kafka.arch.consumer.consumer-group-id=my-consumer-group
kafka.arch.consumer.ack-consumer-config=manual
```

### Secondary Connection

```properties
kafka.arch.common.bootstrap-servers=primary-broker:9092
kafka.arch.common.schema-registry=http://primary-registry:8081
kafka.arch.common.enable-another-connection=true
kafka.arch.common.another-bootstrap-servers=secondary-broker:9092
kafka.arch.common.another-schema-registry=http://secondary-registry:8081
```

---

## 4. Producer Example

```java
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PostsKafkaProducer {

    private static final Logger LOG = LoggerFactory.getLogger(PostsKafkaProducer.class);

    private static final Schema POST_SCHEMA = SchemaBuilder.record("Post")
            .namespace("com.example.entity")
            .fields()
            .requiredLong("id")
            .requiredString("title")
            .requiredString("userId")
            .requiredString("body")
            .endRecord();

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public PostsKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate,
                              @Value("${kafka.topic.posts}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void sendEvent(Post post) {
        GenericRecord event = toRecord(post);

        kafkaTemplate.send(topic, event).handle((result, error) -> {
            if (error != null) {
                LOG.error("Error publishing event: {}", error.getMessage());
            }
            else {
                LOG.info("Event sent successfully");
            }
            return null;
        });
    }

    private GenericRecord toRecord(Post post) {
        GenericRecord record = new GenericData.Record(POST_SCHEMA);
        record.put("id", post.id());
        record.put("title", post.title());
        record.put("userId", post.userId());
        record.put("body", post.body());
        return record;
    }
}
```

---

## 5. Consumer Example

```java
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class PostEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(PostEventConsumer.class);

    @KafkaListener(topics = "${kafka.topic.posts}", groupId = "${kafka.arch.consumer.consumer-group-id}")
    public void onMessage(ConsumerRecord<String, GenericRecord> event, Acknowledgment ack) {
        try {
            var record = event.value();
            LOG.info("Event received: id={}, title={}", record.get("id"), record.get("title"));
            ack.acknowledge();
        }
        catch (Exception e) {
            LOG.error("Error processing event: {}", e.getMessage());
        }
    }
}
```

---

## 6. Batch Consumer Example

```properties
kafka.arch.consumer.enable-batch-listener=true
kafka.arch.consumer.ack-consumer-config=manual
kafka.arch.consumer.max-poll-records=100
```

```java
@Component
public class PostBatchConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(PostBatchConsumer.class);

    @KafkaListener(topics = "${kafka.topic.posts}", groupId = "${kafka.arch.consumer.consumer-group-id}")
    public void onMessage(List<ConsumerRecord<String, GenericRecord>> events, Acknowledgment ack) {
        try {
            LOG.info("Received batch of {} events", events.size());
            for (var event : events) {
                LOG.info("Processing: id={}", event.value().get("id"));
            }
            ack.acknowledge();
        }
        catch (Exception e) {
            LOG.error("Error processing batch: {}", e.getMessage());
        }
    }
}
```

---

## 7. Local Docker Environment

Create a `docker-compose.kafka.yaml` in the project root. See the [library repository](https://github.com/santannaf/spring-kafka) for a complete example with Kafka, Zookeeper, Schema Registry, and Control Center.

```bash
docker compose -f docker-compose.kafka.yaml up -d
```

| Service         | URL                     | Description                    |
|-----------------|-------------------------|--------------------------------|
| Kafka (host)    | `localhost:29092`       | PLAINTEXT listener for the app |
| Kafka SSL       | `localhost:9093`        | SSL listener with certificate  |
| Schema Registry | `http://localhost:8081` | Avro schema management         |
| Control Center  | `http://localhost:9021` | Kafka monitoring UI            |

---

## 8. Beans Provided by the Library

| Bean                            | Type                                      | Notes                           |
|---------------------------------|-------------------------------------------|---------------------------------|
| `kafkaComponentsFactory`        | `KafkaComponentsFactory`                  | Central factory                 |
| `producerFactory`               | `ProducerFactory<String, Object>`         | Avro + SSL configured           |
| `kafkaAdmin`                    | `KafkaAdmin`                              | Topic management                |
| `kafkaTemplate`                 | `KafkaTemplate<String, Object>`           | With Micrometer + Observation   |
| `kafkaListenerContainerFactory` | `ConcurrentKafkaListenerContainerFactory` | ACK mode, batch, error handling |

With `enable-another-connection=true`: `anotherProducerFactory`, `anotherKafkaAdmin`, `anotherKafkaTemplate`, `anotherKafkaListenerContainerFactory`.
