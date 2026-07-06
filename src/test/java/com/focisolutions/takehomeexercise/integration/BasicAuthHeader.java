package com.focisolutions.takehomeexercise.integration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Shared HTTP Basic Authorization header value for integration tests, matching the
 * {@code spring.security.user.name}/{@code password} pinned via {@code @TestPropertySource}
 * in each integration test class -- deliberately not the application's real default
 * credentials, so these tests are self-contained regardless of environment variables.
 */
final class BasicAuthHeader {

    static final String USERNAME = "testuser";
    static final String PASSWORD = "testpass";

    static final String VALUE = "Basic " + Base64.getEncoder()
            .encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));

    private BasicAuthHeader() {
    }
}
