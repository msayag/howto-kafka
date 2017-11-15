package io.github.msayag.kafka.json;

import io.github.msayag.kafka.api.ItemProducer;
import io.github.msayag.kafka.pojo.Item;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static org.apache.kafka.clients.producer.ProducerConfig.*;

public class JsonItemProducer implements ItemProducer {
    private final Producer<String, String> producer;
    private final ObjectMapper mapper;

    public JsonItemProducer() {
        producer = createProducer();
        mapper = new ObjectMapper();
    }

    @Override
    public void produce() {
        IntStream.iterate(1, i -> i + 1)
                .mapToObj(JsonItemProducer::createItem)
                .forEach(item -> {
                    try {
                        send(item);
                        Thread.sleep(1000);
                    } catch (IOException e) {
                        // handle exception
                    } catch (InterruptedException e) {
                        // handle exception
                        Thread.currentThread().interrupt();
                    }
                });
    }

    private void send(Item item) throws IOException {
        String message = mapper.writeValueAsString(item);
        ProducerRecord<String, String> record = new ProducerRecord<>("items", message);
        producer.send(record);
    }

    private Producer<String, String> createProducer() {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    @Override
    public void close() {
        producer.close();
    }

    private static Item createItem(int i) {
        return new Item("Item" + i, "The description of item" + i, i, ThreadLocalRandom.current().nextDouble(100));
    }
}