/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.component.debezium.postgres.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

class DebeziumPostgresProcessor {

    private static final String FEATURE = "camel-debezium-postgres";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void reflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "io.debezium.connector.postgresql.PostgresConnector",
                "io.debezium.connector.postgresql.PostgresConnectorTask",
                "io.debezium.connector.postgresql.PostgresSourceInfoStructMaker",
                "io.debezium.connector.postgresql.snapshot.lock.NoSnapshotLock",
                "io.debezium.connector.postgresql.snapshot.lock.SharedSnapshotLock",
                "io.debezium.connector.postgresql.snapshot.query.SelectAllSnapshotQuery")
                .build());

        // Register Kafka Connect offset storage classes for native compilation (issue #8621)
        // KafkaOffsetBackingStore is loaded dynamically by Debezium when configured via offsetStorage parameter
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.kafka.connect.storage.KafkaOffsetBackingStore",
                "org.apache.kafka.connect.storage.FileOffsetBackingStore",
                "org.apache.kafka.connect.storage.MemoryOffsetBackingStore",
                "org.apache.kafka.connect.runtime.WorkerConfig",
                "org.apache.kafka.connect.runtime.distributed.DistributedConfig",
                "org.apache.kafka.connect.runtime.standalone.StandaloneConfig")
                .methods().fields().build());

        // Register Kafka serializers/deserializers used by KafkaOffsetBackingStore (issue #8621)
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.kafka.common.serialization.ByteArraySerializer",
                "org.apache.kafka.common.serialization.ByteArrayDeserializer",
                "org.apache.kafka.common.serialization.StringSerializer",
                "org.apache.kafka.common.serialization.StringDeserializer")
                .methods().build());

        // Register Kafka client classes used internally by KafkaOffsetBackingStore (issue #8621)
        // These are needed to avoid NPE during offset store initialization in native mode
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.kafka.clients.producer.KafkaProducer",
                "org.apache.kafka.clients.producer.ProducerConfig",
                "org.apache.kafka.clients.consumer.KafkaConsumer",
                "org.apache.kafka.clients.consumer.ConsumerConfig",
                "org.apache.kafka.clients.admin.AdminClient",
                "org.apache.kafka.clients.admin.AdminClientConfig")
                .methods().fields().constructors().build());

        // Register Kafka Connect converters used for offset serialization (issue #8621)
        // KafkaOffsetBackingStore uses JsonConverter by default for key/value serialization
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.kafka.connect.json.JsonConverter",
                "org.apache.kafka.connect.storage.StringConverter",
                "org.apache.kafka.connect.converters.ByteArrayConverter")
                .methods().constructors().build());

        // Register Kafka internal classes loaded via reflection (issue #8621)
        // These are needed by Kafka producer/consumer internals
        // JmxReporter MUST be registered for reflection so Kafka can load it dynamically,
        // even though it's substituted in graal/JmxReporterSubstitutions.java to be a no-op
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.kafka.clients.NetworkClient",
                "org.apache.kafka.clients.Metadata",
                "org.apache.kafka.clients.producer.internals.Sender",
                "org.apache.kafka.clients.producer.internals.BuiltInPartitioner",
                "org.apache.kafka.clients.producer.RoundRobinPartitioner",
                "org.apache.kafka.clients.consumer.internals.ConsumerCoordinator",
                "org.apache.kafka.clients.consumer.internals.Fetcher",
                "org.apache.kafka.common.utils.AppInfoParser",
                "org.apache.kafka.common.metrics.JmxReporter")
                .methods().fields().constructors().build());

        // Register Kafka Connect runtime classes needed by offset storage (issue #8621)
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.kafka.connect.runtime.WorkerConfig$1",
                "org.apache.kafka.connect.runtime.WorkerConfig$2",
                "org.apache.kafka.connect.storage.Converter",
                "org.apache.kafka.connect.storage.HeaderConverter")
                .methods().build());

        // Register Kafka Connect utility classes (issue #8621)
        // ConnectUtils is used to add metrics context properties
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.kafka.connect.util.ConnectUtils",
                "org.apache.kafka.connect.runtime.ConnectMetrics",
                "org.apache.kafka.connect.runtime.ConnectMetricsRegistry")
                .methods().fields().build());

        // Register Kafka common metrics classes (issue #8621)
        // These are used by Kafka clients created by KafkaBasedLog
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.kafka.common.metrics.Metrics",
                "org.apache.kafka.common.metrics.MetricsReporter",
                "org.apache.kafka.common.metrics.MetricsContext",
                "org.apache.kafka.common.metrics.KafkaMetricsContext",
                "org.apache.kafka.common.metrics.MetricConfig",
                "org.apache.kafka.common.metrics.Sensor",
                "org.apache.kafka.common.metrics.Sensor$RecordingLevel")
                .methods().constructors().build());

        // Register Kafka telemetry reporter (issue #8621)
        // ClientTelemetryReporter is loaded dynamically by Kafka clients
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.kafka.common.telemetry.internals.ClientTelemetryReporter")
                .methods().constructors().build());

        // Register additional Kafka client internals for native mode (issue #8621)
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.kafka.common.network.Selector",
                "org.apache.kafka.common.network.KafkaChannel",
                "org.apache.kafka.common.network.NetworkReceive",
                "org.apache.kafka.common.protocol.ApiKeys",
                "org.apache.kafka.common.requests.AbstractRequest",
                "org.apache.kafka.common.requests.AbstractResponse")
                .methods().fields().build());

        // Register Kafka utility classes used by offset store (issue #8621)
        // Utils.closeQuietly() is called in KafkaOffsetBackingStore.stop()
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.kafka.common.utils.Utils",
                "org.apache.kafka.common.utils.Time",
                "org.apache.kafka.common.utils.SystemTime",
                "org.apache.kafka.common.utils.LogContext")
                .methods().fields().constructors().build());

        // Register actual AdminClient implementation (issue #8621)
        // AdminClient is an abstract class, KafkaAdminClient is the actual implementation
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.kafka.clients.admin.KafkaAdminClient")
                .methods().fields().constructors().build());

        // Register AdminClient result classes (issue #8621)
        // These are returned by AdminClient operations like describeCluster()
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.kafka.clients.admin.DescribeClusterResult",
                "org.apache.kafka.clients.admin.DescribeTopicsResult",
                "org.apache.kafka.clients.admin.CreateTopicsResult",
                "org.apache.kafka.clients.admin.ListOffsetsResult")
                .methods().fields().constructors().build());

        // Register KafkaFuture and related classes (issue #8621)
        // KafkaFuture is returned by AdminClient async operations
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.kafka.common.KafkaFuture",
                "org.apache.kafka.common.KafkaFuture$BiConsumer",
                "org.apache.kafka.common.internals.KafkaFutureImpl")
                .methods().fields().constructors().build());

        // Register Kafka cluster metadata classes (issue #8621)
        // Returned by describeCluster() operation
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.kafka.common.Node",
                "org.apache.kafka.common.Cluster",
                "org.apache.kafka.common.ClusterResource")
                .methods().fields().constructors().build());

        // Register AdminClient internal classes (issue #8621)
        // These are used during Admin.create() and KafkaAdminClient.createInternal()
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.kafka.clients.admin.AdminBootstrapAddresses",
                "org.apache.kafka.clients.admin.internals.AdminMetadataManager",
                "org.apache.kafka.clients.ClientUtils",
                "org.apache.kafka.clients.DefaultHostResolver",
                "org.apache.kafka.clients.ApiVersions",
                "org.apache.kafka.common.utils.KafkaThread")
                .methods().fields().constructors().build());

        // Register Kafka partition assignors (issue #8621)
        // These are loaded dynamically and needed by KafkaConsumer
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.kafka.clients.consumer.RangeAssignor",
                "org.apache.kafka.clients.consumer.RoundRobinAssignor",
                "org.apache.kafka.clients.consumer.StickyAssignor",
                "org.apache.kafka.clients.consumer.CooperativeStickyAssignor")
                .methods().constructors().build());

        // Register Kafka consumer enums (issue #8621)
        // IsolationLevel is used by KafkaOffsetBackingStore when exactlyOnce is enabled
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.kafka.common.IsolationLevel")
                .methods().build());

        // Register OAuth/JWT classes shown in AdminClientConfig (issue #8621)
        // These are default classes that Kafka tries to instantiate via reflection
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.kafka.common.security.oauthbearer.DefaultJwtRetriever",
                "org.apache.kafka.common.security.oauthbearer.DefaultJwtValidator")
                .methods().constructors().build());

        // Register KafkaBasedLog and TopicAdmin for KafkaOffsetBackingStore (issue #8621)
        // KafkaBasedLog is used internally by KafkaOffsetBackingStore to read/write offsets
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.kafka.connect.util.KafkaBasedLog",
                "org.apache.kafka.connect.util.TopicAdmin",
                "org.apache.kafka.connect.util.TopicAdmin$NewTopic",
                "org.apache.kafka.connect.util.SharedTopicAdmin")
                .methods().fields().constructors().build());
    }

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem("com.google.protobuf.JavaFeaturesProto"));
        // Kafka clients must be initialized at runtime (issue #8621)
        runtimeInitializedClass
                .produce(new RuntimeInitializedClassBuildItem("org.apache.kafka.clients.producer.KafkaProducer"));
        runtimeInitializedClass
                .produce(new RuntimeInitializedClassBuildItem("org.apache.kafka.clients.consumer.KafkaConsumer"));
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem("org.apache.kafka.clients.admin.AdminClient"));
        runtimeInitializedClass
                .produce(new RuntimeInitializedClassBuildItem("org.apache.kafka.clients.admin.KafkaAdminClient"));
        // Kafka Connect util classes must be initialized at runtime (issue #8621)
        runtimeInitializedClass
                .produce(new RuntimeInitializedClassBuildItem("org.apache.kafka.connect.util.KafkaBasedLog"));
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem("org.apache.kafka.connect.util.TopicAdmin"));
        runtimeInitializedClass
                .produce(new RuntimeInitializedClassBuildItem("org.apache.kafka.connect.util.SharedTopicAdmin"));
        // SASL authentication classes use Random and must be initialized at runtime
        runtimeInitializedClass.produce(
                new RuntimeInitializedClassBuildItem("org.apache.kafka.common.security.authenticator.SaslClientAuthenticator"));
        // Protobuf classes use Unsafe and must be initialized at runtime (issue #8621)
        // This prevents "RecomputeFieldValue.FieldOffset automatic field value transformation failed" warnings
        runtimeInitializedClass
                .produce(new RuntimeInitializedClassBuildItem("org.apache.kafka.shaded.com.google.protobuf.UnsafeUtil"));
        runtimeInitializedClass
                .produce(new RuntimeInitializedClassBuildItem("com.google.protobuf.UnsafeUtil"));
    }

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("io.debezium", "debezium-connector-postgres"));
        // Note: Kafka Connect classes are registered for reflection above
        // We don't index connect-api or connect-runtime to avoid CDI bean discovery issues (issue #8621)
    }
}
