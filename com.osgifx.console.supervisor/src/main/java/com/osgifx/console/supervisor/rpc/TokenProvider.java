/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.supervisor.rpc;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import com.google.gson.Gson;

public final class TokenProvider implements Supplier<String> {

    public static class TokenConfigDTO {
        public String authServerURL;
        public String clientId;
        public String clientSecret;
        public String audience;
        public String scope;
    }

    public static final class TokenDTO {
        public String access_token;
        public int    expires_in;
    }

    public record TokenInfo(String token, Instant validUntil) {
    }

    private static final int REDUCE_VALID_UNTIL_BY = 10;
    private static final int TOKEN_TIMEOUT         = 10;

    private final String            tokenConfig;
    private TokenInfo               cachedToken;
    private final Supplier<Instant> nowProvider = Instant::now;

    public TokenProvider(final String tokenConfig) {
        this.tokenConfig = requireNonNull(tokenConfig, "Token config cannot be null");
    }

    @Override
    public String get() {
        final var now = nowProvider.get();
        if (cachedToken == null || cachedToken.validUntil.isBefore(now)) {
            cachedToken = internalGetToken();
        }
        return cachedToken.token;
    }

    private TokenInfo internalGetToken() throws TokenProviderException {
        try {
            final var config   = parseJson(tokenConfig, TokenConfigDTO.class);
            final var endpoint = requireNonNull(config.authServerURL, "Endpoint cannot be null");
            final var now      = nowProvider.get();

            // @formatter:off
            final var client = HttpClient.newBuilder()
                                         .connectTimeout(Duration.ofSeconds(TOKEN_TIMEOUT))
                                         .build();

            final var request = HttpRequest.newBuilder()
                                           .uri(URI.create(endpoint))
                                           .timeout(Duration.ofSeconds(TOKEN_TIMEOUT))
                                           .header("Connection", "close")
                                           .header("Content-Type", "application/x-www-form-urlencoded")
                                           .header("Accept", "application/json")
                                           .POST(BodyPublishers.ofString(getPostData(config)))
                                           .build();
            // @formatter:on
            final HttpResponse<String> response   = client.send(request, BodyHandlers.ofString(UTF_8));
            final var                  token      = parseJson(response.body(), TokenDTO.class);
            final var                  validUntil = now.plusSeconds((long) token.expires_in - REDUCE_VALID_UNTIL_BY);

            return new TokenInfo(token.access_token, validUntil);
        } catch (IOException | RuntimeException e) {
            throw new TokenProviderException("Exception during oauth request", e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TokenProviderException("Exception during oauth request", e);
        }
    }

    private String getPostData(final TokenConfigDTO tokenConfig) {
        final var clientID     = requireNonNull(tokenConfig.clientId, "Client ID cannot be null");
        final var clientSecret = requireNonNull(tokenConfig.clientSecret, "Client secret cannot be null");
        final var scope        = requireNonNull(tokenConfig.scope, "Scope cannot be null");
        final var audience     = requireNonNull(tokenConfig.audience, "Audience cannot be null");

        final var result = new StringBuilder();
        result.append("grant_type=client_credentials");
        result.append("&client_id=");
        result.append(clientID);
        result.append("&client_secret=");
        result.append(clientSecret);
        result.append("&scope=");
        result.append(scope);
        result.append("&audience=");
        result.append(audience);

        return result.toString();
    }

    private <T> T parseJson(final String input, final Class<T> clazz) {
        return new Gson().fromJson(input, clazz);
    }

}
