package io.github.msayag.kafka.avro;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.github.msayag.kafka.api.ItemProducer;
import io.github.msayag.kafka.pojo.Item;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.*;

public class GenericItemProducer implements ItemProducer {
    private final Producer<String, Object> producer;
    private final Schema schema;

    public GenericItemProducer() {
        producer = createProducer();
        schema = createSchema();
    }

    private GenericRecord createAvroRecord(Item item) {
        GenericRecord avroRecord = new GenericData.Record(schema);
        avroRecord.put("name", item.getName());
        avroRecord.put("description", item.getDescription());
        avroRecord.put("sku", item.getSku());
        avroRecord.put("price", item.getPrice());
        return avroRecord;
    }

    private static Producer<String, Object> createProducer() {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        return new KafkaProducer<>(props);
    }

    private Schema createSchema() {
        //language=JSON
        String userSchema = "{\"type\":\"record\",\n" +
                "  \"name\":\"item\",\n" +
                "  \"fields\":[\n" +
                "    {\"name\":\"name\",\"type\":\"string\"},\n" +
                "    {\"name\":\"description\",\"type\":\"string\"},\n" +
                "    {\"name\":\"sku\",\"type\":\"long\"},\n" +
                "    {\"name\":\"price\",\"type\":\"double\"}\n" +
                "  ]}";
        Schema.Parser parser = new Schema.Parser();
        return parser.parse(userSchema);
    }

    @Override
    public void produce() {
        IntStream.iterate(1, i -> i + 1)
                .mapToObj(GenericItemProducer::createItem)
                .forEach(item -> {
                    try {
                        send(item);
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // handle exception
                        Thread.currentThread().interrupt();
                    }
                });
    }

    private void send(Item item) {
        GenericRecord avroRecord = createAvroRecord(item);
        ProducerRecord<String, Object> record = new ProducerRecord<>("items", avroRecord);
        try {
            producer.send(record);
        } catch (SerializationException e) {
            // handle error
        }
    }

    @Override
    public void close() {
        producer.close();
    }

    private static Item createItem(int i) {
        return new Item("Item" + i, "The description of item" + i, i, ThreadLocalRandom.current().nextDouble(100));
    }
}