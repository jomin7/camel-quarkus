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
package org.apache.camel.quarkus.component.stripe.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

/**
 * Camel Stripe component tests.
 *
 * By default, tests run against a WireMock mock server.
 * To test against the real Stripe API, set the STRIPE_API_KEY environment variable
 * with your test API key (sk_test_...).
 */
@QuarkusTest
@QuarkusTestResource(StripeTestResource.class)
class StripeTest {

    @Test
    void testCustomerOperations() {
        String email = "test@example.com";

        // Create customer
        String customerId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(email)
                .post("/stripe/customer")
                .then()
                .statusCode(201)
                .extract().asString();

        // Retrieve customer
        RestAssured.get("/stripe/customer/" + customerId)
                .then()
                .statusCode(200)
                .body(is(email));

        // Update customer
        String description = "Updated description";
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(description)
                .put("/stripe/customer/" + customerId)
                .then()
                .statusCode(200)
                .body(is(description));

        // Delete customer
        RestAssured.delete("/stripe/customer/" + customerId)
                .then()
                .statusCode(200)
                .body(is("deleted"));
    }

    @Test
    void testProductOperations() {
        String productName = "Test Product";

        // Create product
        String productId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(productName)
                .post("/stripe/product")
                .then()
                .statusCode(201)
                .extract().asString();

        // Retrieve product
        RestAssured.get("/stripe/product/" + productId)
                .then()
                .statusCode(200)
                .body(is(productName));
    }

    @Test
    void testPriceOperations() {
        // Create a product first
        String productName = "Test Product for Price";
        String productId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(productName)
                .post("/stripe/product")
                .then()
                .statusCode(201)
                .extract().asString();

        // Create price
        String unitAmount = "2000"; // $20.00
        String priceId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("productId", productId)
                .body(unitAmount)
                .post("/stripe/price")
                .then()
                .statusCode(201)
                .extract().asString();

        // Retrieve price
        RestAssured.get("/stripe/price/" + priceId)
                .then()
                .statusCode(200)
                .body(is(unitAmount));
    }

    @Test
    void testChargeOperations() {
        String amount = "1000"; // $10.00

        // Create charge
        String chargeId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(amount)
                .post("/stripe/charge")
                .then()
                .statusCode(201)
                .extract().asString();

        // Retrieve charge
        RestAssured.get("/stripe/charge/" + chargeId)
                .then()
                .statusCode(200)
                .body(is(amount));
    }

    @Test
    void testPaymentIntentOperations() {
        String amount = "1500"; // $15.00

        // Create payment intent
        String paymentIntentId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(amount)
                .post("/stripe/paymentintent")
                .then()
                .statusCode(201)
                .extract().asString();

        // Retrieve payment intent
        RestAssured.get("/stripe/paymentintent/" + paymentIntentId)
                .then()
                .statusCode(200)
                .body(is(amount));
    }

    @Test
    void testSubscriptionOperations() {
        // Create customer
        String email = "subscription-test@example.com";
        String customerId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(email)
                .post("/stripe/customer")
                .then()
                .statusCode(201)
                .extract().asString();

        // Create product
        String productName = "Subscription Product";
        String productId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(productName)
                .post("/stripe/product")
                .then()
                .statusCode(201)
                .extract().asString();

        // Create price
        String unitAmount = "999"; // $9.99
        String priceId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("productId", productId)
                .body(unitAmount)
                .post("/stripe/price")
                .then()
                .statusCode(201)
                .extract().asString();

        // Create subscription
        String subscriptionId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("customerId", customerId)
                .body(priceId)
                .post("/stripe/subscription")
                .then()
                .statusCode(201)
                .extract().asString();

        // Retrieve subscription
        RestAssured.get("/stripe/subscription/" + subscriptionId)
                .then()
                .statusCode(200);
    }

    @Test
    void testRefundOperations() {
        // Create charge first
        String amount = "2500"; // $25.00
        String chargeId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(amount)
                .post("/stripe/charge")
                .then()
                .statusCode(201)
                .extract().asString();

        // Create refund
        String refundId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(chargeId)
                .post("/stripe/refund")
                .then()
                .statusCode(201)
                .extract().asString();

        // Retrieve refund
        RestAssured.get("/stripe/refund/" + refundId)
                .then()
                .statusCode(200)
                .body(is(amount));
    }

    @Test
    void testInvoiceOperations() {
        // Test invoice retrieval - this ensures SSL/HTTPS works in native mode
        // since the mock server also uses HTTPS endpoints
        String invoiceId = "in_mock123456";

        RestAssured.get("/stripe/invoice/" + invoiceId)
                .then()
                .statusCode(200);
    }

}
