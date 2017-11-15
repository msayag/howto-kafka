package io.github.msayag.kafka.avro;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.Closeable;
import java.util.List;
import java.util.Properties;

import static io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static io.confluent.kafka.serializers.KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;

public class SpecificItemConsumer implements Closeable {
    private final Consumer<String, Item> consumer;

    public SpecificItemConsumer() {
        consumer = createConsumer();
    }

    public void consume() {
        consumer.subscribe(List.of("items"));
        while (true) {
            try {
                ConsumerRecords<String, Item> records = consumer.poll(1000);
                records.forEach(this::processRecord);
            } catch (Exception e) {
                // handle error
            }
        }
    }

    private void processRecord(ConsumerRecord<String, Item> record) {
        Item item = record.value();
        processItem(item);
    }

    private void processItem(Item item) {
        System.out.println(item);
    }

    private static Consumer<String, Item> createConsumer() {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(GROUP_ID_CONFIG, "example");
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put(SPECIFIC_AVRO_READER_CONFIG, "true");
        props.put(SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        return new KafkaConsumer<>(props);
    }

    @Override
    public void close() {
        consumer.close();
    }

    public static void main(String[] args) {
        new SpecificItemConsumer().consume();
    }
}