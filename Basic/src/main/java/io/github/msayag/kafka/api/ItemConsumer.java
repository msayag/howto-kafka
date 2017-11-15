package io.github.msayag.kafka.api;

import java.io.Closeable;

public interface ItemConsumer extends Closeable {
    void consume();
}
