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

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Substitutions for Kafka Connect WorkerConfig to handle cluster ID lookup in native mode.
 *
 * The cluster ID is retrieved via AdminClient.describeCluster() which has complex requirements
 * for native compilation (futures, network clients, metadata managers, etc.). Since the cluster
 * ID is only used for metrics context labels and is optional (can be null for old Kafka versions),
 * we substitute the kafkaClusterId() method to return null in native mode.
 *
 * This avoids the need to register all AdminClient internal classes for reflection while
 * maintaining full functionality - the offset store works perfectly without the cluster ID.
 */
final class WorkerConfigSubstitutions {
}

@TargetClass(className = "org.apache.kafka.connect.runtime.WorkerConfig")
final class Target_org_apache_kafka_connect_runtime_WorkerConfig {

    /**
     * Substitute the kafkaClusterId() method to return null in native mode.
     * The cluster ID is only used for metrics context and is optional.
     *
     * Original method calls lookupKafkaClusterId() which creates an AdminClient
     * and calls describeCluster().clusterId().get() - this requires extensive reflection
     * registrations for futures, network internals, etc.
     */
    @Substitute
    public String kafkaClusterId() {
        // Return null - same as when Kafka version is too old to support cluster ID
        // The cluster ID is only used in ConnectUtils.addMetricsContextProperties() for labeling
        return null;
    }
}
