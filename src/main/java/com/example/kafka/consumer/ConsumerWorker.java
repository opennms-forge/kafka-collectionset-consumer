package com.example.kafka.consumer;

import com.codahale.metrics.Timer.Context;
import com.google.protobuf.util.JsonFormat;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.opennms.features.kafka.producer.model.CollectionSetProtos;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class ConsumerWorker implements Runnable {

    private final Properties consumerProps;
    private final String topic;
    private final String format;

    public ConsumerWorker(Properties consumerProps, String topic, String format) {
        this.consumerProps = consumerProps;
        this.topic = topic;
        this.format = format;
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

                    if ("raw".equals(format)) {
                        processRaw(record.topic(), record.partition(), record.offset(),
                                record.key(), record.value());
                        return;
                    }

                    try {
                        CollectionSetProtos.CollectionSet cs = "json".equals(format)
                                ? parseJson(record.value())
                                : CollectionSetProtos.CollectionSet.parseFrom(record.value());

                        CollectionSetConsumer.collectionSetSize.update(cs.getSerializedSize());
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

    private void processRaw(String topic, int partition, long offset, byte[] key, byte[] value) {
        System.out.println("==== Raw Message topic=" + topic +
                " partition=" + partition + " offset=" + offset +
                " size=" + value.length + " bytes");
        if (key != null) {
            System.out.println("Key: " + new String(key, StandardCharsets.UTF_8));
        }
        // Attempt UTF-8 text print
        System.out.println("Value (UTF-8 attempt):");
        System.out.println(new String(value, StandardCharsets.UTF_8));
        // Hex dump
        System.out.println("Value (hex):");
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < value.length; i++) {
            if (i > 0 && i % 16 == 0) hex.append('\n');
            hex.append(String.format("%02X ", value[i]));
        }
        System.out.println(hex);
        System.out.println("==== End Raw Message");
    }

    private CollectionSetProtos.CollectionSet parseJson(byte[] value) throws Exception {
        String json = new String(value, StandardCharsets.UTF_8);
        CollectionSetProtos.CollectionSet.Builder builder =
                CollectionSetProtos.CollectionSet.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(json, builder);
        return builder.build();
    }

    private void process(CollectionSetProtos.CollectionSet cs) {
        // Do magical things with the data here
        System.out.println("==== CollectionSet @ " + cs.getTimestamp());
        System.out.println(cs);
        System.out.println("==== End CollectionSet");
    }
}