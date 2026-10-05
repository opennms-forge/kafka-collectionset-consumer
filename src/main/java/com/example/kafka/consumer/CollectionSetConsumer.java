package com.example.kafka.consumer;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.jmx.JmxReporter;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.io.PrintStream;
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

        // --raw dumps payload bytes verbatim to stdout, so keep our own chatter
        // on stderr in that mode to leave stdout clean for redirection.
        boolean raw = cmd.hasOption("raw");
        PrintStream info = raw ? System.err : System.out;

        JmxReporter.forRegistry(metrics)
                .inDomain("com.example.collectionset.consumer")
                .build()
                .start();

        ExecutorService executor =
                Executors.newFixedThreadPool(threads);

        if (ConsumerWorker.isStandalone(props)) {
            // Without a group.id the client refuses an explicit enable.auto.commit=true,
            // and offsets cannot be committed anyway, so force it off.
            props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
            info.println("No group.id configured: running standalone with manual partition "
                    + "assignment. Offsets will not be committed.");
        }

        if (raw) {
            info.println("Starting " + threads + " consumer thread(s) in raw mode: "
                    + "payloads will be written to stdout unmodified");
        } else {
            info.println("Starting " + threads + " consumer thread(s) with format: " + format);
        }
        info.printf(" bootstrap.servers=%s%n group.id=%s%n topic=%s%n",
                props.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG),
                props.getProperty(ConsumerConfig.GROUP_ID_CONFIG),
                cmd.getOptionValue("topic"));

        for (int i = 0; i < threads; i++) {
            // Each consumer needs its own Properties instance
            Properties consumerProps = new Properties();
            consumerProps.putAll(props);

            executor.submit(
                    new ConsumerWorker(
                            consumerProps,
                            cmd.getOptionValue("topic"),
                            format,
                            raw,
                            i,
                            threads
                    )
            );
        }

        // --- Graceful shutdown hook ---
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            info.println("Shutdown requested");
            System.out.flush();
            executor.shutdownNow();
        }));
    }
}

