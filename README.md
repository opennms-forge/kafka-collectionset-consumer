# Kafka CollectionSet Consumer

A reference implementation for consuming metric CollectionSets, producered by the `opennms-kafka-producer` feature, from Apache Kafka.

## Installation

```
mvn clean install
```
... should produce a jar file in `target/`.

## Usage

Create a consumer properties file to configure your environment.  An example properties file is available at `resources/consumer.properties.example`

Some properties can be overridden on the command-line:
```shell
$ java -jar ./collectionset-kafka-consumer-1.0.0-SNAPSHOT.jar --help
usage: collectionset-kafka-consumer [--bootstrap-servers <arg>] --config
       <arg> [--group-id <arg>] [--help] [--threads <n>] --topic <arg>
    --bootstrap-servers <arg>   Override Kafka bootstrap.servers
    --config <arg>              Path to Kafka consumer properties file
    --group-id <arg>            Override Kafka consumer group.id
    --help                      Print this help
    --threads <n>               Number of consumer threads (default: 1)
    --topic <arg>               Kafka topic to consume
```

You can tailor the number of consumer threads to your environment using the `--threads` option.
Specify the topic from which to consumer using the `--topic` option.

When a message is received, the CollectionSet is parsed according to [the CollectionSet proto](https://github.com/OpenNMS/opennms/blob/develop/features/kafka/producer/src/main/proto/collectionset.proto) and printed to the console:
```shell
==== CollectionSet @ 1767411471704
timestamp: 1767411471704
resource {
  response {
    instance: "127.0.0.2"
  }
  numeric {
    group: "ssh"
    name: "ssh"
    value: 15.87699
    metric_value {
      value: 15.87699
    }
  }
}

==== End CollectionSet
```

The `process` method in `ConsumerWorker.java` can be extended to mangle and persist incoming CollectionSets to suit your needs.

## Instrumentation

This reference consumer application is instrumented with metrics exposed via JMX.  It maintains metrics on:

 * Number of messages consumed
 * A histogram tracking the distribution of message sizes processed
 * Per-second metric processing rate (1m/5m/15m)
 * A histogram tracking time to process messages in the `process()` method

These metrics are visible via jconsole or any other JMX-compatible tool.

## Example

```shell
$ cat /tmp/consumer.properties
bootstrap.servers=kafka:9094
group.id=collectionset-consumer
auto.offset.reset=earliest

key.deserializer=org.apache.kafka.common.serialization.ByteArrayDeserializer
value.deserializer=org.apache.kafka.common.serialization.ByteArrayDeserializer
$ java -jar ./collectionset-kafka-consumer-1.0.0-SNAPSHOT.jar --config /tmp/consumer.properties --threads 1 --topic metrics
Starting 1 consumer threads
[pool-1-thread-1] INFO org.apache.kafka.common.config.AbstractConfig - ConsumerConfig values:
        allow.auto.create.topics = true
        auto.commit.interval.ms = 5000
        auto.offset.reset = earliest
        bootstrap.servers = [kafka:9094]
        check.crcs = true
        client.dns.lookup = use_all_dns_ips
        client.id = consumer-collectionset-consumer-1
        client.rack =
        connections.max.idle.ms = 540000
        default.api.timeout.ms = 60000
        enable.auto.commit = true
        enable.metrics.push = true
        exclude.internal.topics = true
        fetch.max.bytes = 52428800
        fetch.max.wait.ms = 500
        fetch.min.bytes = 1
        group.id = collectionset-consumer
        group.instance.id = null
        group.protocol = classic
        group.remote.assignor = null
        heartbeat.interval.ms = 3000
        interceptor.classes = []
        internal.leave.group.on.close = true
        internal.throw.on.fetch.stable.offset.unsupported = false
        isolation.level = read_uncommitted
        key.deserializer = class org.apache.kafka.common.serialization.ByteArrayDeserializer
        max.partition.fetch.bytes = 1048576
        max.poll.interval.ms = 300000
        max.poll.records = 500
        metadata.max.age.ms = 300000
        metadata.recovery.rebootstrap.trigger.ms = 300000
        metadata.recovery.strategy = rebootstrap
        metric.reporters = [org.apache.kafka.common.metrics.JmxReporter]
        metrics.num.samples = 2
        metrics.recording.level = INFO
        metrics.sample.window.ms = 30000
        partition.assignment.strategy = [class org.apache.kafka.clients.consumer.RangeAssignor, class org.apache.kafka.clients.consumer.CooperativeStickyAssignor]
        receive.buffer.bytes = 65536
        reconnect.backoff.max.ms = 1000
        reconnect.backoff.ms = 50
        request.timeout.ms = 30000
        retry.backoff.max.ms = 1000
        retry.backoff.ms = 100
        sasl.client.callback.handler.class = null
        sasl.jaas.config = null
        sasl.kerberos.kinit.cmd = /usr/bin/kinit
        sasl.kerberos.min.time.before.relogin = 60000
        sasl.kerberos.service.name = null
        sasl.kerberos.ticket.renew.jitter = 0.05
        sasl.kerberos.ticket.renew.window.factor = 0.8
        sasl.login.callback.handler.class = null
        sasl.login.class = null
        sasl.login.connect.timeout.ms = null
        sasl.login.read.timeout.ms = null
        sasl.login.refresh.buffer.seconds = 300
        sasl.login.refresh.min.period.seconds = 60
        sasl.login.refresh.window.factor = 0.8
        sasl.login.refresh.window.jitter = 0.05
        sasl.login.retry.backoff.max.ms = 10000
        sasl.login.retry.backoff.ms = 100
        sasl.mechanism = GSSAPI
        sasl.oauthbearer.assertion.algorithm = RS256
        sasl.oauthbearer.assertion.claim.aud = null
        sasl.oauthbearer.assertion.claim.exp.seconds = 300
        sasl.oauthbearer.assertion.claim.iss = null
        sasl.oauthbearer.assertion.claim.jti.include = false
        sasl.oauthbearer.assertion.claim.nbf.seconds = 60
        sasl.oauthbearer.assertion.claim.sub = null
        sasl.oauthbearer.assertion.file = null
        sasl.oauthbearer.assertion.private.key.file = null
        sasl.oauthbearer.assertion.private.key.passphrase = null
        sasl.oauthbearer.assertion.template.file = null
        sasl.oauthbearer.client.credentials.client.id = null
        sasl.oauthbearer.client.credentials.client.secret = null
        sasl.oauthbearer.clock.skew.seconds = 30
        sasl.oauthbearer.expected.audience = null
        sasl.oauthbearer.expected.issuer = null
        sasl.oauthbearer.header.urlencode = false
        sasl.oauthbearer.jwks.endpoint.refresh.ms = 3600000
        sasl.oauthbearer.jwks.endpoint.retry.backoff.max.ms = 10000
        sasl.oauthbearer.jwks.endpoint.retry.backoff.ms = 100
        sasl.oauthbearer.jwks.endpoint.url = null
        sasl.oauthbearer.jwt.retriever.class = class org.apache.kafka.common.security.oauthbearer.DefaultJwtRetriever
        sasl.oauthbearer.jwt.validator.class = class org.apache.kafka.common.security.oauthbearer.DefaultJwtValidator
        sasl.oauthbearer.scope = null
        sasl.oauthbearer.scope.claim.name = scope
        sasl.oauthbearer.sub.claim.name = sub
        sasl.oauthbearer.token.endpoint.url = null
        security.protocol = PLAINTEXT
        security.providers = null
        send.buffer.bytes = 131072
        session.timeout.ms = 45000
        share.acknowledgement.mode = implicit
        socket.connection.setup.timeout.max.ms = 30000
        socket.connection.setup.timeout.ms = 10000
        ssl.cipher.suites = null
        ssl.enabled.protocols = [TLSv1.2, TLSv1.3]
        ssl.endpoint.identification.algorithm = https
        ssl.engine.factory.class = null
        ssl.key.password = null
        ssl.keymanager.algorithm = SunX509
        ssl.keystore.certificate.chain = null
        ssl.keystore.key = null
        ssl.keystore.location = null
        ssl.keystore.password = null
        ssl.keystore.type = JKS
        ssl.protocol = TLSv1.3
        ssl.provider = null
        ssl.secure.random.implementation = null
        ssl.trustmanager.algorithm = PKIX
        ssl.truststore.certificates = null
        ssl.truststore.location = null
        ssl.truststore.password = null
        ssl.truststore.type = JKS
        value.deserializer = class org.apache.kafka.common.serialization.ByteArrayDeserializer

[pool-1-thread-1] INFO org.apache.kafka.common.telemetry.internals.KafkaMetricsCollector - initializing Kafka metrics collector
[pool-1-thread-1] INFO org.apache.kafka.common.utils.AppInfoParser - Kafka version: 4.1.1
[pool-1-thread-1] INFO org.apache.kafka.common.utils.AppInfoParser - Kafka commitId: be816b82d25370ce
[pool-1-thread-1] INFO org.apache.kafka.common.utils.AppInfoParser - Kafka startTimeMs: 1768254161346
[pool-1-thread-1] INFO org.apache.kafka.clients.consumer.internals.ClassicKafkaConsumer - [Consumer clientId=consumer-collectionset-consumer-1, groupId=collectionset-consumer] Subscribed to topic(s): metrics
[pool-1-thread-1] INFO org.apache.kafka.clients.Metadata - [Consumer clientId=consumer-collectionset-consumer-1, groupId=collectionset-consumer] Cluster ID: AuWHI3ZvTLC9W7Wv933ELQ
[pool-1-thread-1] INFO org.apache.kafka.clients.consumer.internals.ConsumerCoordinator - [Consumer clientId=consumer-collectionset-consumer-1, groupId=collectionset-consumer] Discovered group coordinator kafka.example.com:9094 (id: 2147482646 rack: null isFenced: false)
[pool-1-thread-1] INFO org.apache.kafka.clients.consumer.internals.ConsumerCoordinator - [Consumer clientId=consumer-collectionset-consumer-1, groupId=collectionset-consumer] (Re-)joining group
[pool-1-thread-1] INFO org.apache.kafka.clients.consumer.internals.ConsumerCoordinator - [Consumer clientId=consumer-collectionset-consumer-1, groupId=collectionset-consumer] Request joining group due to: need to re-join with the given member-id: consumer-collectionset-consumer-1-40de06fa-aba6-4127-aa5a-e7656e6da29c
[pool-1-thread-1] INFO org.apache.kafka.clients.consumer.internals.ConsumerCoordinator - [Consumer clientId=consumer-collectionset-consumer-1, groupId=collectionset-consumer] (Re-)joining group
[pool-1-thread-1] INFO org.apache.kafka.clients.consumer.internals.ConsumerCoordinator - [Consumer clientId=consumer-collectionset-consumer-1, groupId=collectionset-consumer] Successfully joined group with generation Generation{generationId=7, memberId='consumer-collectionset-consumer-1-40de06fa-aba6-4127-aa5a-e7656e6da29c', protocol='range'}
[pool-1-thread-1] INFO org.apache.kafka.clients.consumer.internals.ConsumerCoordinator - [Consumer clientId=consumer-collectionset-consumer-1, groupId=collectionset-consumer] Finished assignment for group at generation 7: {consumer-collectionset-consumer-1-40de06fa-aba6-4127-aa5a-e7656e6da29c=Assignment(partitions=[metrics-0, metrics-1, metrics-2, metrics-3])}
[pool-1-thread-1] INFO org.apache.kafka.clients.consumer.internals.ConsumerCoordinator - [Consumer clientId=consumer-collectionset-consumer-1, groupId=collectionset-consumer] Successfully synced group in generation Generation{generationId=7, memberId='consumer-collectionset-consumer-1-40de06fa-aba6-4127-aa5a-e7656e6da29c', protocol='range'}
[pool-1-thread-1] INFO org.apache.kafka.clients.consumer.internals.ConsumerCoordinator - [Consumer clientId=consumer-collectionset-consumer-1, groupId=collectionset-consumer] Notifying assignor about the new Assignment(partitions=[metrics-0, metrics-1, metrics-2, metrics-3])
[pool-1-thread-1] INFO org.apache.kafka.clients.consumer.internals.ConsumerRebalanceListenerInvoker - [Consumer clientId=consumer-collectionset-consumer-1, groupId=collectionset-consumer] Adding newly assigned partitions: [metrics-0, metrics-1, metrics-2, metrics-3]
[pool-1-thread-1] INFO org.apache.kafka.clients.consumer.internals.ConsumerUtils - Setting offset for partition metrics-3 to the committed offset FetchPosition{offset=1179, offsetEpoch=Optional[0], currentLeader=LeaderAndEpoch{leader=Optional[kafka.example.com:9094 (id: 1001 rack: null isFenced: false)], epoch=0}}
[pool-1-thread-1] INFO org.apache.kafka.clients.consumer.internals.ConsumerUtils - Setting offset for partition metrics-2 to the committed offset FetchPosition{offset=2500, offsetEpoch=Optional[0], currentLeader=LeaderAndEpoch{leader=Optional[kafka.example.com:9094 (id: 1001 rack: null isFenced: false)], epoch=0}}
[pool-1-thread-1] INFO org.apache.kafka.clients.consumer.internals.ConsumerUtils - Setting offset for partition metrics-1 to the committed offset FetchPosition{offset=2500, offsetEpoch=Optional[0], currentLeader=LeaderAndEpoch{leader=Optional[kafka.example.com:9094 (id: 1001 rack: null isFenced: false)], epoch=0}}
[pool-1-thread-1] INFO org.apache.kafka.clients.consumer.internals.ConsumerUtils - Setting offset for partition metrics-0 to the committed offset FetchPosition{offset=2105, offsetEpoch=Optional[0], currentLeader=LeaderAndEpoch{leader=Optional[kafka.example.com:9094 (id: 1001 rack: null isFenced: false)], epoch=0}}
==== CollectionSet @ 1767217376786
timestamp: 1767217376786
resource {
  node {
    node_id: 1
    foreign_source: "1"
    foreign_id: "selfmonitor"
    node_label: "localhost"
    location: "Default"
  }
  resource_id: "node[1].nodeSnmp[]"
  resource_type_name: "nodeSnmp"
  numeric {
    group: "opennmsEventQuery"
    name: "OnmsEventCount"
    value: 14.0
    metric_value {
      value: 14.0
    }
  }
  numeric {
    group: "opennmsEventQuery"
    name: "OnmsEventEstimate"
    value: 1359.0
    metric_value {
      value: 1359.0
    }
  }
  numeric {
    group: "opennmsAlarmQuery"
    name: "OnmsAlarmCount"
    metric_value {
    }
  }
  numeric {
    group: "opennmsNodeQuery"
    name: "OnmsNodeCount"
    value: 5.0
    metric_value {
      value: 5.0
    }
  }
}

==== End CollectionSet
^CShutdown requested
```
