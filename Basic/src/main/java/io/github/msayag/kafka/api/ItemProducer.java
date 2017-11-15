package io.github.msayag.kafka.api;

import io.github.msayag.kafka.pojo.Item;

import java.io.Closeable;
import java.io.IOException;

public interface ItemProducer extends Closeable {
    void send(Item item) throws IOException;
}
