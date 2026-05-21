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

import java.util.HashMap;
import java.util.Map;

import io.strimzi.test.container.StrimziKafkaCluster;
import io.strimzi.test.container.StrimziKafkaContainer;
import org.apache.camel.quarkus.test.support.debezium.AbstractDebeziumTestResource;
import org.apache.camel.quarkus.test.support.debezium.Type;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class DebeziumPostgresTestResource extends AbstractDebeziumTestResource<PostgreSQLContainer> {

    private static final Logger LOG = Logger.getLogger(DebeziumPostgresTestResource.class);

    public static final String DB_USERNAME = "postgres";
    public static final String DB_PASSWORD = "changeit";
    private static final String POSTGRES_IMAGE = ConfigProvider.getConfig().getValue("postgres-debezium.container.image",
            String.class);
    private static final String KAFKA_IMAGE_NAME = ConfigProvider.getConfig().getValue("kafka.container.image",
            String.class);
    private static final int DB_PORT = 5432;

    private StrimziKafkaCluster kafkaCluster;
    private StrimziKafkaContainer kafkaContainer;

    public DebeziumPostgresTestResource() {
        super(Type.postgres);
    }

    @Override
    public Map<String, String> start() {
        try {
            LOG.info("Starting Kafka cluster");
            kafkaCluster = new StrimziKafkaCluster.StrimziKafkaClusterBuilder()
                    .withImage(KAFKA_IMAGE_NAME)
                    .build();
            kafkaCluster.start();
            kafkaContainer = kafkaCluster.getBrokers().stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("No Kafka broker available"));
            LOG.infof("Kafka cluster started with bootstrap servers: %s", kafkaContainer.getBootstrapServers());

            Map<String, String> config = new HashMap<>(super.start());
            config.put("kafka.bootstrap.servers", kafkaContainer.getBootstrapServers());
            return config;
        } catch (Exception e) {
            LOG.error("Failed to start containers", e);
            throw new RuntimeException("Error starting test containers", e);
        }
    }

    @Override
    protected PostgreSQLContainer createContainer() {
        DockerImageName imageName = DockerImageName.parse(POSTGRES_IMAGE)
                .asCompatibleSubstituteFor("postgres");
        return new PostgreSQLContainer(imageName)
                .withUsername(DB_USERNAME)
                .withPassword(DB_PASSWORD)
                .withDatabaseName(DebeziumPostgresResource.DB_NAME)
                .withInitScript("initPostgres.sql");
    }

    @Override
    public void stop() {
        try {
            super.stop();
        } catch (Exception e) {
            LOG.warn("Error stopping Postgres container", e);
        }

        try {
            if (kafkaCluster != null) {
                LOG.info("Stopping Kafka cluster");
                kafkaCluster.stop();
            }
        } catch (Exception e) {
            LOG.warn("Error stopping Kafka cluster", e);
        }
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
}
