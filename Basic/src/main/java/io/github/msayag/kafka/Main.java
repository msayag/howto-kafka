package io.github.msayag.kafka;

import io.github.msayag.kafka.api.ItemConsumer;
import io.github.msayag.kafka.api.ItemProducer;
import io.github.msayag.kafka.json.JsonItemConsumer;
import io.github.msayag.kafka.json.JsonItemProducer;

import java.io.IOException;

public class Main {
    public static void main(String... args) {
        if (args.length == 0) {
            printUsage();
        } else {
            switch (args[0]) {
                case "producer":
                    try (ItemProducer producer = new JsonItemProducer()) {
                        producer.produce();
                    } catch (IOException e) {
                        // handle exception
                    }
                    break;
                case "consumer":
                    try (ItemConsumer producer = new JsonItemConsumer()) {
                        producer.consume();
                    } catch (IOException e) {
                        // handle exception
                    }
            }
        }

    }

    private static void printUsage() {
        System.out.println("Please provide the ");
        System.out.println("    gradle run -PappArgs=producer");
        System.out.println("  or");
        System.out.println("    gradle run -PappArgs=consumer");
    }
}
