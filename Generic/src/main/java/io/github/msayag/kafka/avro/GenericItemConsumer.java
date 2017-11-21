package io.github.msayag.kafka.avro;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.github.msayag.kafka.api.ItemConsumer;
import io.github.msayag.kafka.pojo.Item;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.util.Utf8;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Arrays;
import java.util.Properties;

import static io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;

public class GenericItemConsumer implements ItemConsumer {
    private final Consumer<String, Object> consumer;

    public GenericItemConsumer() {
        consumer = createConsumer();
    }

    @Override
    public void consume() {
        consumer.subscribe(Arrays.asList("items"));
        while (true) {
            try {
                ConsumerRecords<String, Object> records = consumer.poll(1000);
                records.forEach(this::processRecord);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void processRecord(ConsumerRecord<String, Object> record) {
        Record value = (Record) record.value();
        Item item = parseItem(value);
        processItem(item);
    }

    private Item parseItem(Record record) {
        return new Item(
                ((Utf8) record.get("name")).toString(),
                ((Utf8) record.get("description")).toString(),
                (Long) record.get("sku"),
                (Double) record.get("price"));
    }

    private void processItem(Item item) {
        System.out.println(item);
    }

    private static Consumer<String, Object> createConsumer() {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(GROUP_ID_CONFIG, "example");
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put(SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        return new KafkaConsumer<>(props);
    }

    @Override
    public void close() {
        consumer.close();
    }

    public static void main(String[] args) {
        new GenericItemConsumer().consume();
    }
}