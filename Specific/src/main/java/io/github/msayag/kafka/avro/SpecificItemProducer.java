package io.github.msayag.kafka.avro;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.Closeable;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.*;

public class SpecificItemProducer implements Closeable {
    private final Producer<String, Item> producer;

    public SpecificItemProducer() {
        producer = createProducer();
    }

    public void produce() {
        IntStream.iterate(1, i -> i + 1)
                .mapToObj(SpecificItemProducer::createItem)
                .forEach(item -> {
                    try {
                        send(item);
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // handle exception
                        Thread.currentThread().interrupt();
                    }
                });
    }

    private void send(Item item) {
        ProducerRecord<String, Item> record = new ProducerRecord<>("items", item);
        try {
            producer.send(record);
        } catch (SerializationException e) {
            // handle error
        }
    }

    private static Producer<String, Item> createProducer() {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        props.put(COMPRESSION_TYPE_CONFIG, "snappy");
        return new KafkaProducer<>(props);
    }

    public void close() {
        producer.close();
    }

    private static Item createItem(int i) {
        return Item.newBuilder()
                .setName("Item" + i)
                .setDescription("The description of item" + i)
                .setSku(i)
                .setPrice(ThreadLocalRandom.current().nextDouble(100))
                .build();
    }
}