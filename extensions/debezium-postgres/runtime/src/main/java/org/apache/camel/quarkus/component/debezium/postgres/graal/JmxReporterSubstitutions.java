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
package org.apache.camel.quarkus.component.debezium.postgres.graal;

import java.util.List;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.kafka.common.metrics.KafkaMetric;

/**
 * Substitutions to disable JMX reporting in Kafka for native mode.
 * GraalVM native images do not support MBeanServer (returns null), which causes
 * NullPointerException when Kafka's JmxReporter tries to register metrics.
 *
 * This approach follows the pattern used by Quarkus Kafka client extension and
 * is consistent with other Camel Quarkus extensions (slack, paho, activemq, etc).
 *
 * See:
 * https://github.com/quarkusio/quarkus/blob/main/extensions/kafka-client/deployment/src/main/java/io/quarkus/kafka/client/deployment/KafkaProcessor.java
 * See: https://issues.apache.org/jira/browse/KAFKA-8629
 */
final class JmxReporterSubstitutions {
}

@TargetClass(className = "org.apache.kafka.common.metrics.JmxReporter")
final class Target_org_apache_kafka_common_metrics_JmxReporter {

    @Substitute
    public Target_org_apache_kafka_common_metrics_JmxReporter() {
        // No-op constructor: prevent MBeanServer initialization
    }

    @Substitute
    public void init(List<KafkaMetric> metrics) {
        // No-op: JMX is not supported in GraalVM native mode
    }

    @Substitute
    public void metricChange(KafkaMetric metric) {
        // No-op: JMX is not supported in GraalVM native mode
    }

    @Substitute
    public void metricRemoval(KafkaMetric metric) {
        // No-op: JMX is not supported in GraalVM native mode
    }

    @Substitute
    public void close() {
        // No-op: JMX is not supported in GraalVM native mode
    }
}
