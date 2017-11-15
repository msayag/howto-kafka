package io.github.msayag.kafka;

import io.github.msayag.kafka.api.ItemConsumer;
import io.github.msayag.kafka.api.ItemProducer;
import io.github.msayag.kafka.avro.GenericItemConsumer;
import io.github.msayag.kafka.avro.GenericItemProducer;

import java.io.IOException;

public class Main {
    public static void main(String... args) {
        if (args.length == 0) {
            printUsage();
        } else {
            switch (args[0]) {
                case "producer":
                    try (ItemProducer producer = new GenericItemProducer()) {
                        producer.produce();
                    } catch (IOException e) {
                        // handle exception
                    }
                    break;
                case "consumer":
                    try (ItemConsumer producer = new GenericItemConsumer()) {
                        producer.consume();
                    } catch (IOException e) {
                        // handle exception
                    }
                    break;
                default:
                    System.out.println("Unsupported mode: " + args[0]);
                    printUsage();
            }
        }

    }

    private static void printUsage() {
        System.out.println("Please provide the application mode (producer or consumer)");
        System.out.println("    gradle run -PappArgs=producer");
        System.out.println("  or");
        System.out.println("    gradle run -PappArgs=consumer");
    }
}
