package com.example.kafka.consumer;

import com.codahale.metrics.Timer.Context;
import com.google.protobuf.util.JsonFormat;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.opennms.features.kafka.producer.model.CollectionSetProtos;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class ConsumerWorker implements Runnable {

    private final Properties consumerProps;
    private final String topic;
    private final String format;
    private final boolean raw;
    private final int workerIndex;
    private final int workerCount;

    /** Serialises raw writes so payloads from different worker threads never interleave. */
    private static final Object RAW_OUT_LOCK = new Object();

    public ConsumerWorker(Properties consumerProps, String topic, String format,
                          boolean raw, int workerIndex, int workerCount) {
        this.consumerProps = consumerProps;
        this.topic = topic;
        this.format = format;
        this.raw = raw;
        this.workerIndex = workerIndex;
        this.workerCount = workerCount;
    }

    /** True when no group.id is configured, so group management is unavailable. */
    public static boolean isStandalone(Properties props) {
        String groupId = props.getProperty(ConsumerConfig.GROUP_ID_CONFIG);
        return groupId == null || groupId.isBlank();
    }

    @Override
    public void run() {
        try {
            consume();
        } catch (Throwable t) {
            // Without this, an exception thrown here is captured by the
            // executor's Future and never seen; the thread goes idle and
            // the JVM sits doing nothing.
            System.err.println("Consumer thread " + Thread.currentThread().getName()
                    + " failed, exiting: " + t);
            t.printStackTrace();
            System.exit(1);
        }
    }

    private void consume() {
        try (KafkaConsumer<byte[], byte[]> consumer =
                     new KafkaConsumer<>(consumerProps)) {

            if (isStandalone(consumerProps)) {
                assignStandalone(consumer);
            } else {
                consumer.subscribe(Collections.singletonList(topic));
            }

            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<byte[], byte[]> records =
                        consumer.poll(Duration.ofSeconds(1));

                records.forEach(record -> {
                    if (record.value() == null) return;

                    if (raw) {
                        writeRaw(record.value());
                        return;
                    }

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

    /**
     * Standalone mode: without a group.id the broker will not do partition
     * assignment or offset commits for us, so look up the topic's partitions
     * and assign a slice of them to this worker directly. Nothing is committed,
     * so every start positions itself per auto.offset.reset.
     */
    private void assignStandalone(KafkaConsumer<byte[], byte[]> consumer) {
        List<PartitionInfo> partitions = consumer.partitionsFor(topic);
        if (partitions == null || partitions.isEmpty()) {
            throw new IllegalStateException("Topic '" + topic
                    + "' has no partitions visible to this client (does it exist, and do we have Describe on it?)");
        }
        List<TopicPartition> mine = new ArrayList<>();
        for (PartitionInfo p : partitions) {
            if (p.partition() % workerCount == workerIndex) {
                mine.add(new TopicPartition(topic, p.partition()));
            }
        }
        (raw ? System.err : System.out).println("Standalone mode (no group.id): " + Thread.currentThread().getName()
                + " assigned " + mine.size() + " of " + partitions.size()
                + " partition(s) of '" + topic + "': " + mine);
        consumer.assign(mine);
    }

    /**
     * --raw mode: write the payload bytes to stdout exactly as they came off
     * the wire. No parsing, no decoding, no headers, and no separator between
     * messages, so the output is byte-for-byte what the producer sent.
     */
    private void writeRaw(byte[] value) {
        synchronized (RAW_OUT_LOCK) {
            System.out.writeBytes(value);
            System.out.flush();
            // PrintStream swallows IO errors; surface a closed or broken stdout
            // (e.g. the far end of a pipe went away) instead of spinning forever.
            if (System.out.checkError()) {
                throw new IllegalStateException("stdout is closed or failed, stopping raw output");
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