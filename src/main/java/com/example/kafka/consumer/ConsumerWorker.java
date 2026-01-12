package com.example.kafka.consumer;

import com.codahale.metrics.Timer.Context;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.opennms.features.kafka.producer.model.CollectionSetProtos;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
public class ConsumerWorker implements Runnable {

    private final Properties consumerProps;
    private final String topic;

    public ConsumerWorker(Properties consumerProps, String topic) {
        this.consumerProps = consumerProps;
        this.topic = topic;
    }

    @Override
    public void run() {
        try (KafkaConsumer<byte[], byte[]> consumer =
                     new KafkaConsumer<>(consumerProps)) {

            consumer.subscribe(Collections.singletonList(topic));

            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<byte[], byte[]> records =
                        consumer.poll(Duration.ofSeconds(1));

                records.forEach(record -> {
                    if (record.value() == null) return;

                    try {
                        CollectionSetProtos.CollectionSet cs =
                                CollectionSetProtos.CollectionSet
                                        .parseFrom(record.value());
                        // Message size is useful
                        CollectionSetConsumer.collectionSetSize.update(cs.getSerializedSize());
                        // Do something with the message
                        try (Context ctx = CollectionSetConsumer.messageMetrics.time()) {
                            process(cs);
                        }
                    } catch (Exception e) {
                        // handle malformed message
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    private void process(CollectionSetProtos.CollectionSet cs) {

        // Do magical things with the data here
        System.out.println("==== CollectionSet @ " + cs.getTimestamp());
        System.out.println(cs);
        System.out.println("==== End CollectionSet");
    }
}
