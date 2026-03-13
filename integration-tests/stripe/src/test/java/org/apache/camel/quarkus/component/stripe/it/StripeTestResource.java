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

import java.util.Map;

import org.apache.camel.quarkus.test.wiremock.WireMockTestResourceLifecycleManager;

public class StripeTestResource extends WireMockTestResourceLifecycleManager {

    private static final String STRIPE_API_BASE_URL = "https://api.stripe.com";
    private static final String STRIPE_ENV_API_KEY = "STRIPE_API_KEY";

    @Override
    public Map<String, String> start() {
        Map<String, String> properties = super.start();
        String wiremockUrl = properties.get("wiremock.url");

        // Configure Stripe API URL - use WireMock when mocking is enabled
        if (wiremockUrl != null) {

            properties.put("camel.component.stripe.api-base", wiremockUrl);
        }

        // Set API key - use test key for mocks, or real key from environment
        String apiKey = envOrDefault(STRIPE_ENV_API_KEY, "sk_test_mock_key");
        properties.put("camel.component.stripe.api-key", apiKey);

        return properties;
    }

    @Override
    protected String getRecordTargetBaseUrl() {
        return STRIPE_API_BASE_URL;
    }

    @Override
    protected boolean isMockingEnabled() {
        return !envVarsPresent(STRIPE_ENV_API_KEY);
    }
}
