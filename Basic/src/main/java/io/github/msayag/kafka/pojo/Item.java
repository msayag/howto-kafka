package io.github.msayag.kafka.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Item {
    private final String name;
    private final String description;
    private final long sku;
    private final double price;

    @JsonCreator
    public Item(@JsonProperty("name") String name, @JsonProperty("description") String description, @JsonProperty("sku") long sku, @JsonProperty("price") double price) {
        this.name = name;
        this.description = description;
        this.sku = sku;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getSku() {
        return sku;
    }

    public double getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return "Item{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", sku=" + sku +
                ", price=" + price +
                '}';
    }
}
