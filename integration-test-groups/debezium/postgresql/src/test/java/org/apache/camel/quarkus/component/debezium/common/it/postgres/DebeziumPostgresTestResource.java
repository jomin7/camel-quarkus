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

package org.apache.camel.quarkus.component.debezium.common.it.postgres;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import io.strimzi.test.container.StrimziKafkaCluster;
import io.strimzi.test.container.StrimziKafkaContainer;
import org.apache.camel.quarkus.test.support.debezium.AbstractDebeziumTestResource;
import org.apache.camel.quarkus.test.support.debezium.Type;
import org.eclipse.microprofile.config.ConfigProvider;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class DebeziumPostgresTestResource extends AbstractDebeziumTestResource<PostgreSQLContainer> {

    public static final String DB_USERNAME = "postgres";
    public static final String DB_PASSWORD = "changeit";
    private static final int DB_PORT = 5432;

    private StrimziKafkaCluster kafkaCluster;
    private StrimziKafkaContainer kafkaContainer;

    public DebeziumPostgresTestResource() {
        super(Type.postgres);
    }

    @Override
    protected PostgreSQLContainer createContainer() {
        String postgresImage = ConfigProvider.getConfig().getValue("postgres-debezium.container.image", String.class);
        DockerImageName imageName = DockerImageName.parse(postgresImage)
                .asCompatibleSubstituteFor("postgres");
        return new PostgreSQLContainer(imageName)
                .withUsername(DB_USERNAME)
                .withPassword(DB_PASSWORD)
                .withDatabaseName(DebeziumPostgresResource.DB_NAME)
                .withInitScript("initPostgres.sql")
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)))
                .withStartupTimeout(Duration.ofMinutes(3));
    }

    @Override
    protected String getJdbcUrl() {
        return "jdbc:postgresql://" + container.getHost() + ":"
                + container.getMappedPort(DB_PORT) + "/" + DebeziumPostgresResource.DB_NAME + "?user="
                + DB_USERNAME + "&password=" + DB_PASSWORD;
    }

    @Override
    protected String getUsername() {
        return DB_USERNAME;
    }

    @Override
    protected String getPassword() {
        return DB_PASSWORD;
    }

    @Override
    protected int getPort() {
        return DB_PORT;
    }

    /**
     * Starts both Kafka and PostgreSQL containers for testing.
     * Kafka is used for KafkaOffsetBackingStore testing (issue #8621).
     * All PostgreSQL Debezium tests in this module will use Kafka-based offset storage.
     *
     * @return configuration map with Kafka bootstrap servers and PostgreSQL connection details
     */
    @Override
    public Map<String, String> start() {
        try {
            // Start single Kafka broker for KafkaOffsetBackingStore testing (issue #8621)
            // Using 1 broker instead of default 3 to minimize resource usage in CI grouped tests
            // Pattern follows KafkaTestResource from integration-tests-support/kafka
            String kafkaImage = ConfigProvider.getConfig().getValue("kafka.container.image", String.class);
            kafkaCluster = new StrimziKafkaCluster.StrimziKafkaClusterBuilder()
                    .withImage(kafkaImage)
                    .withNumberOfBrokers(1) // Single broker for minimal resource usage
                    .withContainerCustomizer(c -> c.withLogConsumer(frame -> System.out.print(frame.getUtf8String())))
                    .build();
            kafkaCluster.start();
            kafkaContainer = kafkaCluster.getBrokers().stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("No Kafka broker found in cluster"));

            // Start PostgreSQL container
            Map<String, String> config = new HashMap<>(super.start());

            // Add Kafka bootstrap servers to config for KafkaOffsetBackingStore
            String bootstrapServers = kafkaContainer.getBootstrapServers();
            config.put("kafka.bootstrap.servers", bootstrapServers);

            System.out.println("Kafka started successfully for offset storage: " + bootstrapServers);

            return config;
        } catch (Exception e) {
            throw new RuntimeException("Failed to start Kafka container for Debezium offset storage. " +
                    "This is required for testing issue #8621 (KafkaOffsetBackingStore support)", e);
        }
    }

    /**
     * Stop containers following the pattern from KafkaTestResource.
     * Stops PostgreSQL first (which stops the Debezium connector), then Kafka.
     * Uses try-catch to prevent stop failures from blocking shutdown.
     */
    @Override
    public void stop() {
        try {
            // Stop PostgreSQL first - this ensures Debezium connector stops writing to Kafka
            super.stop();
        } finally {
            try {
                if (kafkaCluster != null) {
                    kafkaCluster.stop();
                }
            } catch (Exception e) {
                // Ignore stop failures - container cleanup is best-effort
                // This pattern matches KafkaTestResource from integration-tests-support/kafka
            }
        }
    }
}
