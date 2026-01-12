package com.example.kafka.consumer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CliOptions {

    public static CommandLine parse(String[] args) {
        Options options = new Options();

        options.addRequiredOption(null, "config", true,
                "Path to Kafka consumer properties file");

        options.addRequiredOption(null, "topic", true,
                "Kafka topic to consume");

        options.addOption(null, "group-id", true,
                "Override Kafka consumer group.id");

        options.addOption(null, "bootstrap-servers", true,
                "Override Kafka bootstrap.servers");

        options.addOption(
                Option.builder()
                        .longOpt("threads")
                        .hasArg()
                        .argName("n")
                        .desc("Number of consumer threads (default: 1)")
                        .build()
        );


        options.addOption(null, "help", false,
                "Print this help");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            formatter.printHelp("collectionset-kafka-consumer", options, true);
            System.exit(1);
            return null;
        }
    }
}

