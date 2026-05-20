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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.test.support.debezium.AbstractDebeziumTest;
import org.apache.camel.quarkus.test.support.debezium.Type;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.*;

@QuarkusTest
@QuarkusTestResource(value = DebeziumPostgresTestResource.class, restrictToAnnotatedClass = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DebeziumPostgresTest extends AbstractDebeziumTest {
    private static Connection connection;

    public DebeziumPostgresTest() {
        super(Type.postgres);
    }

    @BeforeAll
    public static void setUp(Config config) {
        final String jdbcUrl = config.getValue(Type.postgres.getPropertyJdbc(), String.class);
        // Retry connection acquisition to handle transient container startup issues
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    try {
                        connection = DriverManager.getConnection(jdbcUrl);
                    } catch (SQLException e) {
                        throw new AssertionError("Failed to connect to database: " + e.getMessage(), e);
                    }
                });
    }

    @Test
    @Order(4)
    public void testAdditionalProperty() {
        //https://github.com/apache/camel-quarkus/issues/3488
        RestAssured.get(Type.postgres.getComponent() + "/getAdditionalProperties")
                .then()
                .statusCode(200)
                .body("'database.connectionTimeZone'", is("CET"))
                .body("'bootstrap.servers'", is(notNullValue()));
    }

    /**
     * Test that Debezium uses KafkaOffsetBackingStore for offset storage.
     * This verifies issue #8621 - testing Debezium with Kafka-based offset storage
     * instead of the default file-based storage.
     */
    @Test
    @Order(5)
    public void testKafkaOffsetBackingStore() {
        // Verify Kafka bootstrap servers are configured
        RestAssured.get(Type.postgres.getComponent() + "/kafkaBootstrapServers")
                .then()
                .statusCode(200)
                .body(not(equalTo("not-configured")))
                .body(containsString("PLAINTEXT://"));

        // Verify offset storage type is Kafka
        RestAssured.get(Type.postgres.getComponent() + "/offsetStorageType")
                .then()
                .statusCode(200)
                .body(equalTo("kafka"));
    }

    @AfterAll
    public static void cleanUp() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    @Override
    protected Connection getConnection() {
        return connection;
    }

}
