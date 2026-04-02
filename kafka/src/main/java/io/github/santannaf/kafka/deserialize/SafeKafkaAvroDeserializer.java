package io.github.santannaf.kafka.deserialize;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SafeKafkaAvroDeserializer extends KafkaAvroDeserializer {
    private static final Logger log = LoggerFactory.getLogger(SafeKafkaAvroDeserializer.class);

    @Override
    public Object deserialize(String topic, byte[] bytes) {
        try {
            return super.deserialize(topic, bytes);
        } catch (Exception error) {
            logDeserializationError(topic, error);
            return null;
        }
    }

    @Override
    public Object deserialize(String topic, byte[] bytes, Schema readerSchema) {
        try {
            return super.deserialize(topic, bytes, readerSchema);
        } catch (Exception error) {
            logDeserializationError(topic, error);
            return null;
        }
    }

    private void logDeserializationError(String topic, Exception error) {
        log.error("Erro ao deserializer mensagem do topico={}, causa={}", topic, error.getMessage(), error);
    }
}
