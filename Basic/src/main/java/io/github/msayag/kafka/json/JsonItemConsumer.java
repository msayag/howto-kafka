package io.github.msayag.kafka.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.msayag.kafka.api.ItemConsumer;
import io.github.msayag.kafka.pojo.Item;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;

public class JsonItemConsumer implements ItemConsumer {
    private final Consumer<String, String> consumer;
    private final ObjectMapper mapper;

    public JsonItemConsumer() {
        consumer = createConsumer();
        mapper = new ObjectMapper();
    }

    @Override
    public void consume() {
        consumer.subscribe(List.of("items"));
        while (true) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(1000);
                records.forEach(this::processRecord);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void processRecord(ConsumerRecord<String, String> record) {
        try {
            Item item = mapper.readValue(record.value(), Item.class);
            processItem(item);
        } catch (IOException e) {
            // handle error
        }
    }

    private void processItem(Item item) {
        System.out.println(item);
    }

    private Consumer<String, String> createConsumer() {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(GROUP_ID_CONFIG, "example");
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        return new KafkaConsumer<>(props);
    }

    @Override
    public void close() {
        consumer.close();
    }
}