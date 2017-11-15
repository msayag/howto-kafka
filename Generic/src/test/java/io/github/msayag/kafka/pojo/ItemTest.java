package io.github.msayag.kafka.pojo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ItemTest {

    @Test
    void getName() {
        Item i = new Item("dd", "fff", 123, 44.44);
        Assertions.assertEquals("ddd", i.getName());
    }
}