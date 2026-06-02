package com.example.kafka.consumer;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.jmx.JmxReporter;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CollectionSetConsumer {

    // Start Dropwizard JMX metrics
    private static final MetricRegistry metrics = new MetricRegistry();
    public static final Timer messageMetrics = metrics.timer("messageMetrics");
    public static final Histogram collectionSetSize = metrics.histogram("messageSize");

    public static void main(String[] args) throws Exception {
        var cmd = CliOptions.parse(args);

        Properties props = KafkaConfigLoader.load(cmd.getOptionValue("config"));

        // group id override
        if (cmd.hasOption("group-id")) {
            props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                cmd.getOptionValue("group-id")
            );
        }

        // bootstrap server override
        if (cmd.hasOption("bootstrap-servers")) {
            props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                cmd.getOptionValue("bootstrap-servers")
            );
        }
        // Consumer thread count
        int threads = Integer.parseInt(
                cmd.getOptionValue("threads", "1")
        );

        String format = cmd.getOptionValue("format", "protobuf").toLowerCase();
        if (!format.equals("protobuf") && !format.equals("json") && !format.equals("raw")) {
            System.err.println("Invalid --format value '" + format + "'. Must be 'protobuf', 'json', or 'raw'.");
            System.exit(1);
        }

        JmxReporter.forRegistry(metrics)
                .inDomain("com.example.collectionset.consumer")
                .build()
                .start();

        ExecutorService executor =
                Executors.newFixedThreadPool(threads);

        System.out.println("Starting " + threads + " consumer thread(s) with format: " + format);

        for (int i = 0; i < threads; i++) {
            // Each consumer needs its own Properties instance
            Properties consumerProps = new Properties();
            consumerProps.putAll(props);

            executor.submit(
                    new ConsumerWorker(
                            consumerProps,
                            cmd.getOptionValue("topic"),
                            format
                    )
            );
        }

        // --- Graceful shutdown hook ---
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown requested");
            executor.shutdownNow();
        }));
    }
}

