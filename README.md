# (HowTo) Kafka, Streams and Avro serialization

* [Requirements](#requirements)
* [The Kafka Broker](#the-kafka-broker)
* [Sending an Object as JSON](#sending-an-object-as-json)
* [Sending an Object with Avro Serialization](#sending-an-object-with-avro-serialization)
   - [The Schema Registry](#the-schema-registry)
   - [The Generic Way](#the-generic-way)
   - [The Specific Way](#the-specific-way)
* [Kafka Streams](#kafka-streams)
* [Execution instructions](#execution-instructions)

## Requirements
1. Java 8 or higher
1. Docker and docker-compose  
Instructions can be found in [this quickstart](https://docs.confluent.io/3.3.1/installation/docker/docs/quickstart.html) from Confluent. 
1. [gradle](https://gradle.org/)


## The Kafka broker
[Kafka](https://kafka.apache.org/intro) is a distributed streaming platform and the Kafka broker is the channel through which the messages are passed.  
The easiest way to start a single Kafka broker locally is probably to run the pre-packaged Docker images with this [docker-compose.yml](Basic/docker-compose.yml) file.  

```yaml
version: '3'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:3.3.1
    network_mode: host
    environment:
      ZOOKEEPER_CLIENT_PORT: 32181
      ZOOKEEPER_TICK_TIME: 2000

  kafka:
    image: confluentinc/cp-kafka:3.3.1
    network_mode: host
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: localhost:32181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

## Sending an object as JSON
### The producer
The producer creates the objects, convert (serialize) them to JSON and publish them by sending and enqueuing to Kafka.

The basic properties of the producer are the address of the broker and the serializer of the key and values.
The serializer of the key is set to the StringSerializer and should be set according to its type.
The value is sent as a json string so StringSerializer is selected. .

```java
    private static Producer<String, String> createProducer() {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }
```
The object that is sent (Item) is a simple Java object (POJO) and should be serializable by the serialization library (Jackson in our case).

```java
    private void send(Item item) throws IOException {
        String message = mapper.writeValueAsString(item);
        ProducerRecord<String, String> record = new ProducerRecord<>("items", message);
        producer.send(record);
    }
```

### The consumer
The consumer reads the objects as JSON from the Kafka queue and convert (deserializes) them back to the original object .

The basic properties of the consumer similar to the ones of the producer (note that the Serializer are replaced with a **De**serializer)
In addition, the consumer group must be specified.

```java
    private Consumer<String, String> createConsumer() {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(GROUP_ID_CONFIG, "example");
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        return new KafkaConsumer<>(props);
    }
```

The consumer reads the objects from subscribed topics and then convert and process them.

```java
    public void consume() {
        consumer.subscribe(Arrays.asList("items"));
        while (true) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(1000);
                records.forEach(this::processRecord);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void processRecord(ConsumerRecord<String, String> record) {
        try {
            Item item = mapper.readValue(record.value(), Item.class);
            processItem(item);
        } catch (IOException e) {
            // handle error
        }
    }
```

## Sending an Object with Avro Serialization
[Apache Avro](https://avro.apache.org/docs/1.8.2/index.html) is a data serialization system that provides a compact and fast binary data format.  
We will use it to send serialized objects and read them from Kafka.

### The Schema Registry
[Schema Registry](https://github.com/confluentinc/schema-registry) is a service that manages the schemas of Avro so the producer and the consumer speaks the same language.
Running the registry locally is as simple as adding its settings to the [docker-compose.yml](Generic/docker-compose.yml) file:
```yaml
  schema-registry:
    image: confluentinc/cp-schema-registry:3.3.1
    network_mode: host
    depends_on:
      - zookeeper
      - kafka
    environment:
      SCHEMA_REGISTRY_KAFKASTORE_CONNECTION_URL: localhost:32181
      SCHEMA_REGISTRY_HOST_NAME: localhost
      SCHEMA_REGISTRY_LISTENERS: http://0.0.0.0:8081
```

### GenericRecord vs. SpecificRecord
There are basically 2 ways to exchange Avro objects GenericRecord and SpecificRecord.  
GenericRecord is a record that contains the object data in the form of a map structure.  
An Item object, for example, can be represented as:
```
Item: 
    UTF8: name
    UTF8: description
    long: sku
    double: price
```

SpecificRecord, on the other hand, contains a modified version ot the object, that knows how to serialize / deserialize itself so the consumer doesn't have to deserialize it explicitly.
First, let's examine the generic way

### The generic way
#### The producer

The producer has to be modified to create and send the serialized objects instead of JSON Strings, so we have to tell it to serialize the values with the KafkaAvroSerializer and to use the schema registry for exchanging the schema with the consumer.
```java
    private static Producer<String, Object> createProducer() {
        Properties props = new Properties();
        ...
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        return new KafkaProducer<>(props);
    }
```

At this stage, the schema has to be specified inline and object has to be explicitly serialized before sending.
Let's first create a schema for the Item object:
```java
    private Schema createSchema() {
        //language=JSON
        String userSchema = "{\"type\":\"record\",\n" +
                "  \"name\":\"item\",\n" +
                "  \"fields\":[\n" +
                "    {\"name\":\"name\",\"type\":\"string\"},\n" +
                "    {\"name\":\"description\",\"type\":\"string\"},\n" +
                "    {\"name\":\"sku\",\"type\":\"long\"},\n" +
                "    {\"name\":\"price\",\"type\":\"double\"}\n" +
                "  ]}";
        Schema.Parser parser = new Schema.Parser();
        return parser.parse(userSchema);
    }
```
And the send() method should be modified to:
```java
    private void send(Item item) {
        GenericRecord avroRecord = createAvroRecord(item);
        ProducerRecord<String, Object> record = new ProducerRecord<>("items", avroRecord);
        try {
            producer.send(record);
        } catch (SerializationException e) {
            // handle error
        }
    }
```

#### The consumer
The consumer has to be modified to expect serialized objects and deserialize them with 
```java
    private static Consumer<String, Item> createConsumer() {
            Properties props = new Properties();
            ...
            props.put(VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
            props.put(SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
            return new KafkaConsumer<>(props);
        }
```
The consumer reads has to deserialize the Avro object back to the Java's Item POJO.
```java
    private void processRecord(ConsumerRecord<String, Object> record) {
        Record value = (Record) record.value();
        Item item = parseItem(value);
        processItem(item);
    }

    private Item parseItem(Record record) {
        return new Item(
                ((Utf8) record.get("name")).toString(),
                ((Utf8) record.get("description")).toString(),
                (Long) record.get("sku"),
                (Double) record.get("price"));
    }
```

### The specific way
As noted above, SpecificRecord contains a modified version ot the object, that knows how to serialize / deserialize itself.  
The class of that object can be generated automatically from an Avro schema file.
Let's create the following schema and place it under [src/main/avro/Item.avsc](Specific/src/main/avro/Item.avsc)
```json
{
  "namespace": "io.github.msayag.kafka.avro",
  "type": "record",
  "name": "Item",
  "fields": [
    {
      "name": "name",
      "type": "string"
    },
    {
      "name": "description",
      "type": "string"
    },
    {
      "name": "sku",
      "type": "long"
    },
    {
      "name": "price",
      "type": "double"
    }
  ]
}
```
After adding the following to the [build.gradle](Specific/build.gradle) file, the class Item will be created every time the `build` task is invoked.
```groovy
apply plugin: "com.commercehub.gradle.plugin.avro"

buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath "com.commercehub.gradle.plugin:gradle-avro-plugin:0.11.0"
    }
}
```
#### The producer

The use of the generated class let us simplify the producer and the consumer.  
The producer doesn't have to create the schema explicitly, nor to create a GenericRecord before sending the message.
The createSchema() methos is not used any more and can be removed, and the send() method is simplified to:
```java
    private void send(Item item) {
        ProducerRecord<String, Item> record = new ProducerRecord<>("items", item);
        try {
            producer.send(record);
        } catch (SerializationException e) {
            // handle error
        }
    }
```

#### The consumer
We need to tell the consumer to deserialize the object as a specific record
```java
    private static Consumer<String, Item> createConsumer() {
        Properties props = new Properties();
        ...
        props.put(SPECIFIC_AVRO_READER_CONFIG, "true");
        return new KafkaConsumer<>(props);
    }
```
It also doesn't have to parse the item anymore and the processRecord() is simplified to:
```java
    private void processRecord(ConsumerRecord<String, Item> record) {
        Item item = record.value();
        processItem(item);
    }
```
## Kafka Streams
[Kafka Streams](https://kafka.apache.org/documentation/streams/) is a client library for building applications and microservices.
It let us stream messages from one service to another and process, aggregate and group them without the need to explicitly poll, parse and send them back to other Kafka topics.  
The consumer has to be rewritten as
```java
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
```
The producer, on the other hand, doesn't have to be modified at all.

---
#### Execution instructions
> All the code for this tutorial can be downloaded from the GitHub repository using the links above.  
> To run the examples:
> 1. Enter the folder of the specific section
> 1. Run the Docker containers:  
>     `docker-compose up -d`
> 1. Run the producer:
>     `./gradlew run -Pmode=producer`  
> 1. Run the consumer:
>     `./gradlew run -Pmode=consumer`  
> 1. When done, stop the dockers:
>     `docker-compose down`