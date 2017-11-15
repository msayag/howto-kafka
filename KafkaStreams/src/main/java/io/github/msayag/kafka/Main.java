package io.github.msayag.kafka;

import io.github.msayag.kafka.kstream.ItemConsumer;
import io.github.msayag.kafka.kstream.ItemProducer;

public class Main {
    public static void main(String... args) {
        if (args.length == 0) {
            printUsage();
        } else {
            switch (args[0]) {
                case "producer":
                    try (ItemProducer producer = new ItemProducer()) {
                        producer.produce();
                    }
                    break;
                case "consumer":
                    ItemConsumer producer = new ItemConsumer();
                    producer.consume();
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
