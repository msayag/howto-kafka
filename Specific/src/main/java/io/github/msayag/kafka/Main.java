package io.github.msayag.kafka;

import io.github.msayag.kafka.avro.SpecificItemConsumer;
import io.github.msayag.kafka.avro.SpecificItemProducer;

public class Main {
    public static void main(String... args) {
        if (args.length == 0) {
            printUsage();
        } else {
            switch (args[0]) {
                case "producer":
                    try (SpecificItemProducer producer = new SpecificItemProducer()) {
                        producer.produce();
                    }
                    break;
                case "consumer":
                    SpecificItemConsumer producer = new SpecificItemConsumer();
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
