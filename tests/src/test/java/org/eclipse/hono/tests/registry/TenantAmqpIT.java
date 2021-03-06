/**
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 1.0 which is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * SPDX-License-Identifier: EPL-1.0
 */

package org.eclipse.hono.tests.registry;

import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;

import org.eclipse.hono.client.HonoClient;
import org.eclipse.hono.client.ServiceInvocationException;
import org.eclipse.hono.client.TenantClient;
import org.eclipse.hono.util.Constants;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.proton.ProtonClientOptions;

/**
 * Tests verifying the behavior of the Device Registry component's Tenant AMQP endpoint.
 */
@RunWith(VertxUnitRunner.class)
public class TenantAmqpIT {

    private static final String TEST_CONFIGURED_BUT_NOT_CREATED_TENANT = "NOT_CREATED_TENANT";
    private static final String TEST_NOT_CONFIGURED_TENANT = "NOT_CONFIGURED_TENANT";

    private static final Vertx vertx = Vertx.vertx();

    private static HonoClient client;
    private static TenantClient tenantClient;

    /**
     * Global timeout for all test cases.
     */
    @Rule
    public Timeout globalTimeout = new Timeout(5, TimeUnit.SECONDS);

    /**
     * Starts the device registry, connects a client and provides a tenant API client.
     * 
     * @param ctx The vert.x test context.
     */
    @BeforeClass
    public static void prepareDeviceRegistry(final TestContext ctx) {

        client = DeviceRegistryAmqpTestSupport.prepareDeviceRegistryClient(vertx);

        client.connect(new ProtonClientOptions())
            .compose(c -> c.getOrCreateTenantClient())
            .setHandler(ctx.asyncAssertSuccess(r -> {
                tenantClient = r;
            }));
    }

    /**
     * Shuts down the device registry and closes the client.
     * 
     * @param ctx The vert.x test context.
     */
    @AfterClass
    public static void shutdown(final TestContext ctx) {

        DeviceRegistryAmqpTestSupport.shutdownDeviceRegistryClient(ctx, vertx, client);

    }

    /**
     * Verify that the details of the tenant configured for the user (that was used during client creation) can be retrieved.
     * 
     * @param ctx The vert.x test context.
     */
    @Test
    public void testGetTenant(final TestContext ctx) {

        tenantClient
            .get(Constants.DEFAULT_TENANT)
            .setHandler(ctx.asyncAssertSuccess(tenantObject -> {
                ctx.assertEquals(Constants.DEFAULT_TENANT, tenantObject.getTenantId());
                ctx.assertTrue(tenantObject.isEnabled());
            }));
    }

    /**
     * Verify that the details of a tenant that is NOT configured for the user (that was used during client creation)
     * are not retrievable.
     *
     * @param ctx The vert.x test context.
     */
    @Test
    public void testGetNotConfiguredTenantReturnsUnauthorized(final TestContext ctx) {

        tenantClient
                .get(TEST_NOT_CONFIGURED_TENANT)
                .setHandler(ctx.asyncAssertFailure(t -> {
                    ctx.assertEquals(
                            HttpURLConnection.HTTP_FORBIDDEN,
                            ((ServiceInvocationException) t).getErrorCode());
                }));
    }

    /**
     * Verify that retrieving the details of a tenant that is configured for the user (that was used during client creation)
     * but that was not created returns {@link HttpURLConnection#HTTP_NOT_FOUND}.
     *
     * @param ctx The vert.x test context.
     */
    @Test
    public void testGetConfiguredButNotCreatedTenantReturnsNotFound(final TestContext ctx) {

        tenantClient
                .get(TEST_CONFIGURED_BUT_NOT_CREATED_TENANT)
                .setHandler(ctx.asyncAssertFailure(t -> {
                    ctx.assertEquals(
                            HttpURLConnection.HTTP_NOT_FOUND,
                            ((ServiceInvocationException) t).getErrorCode());
                }));
    }
}
