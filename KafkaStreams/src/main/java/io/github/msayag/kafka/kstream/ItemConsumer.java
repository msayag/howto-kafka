package io.github.msayag.kafka.kstream;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.github.msayag.kafka.avro.Item;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Properties;

import static io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.*;

public class ItemConsumer {
    private static Properties getProperties() {
        Properties props = new Properties();
        props.put(APPLICATION_ID_CONFIG, "example-application");
        props.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        props.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        return props;
    }

    public void consume() {
        StreamsBuilder builder = new StreamsBuilder();
        builder.<String, Item>stream("items")
                .mapValues(item -> processItem(item))
                .to("items2", Produced.with(Serdes.String(), Serdes.String()));

        Properties config = getProperties();
        KafkaStreams streams = new KafkaStreams(builder.build(), config);
        streams.start();
    }

    private String processItem(Item item) {
        return item.getName() + ": " + item.getPrice();
    }

    public static void main(String... args) {
        new ItemConsumer().consume();
    }
}