/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.gson.Gson;

public final class TokenProvider implements Supplier<String> {

    public static class TokenProviderException extends RuntimeException {

        private static final long serialVersionUID = 607308379133795790L;

        public TokenProviderException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public TokenProviderException(final String message) {
            super(message);
        }
    }

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

    private TokenInfo               cachedToken;
    private final TokenConfigDTO    tokenConfig;
    private final Supplier<Instant> nowProvider = Instant::now;

    public TokenProvider(final TokenConfigDTO tokenConfig) {
        checkNotNull(tokenConfig, "Token config cannot be null");
        checkNotNull(tokenConfig.authServerURL, "Endpoint cannot be null");
        checkNotNull(tokenConfig.clientId, "Client ID cannot be null");
        checkNotNull(tokenConfig.clientSecret, "Client secret cannot be null");
        checkNotNull(tokenConfig.scope, "Scope cannot be null");
        checkNotNull(tokenConfig.audience, "Audience cannot be null");

        this.tokenConfig = tokenConfig;
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
            final var now = nowProvider.get();
            // @formatter:off
            final var client = HttpClient.newBuilder()
                                         .connectTimeout(Duration.ofSeconds(TOKEN_TIMEOUT))
                                         .build();

            final var request = HttpRequest.newBuilder()
                                           .uri(URI.create(tokenConfig.authServerURL))
                                           .timeout(Duration.ofSeconds(TOKEN_TIMEOUT))
                                           .header("Connection", "close")
                                           .header("Content-Type", "application/x-www-form-urlencoded")
                                           .header("Accept", "application/json")
                                           .POST(BodyPublishers.ofString(getPostData(tokenConfig)))
                                           .build();
            // @formatter:on
            final HttpResponse<String> response   = client.send(request, BodyHandlers.ofString(UTF_8));
            final var                  token      = parseJson(response.body(), TokenDTO.class);
            final var                  validUntil = now.plusSeconds((long) token.expires_in - REDUCE_VALID_UNTIL_BY);

            return new TokenInfo(token.access_token, validUntil);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TokenProviderException("Exception during OAuth request", e);
        } catch (final Exception e) {
            throw new TokenProviderException("Exception during OAuth request", e);
        }
    }

    private String getPostData(final TokenConfigDTO tokenConfig) {
        final Map<String, String> elements = Maps.newLinkedHashMap();

        elements.put("grant_type", "client_credentials");
        elements.put("client_id", tokenConfig.clientId);
        elements.put("client_secret", tokenConfig.clientSecret);
        elements.put("scope", tokenConfig.scope);
        elements.put("audience", tokenConfig.audience);

        return Joiner.on("&").withKeyValueSeparator("=").join(elements);
    }

    public static <T> T parseJson(final String input, final Class<T> clazz) {
        return new Gson().fromJson(input, clazz);
    }

}
