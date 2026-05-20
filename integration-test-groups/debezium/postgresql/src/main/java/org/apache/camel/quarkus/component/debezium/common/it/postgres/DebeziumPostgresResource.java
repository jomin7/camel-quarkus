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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.quarkus.test.support.debezium.AbstractDebeziumResource;
import org.apache.camel.quarkus.test.support.debezium.Type;
import org.eclipse.microprofile.config.Config;

@Path("/debezium-postgres")
@ApplicationScoped
public class DebeziumPostgresResource extends AbstractDebeziumResource {

    public static final String DB_NAME = "PostgresDB";

    @Inject
    Config config;

    public DebeziumPostgresResource() {
        super(Type.postgres);
    }

    /**
     * Gets the configured Kafka bootstrap servers for offset storage.
     *
     * @return the Kafka bootstrap servers or null if not configured
     */
    private String getKafkaBootstrapServers() {
        return config.getOptionalValue("kafka.bootstrap.servers", String.class).orElse(null);
    }

    @Path("/receiveEmptyMessages")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String receiveEmptyMessages() {
        return super.receiveEmptyMessages();
    }

    @Path("/receive")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String receive() {
        return super.receive();
    }

    /**
     * Endpoint to retrieve the configured Kafka bootstrap servers.
     * Used for testing that KafkaOffsetBackingStore is properly configured.
     *
     * @return the Kafka bootstrap servers or "not-configured" if not available
     */
    @Path("/kafkaBootstrapServers")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String kafkaBootstrapServers() {
        String servers = getKafkaBootstrapServers();
        return servers != null ? servers : "not-configured";
    }

    /**
     * Endpoint to determine which offset storage type is being used.
     * Returns "kafka" if KafkaOffsetBackingStore is configured, "file" otherwise.
     *
     * @return "kafka" or "file" depending on the offset storage configuration
     */
    @Path("/offsetStorageType")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String offsetStorageType() {
        return getKafkaBootstrapServers() != null ? "kafka" : "file";
    }

    /**
     * Constructs the Debezium endpoint URL with appropriate offset storage configuration.
     * Uses KafkaOffsetBackingStore if Kafka is available, otherwise falls back to file-based storage.
     *
     * @param  hostname              database hostname
     * @param  port                  database port
     * @param  username              database username
     * @param  password              database password
     * @param  databaseServerName    database server name
     * @param  offsetStorageFileName file path for file-based offset storage
     * @return                       the configured endpoint URL
     */
    @Override
    protected String getEndpointUrl(String hostname, String port, String username, String password, String databaseServerName,
            String offsetStorageFileName) {
        String kafkaBootstrapServers = getKafkaBootstrapServers();

        // Use KafkaOffsetBackingStore if Kafka is available (issue #8621)
        if (kafkaBootstrapServers != null) {
            System.out.println("Debezium PostgreSQL: Using KafkaOffsetBackingStore with servers: " + kafkaBootstrapServers);
            return Type.postgres.getComponent() + ":localhost?"
                    + "databaseHostname=" + hostname
                    + "&databasePort=" + port
                    + "&databaseUser=" + username
                    + "&databasePassword=" + password
                    + "&databaseDbname=" + DB_NAME
                    + "&topicPrefix=cq-testing"
                    // Kafka offset storage configuration
                    + "&offsetStorage=org.apache.kafka.connect.storage.KafkaOffsetBackingStore"
                    + "&offsetStorageTopic=debezium-offset-storage"
                    + "&offsetStoragePartitions=1" // Single partition sufficient for test
                    + "&offsetStorageReplicationFactor=1" // Single broker in test environment
                    + "&offsetFlushIntervalMs=1000" // Faster flush for test responsiveness
                    + "&additionalProperties.bootstrap.servers=" + kafkaBootstrapServers;
        } else {
            // Fallback to file-based storage
            System.out.println("Debezium PostgreSQL: Kafka not available, using file-based offset storage");
            return super.getEndpointUrl(hostname, port, username, password, databaseServerName, offsetStorageFileName)
                    + "&databaseDbname=" + DB_NAME;
        }
    }
}
