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

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stripe.model.Charge;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.Refund;
import com.stripe.model.Subscription;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.stripe.StripeConstants;
import org.jboss.logging.Logger;

@Path("/stripe")
@ApplicationScoped
public class StripeResource {

    private static final Logger LOG = Logger.getLogger(StripeResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/customer")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCustomer(String email) {
        LOG.infof("Creating customer with email: %s", email);
        Map<String, Object> params = new HashMap<>();
        params.put("email", email);
        params.put("name", "Test Customer");

        Customer customer = producerTemplate.requestBody("stripe:customers", params, Customer.class);
        LOG.infof("Created customer: %s", customer.getId());

        return Response
                .created(URI.create("/stripe/customer/" + customer.getId()))
                .entity(customer.getId())
                .build();
    }

    @Path("/customer/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCustomer(@PathParam("id") String customerId) {
        LOG.infof("Retrieving customer: %s", customerId);

        Customer customer = producerTemplate.requestBodyAndHeader(
                "stripe:customers",
                null,
                StripeConstants.OBJECT_ID, customerId,
                Customer.class);

        return Response.ok(customer.getEmail()).build();
    }

    @Path("/customer/{id}")
    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateCustomer(@PathParam("id") String customerId, String description) {
        LOG.infof("Updating customer: %s with description: %s", customerId, description);

        Map<String, Object> params = new HashMap<>();
        params.put("description", description);

        Customer customer = producerTemplate.requestBodyAndHeaders(
                "stripe:customers",
                params,
                Map.of(
                        StripeConstants.OBJECT_ID, customerId,
                        StripeConstants.METHOD_HEADER, StripeConstants.METHOD_UPDATE),
                Customer.class);

        return Response.ok(customer.getDescription()).build();
    }

    @Path("/customer/{id}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteCustomer(@PathParam("id") String customerId) {
        LOG.infof("Deleting customer: %s", customerId);

        producerTemplate.sendBodyAndHeaders(
                "stripe:customers",
                null,
                Map.of(
                        StripeConstants.OBJECT_ID, customerId,
                        StripeConstants.METHOD_HEADER, StripeConstants.METHOD_DELETE));

        return Response.ok("deleted").build();
    }

    @Path("/product")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createProduct(String name) {
        LOG.infof("Creating product with name: %s", name);
        Map<String, Object> params = new HashMap<>();
        params.put("name", name);

        Product product = producerTemplate.requestBody("stripe:products", params, Product.class);
        LOG.infof("Created product: %s", product.getId());

        return Response
                .created(URI.create("/stripe/product/" + product.getId()))
                .entity(product.getId())
                .build();
    }

    @Path("/product/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProduct(@PathParam("id") String productId) {
        LOG.infof("Retrieving product: %s", productId);

        Product product = producerTemplate.requestBodyAndHeader(
                "stripe:products",
                null,
                StripeConstants.OBJECT_ID, productId,
                Product.class);

        return Response.ok(product.getName()).build();
    }

    @Path("/price")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPrice(@QueryParam("productId") String productId, String unitAmount) {
        LOG.infof("Creating price for product: %s with amount: %s", productId, unitAmount);
        Map<String, Object> params = new HashMap<>();
        params.put("product", productId);
        params.put("unit_amount", Long.parseLong(unitAmount));
        params.put("currency", "usd");

        Price price = producerTemplate.requestBody("stripe:prices", params, Price.class);
        LOG.infof("Created price: %s", price.getId());

        return Response
                .created(URI.create("/stripe/price/" + price.getId()))
                .entity(price.getId())
                .build();
    }

    @Path("/price/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPrice(@PathParam("id") String priceId) {
        LOG.infof("Retrieving price: %s", priceId);

        Price price = producerTemplate.requestBodyAndHeader(
                "stripe:prices",
                null,
                StripeConstants.OBJECT_ID, priceId,
                Price.class);

        return Response.ok(String.valueOf(price.getUnitAmount())).build();
    }

    @Path("/charge")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCharge(String amount) {
        LOG.infof("Creating charge with amount: %s", amount);
        Map<String, Object> params = new HashMap<>();
        params.put("amount", Long.parseLong(amount));
        params.put("currency", "usd");
        params.put("source", "tok_visa"); // Test token

        Charge charge = producerTemplate.requestBody("stripe:charges", params, Charge.class);
        LOG.infof("Created charge: %s", charge.getId());

        return Response
                .created(URI.create("/stripe/charge/" + charge.getId()))
                .entity(charge.getId())
                .build();
    }

    @Path("/charge/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCharge(@PathParam("id") String chargeId) {
        LOG.infof("Retrieving charge: %s", chargeId);

        Charge charge = producerTemplate.requestBodyAndHeader(
                "stripe:charges",
                null,
                StripeConstants.OBJECT_ID, chargeId,
                Charge.class);

        return Response.ok(String.valueOf(charge.getAmount())).build();
    }

    @Path("/paymentintent")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPaymentIntent(String amount) {
        LOG.infof("Creating payment intent with amount: %s", amount);
        Map<String, Object> params = new HashMap<>();
        params.put("amount", Long.parseLong(amount));
        params.put("currency", "usd");
        params.put("payment_method_types", List.of("card"));

        PaymentIntent paymentIntent = producerTemplate.requestBody("stripe:paymentintents", params, PaymentIntent.class);
        LOG.infof("Created payment intent: %s", paymentIntent.getId());

        return Response
                .created(URI.create("/stripe/paymentintent/" + paymentIntent.getId()))
                .entity(paymentIntent.getId())
                .build();
    }

    @Path("/paymentintent/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPaymentIntent(@PathParam("id") String paymentIntentId) {
        LOG.infof("Retrieving payment intent: %s", paymentIntentId);

        PaymentIntent paymentIntent = producerTemplate.requestBodyAndHeader(
                "stripe:paymentintents",
                null,
                StripeConstants.OBJECT_ID, paymentIntentId,
                PaymentIntent.class);

        return Response.ok(String.valueOf(paymentIntent.getAmount())).build();
    }

    @Path("/subscription")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSubscription(@QueryParam("customerId") String customerId, String priceId) {
        LOG.infof("Creating subscription for customer: %s with price: %s", customerId, priceId);
        Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);
        params.put("items", List.of(Map.of("price", priceId)));

        Subscription subscription = producerTemplate.requestBody("stripe:subscriptions", params, Subscription.class);
        LOG.infof("Created subscription: %s", subscription.getId());

        return Response
                .created(URI.create("/stripe/subscription/" + subscription.getId()))
                .entity(subscription.getId())
                .build();
    }

    @Path("/subscription/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSubscription(@PathParam("id") String subscriptionId) {
        LOG.infof("Retrieving subscription: %s", subscriptionId);

        Subscription subscription = producerTemplate.requestBodyAndHeader(
                "stripe:subscriptions",
                null,
                StripeConstants.OBJECT_ID, subscriptionId,
                Subscription.class);

        return Response.ok(subscription.getStatus()).build();
    }

    @Path("/refund")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createRefund(String chargeId) {
        LOG.infof("Creating refund for charge: %s", chargeId);
        Map<String, Object> params = new HashMap<>();
        params.put("charge", chargeId);

        Refund refund = producerTemplate.requestBody("stripe:refunds", params, Refund.class);
        LOG.infof("Created refund: %s", refund.getId());

        return Response
                .created(URI.create("/stripe/refund/" + refund.getId()))
                .entity(refund.getId())
                .build();
    }

    @Path("/refund/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRefund(@PathParam("id") String refundId) {
        LOG.infof("Retrieving refund: %s", refundId);

        Refund refund = producerTemplate.requestBodyAndHeader(
                "stripe:refunds",
                null,
                StripeConstants.OBJECT_ID, refundId,
                Refund.class);

        return Response.ok(String.valueOf(refund.getAmount())).build();
    }

    @Path("/invoice/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInvoice(@PathParam("id") String invoiceId) {
        LOG.infof("Retrieving invoice: %s", invoiceId);

        Invoice invoice = producerTemplate.requestBodyAndHeaders(
                "stripe:invoices",
                null,
                Map.of(
                        StripeConstants.OBJECT_ID, invoiceId,
                        StripeConstants.METHOD_HEADER, StripeConstants.METHOD_RETRIEVE),
                Invoice.class);

        return Response.ok(invoice.getStatus()).build();
    }
}
