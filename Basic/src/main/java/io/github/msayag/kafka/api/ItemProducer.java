package io.github.msayag.kafka.api;

import java.io.Closeable;

public interface ItemProducer extends Closeable {
    void produce();
}
