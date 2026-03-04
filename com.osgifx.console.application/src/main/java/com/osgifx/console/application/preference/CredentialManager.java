/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
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
package com.osgifx.console.application.preference;

import static com.osgifx.console.constants.FxConstants.WORKSPACE_PROPERTY;
import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

@Component(service = CredentialManager.class)
public final class CredentialManager {

    private static final String ALGORITHM       = "AES";
    private static final Path   CREDENTIAL_FILE = Paths.get(System.getProperty(WORKSPACE_PROPERTY), "credentials.json");

    @Reference
    private LoggerFactory factory;
    private FluentLogger  logger;
    private SecretKeySpec secretKey;

    @Activate
    void activate() {
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));
        try {
            final var keyStr = System.getProperty("user.name") + System.getProperty("os.name")
                    + System.getProperty("os.arch") + "osgifx";
            final var sha    = MessageDigest.getInstance("SHA-256");
            final var key    = Arrays.copyOf(sha.digest(keyStr.getBytes(StandardCharsets.UTF_8)), 16);
            secretKey = new SecretKeySpec(key, ALGORITHM);
            logger.atInfo().log("Successfully initialized Machine-Bound Encryption");
        } catch (final Exception e) {
            logger.atWarning().withException(e).log("Unexpected error initializing AES Key");
        }
    }

    public void savePassword(final String connectionId, final String password) {
        if (secretKey == null || connectionId == null || password == null) {
            return;
        }
        try {
            final var encrypted = encrypt(password);
            final var map       = getCredentialsMap();
            map.put(connectionId, encrypted);
            saveCredentialsMap(map);
        } catch (final Exception e) {
            logger.atWarning().withException(e).log("Failed to save password for connection '%s'", connectionId);
        }
    }

    public String getPassword(final String connectionId) {
        if (secretKey == null || connectionId == null) {
            return null;
        }
        try {
            final var map       = getCredentialsMap();
            final var encrypted = map.get(connectionId);
            if (encrypted == null) {
                return null;
            }
            return decrypt(encrypted);
        } catch (final Exception e) {
            logger.atWarning().withException(e).log("Failed to retrieve password for connection '%s'", connectionId);
            return null;
        }
    }

    public void removePassword(final String connectionId) {
        if (secretKey == null || connectionId == null) {
            return;
        }
        try {
            final var map = getCredentialsMap();
            if (map.remove(connectionId) != null) {
                saveCredentialsMap(map);
            }
        } catch (final Exception e) {
            logger.atWarning().withException(e).log("Failed to delete password for connection '%s'", connectionId);
        }
    }

    public void saveTrustStorePassword(final String connectionId, final String password) {
        savePassword(connectionId + "_tsp", password);
    }

    public String getTrustStorePassword(final String connectionId) {
        return getPassword(connectionId + "_tsp");
    }

    public void removeTrustStorePassword(final String connectionId) {
        removePassword(connectionId + "_tsp");
    }

    private String encrypt(final String strToEncrypt) throws Exception {
        final var cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(ENCRYPT_MODE, secretKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
    }

    private String decrypt(final String strToDecrypt) throws Exception {
        final var cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(DECRYPT_MODE, secretKey);
        return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)), StandardCharsets.UTF_8);
    }

    private Map<String, String> getCredentialsMap() {
        if (!Files.exists(CREDENTIAL_FILE)) {
            return new ConcurrentHashMap<>();
        }
        try (var reader = Files.newBufferedReader(CREDENTIAL_FILE, StandardCharsets.UTF_8)) {
            final var                 type = new TypeToken<ConcurrentHashMap<String, String>>() {
                                           }.getType();
            final Map<String, String> map  = createGSON().get().fromJson(reader, type);
            return map == null ? new ConcurrentHashMap<>() : map;
        } catch (final Exception e) {
            logger.atWarning().withException(e).log("Failed to read credentials file");
            return new ConcurrentHashMap<>();
        }
    }

    private void saveCredentialsMap(final Map<String, String> map) throws Exception {
        Files.createDirectories(CREDENTIAL_FILE.getParent());
        try (var writer = Files.newBufferedWriter(CREDENTIAL_FILE, StandardCharsets.UTF_8)) {
            createGSON().get().toJson(map, writer);
        }
    }

    private Supplier<Gson> createGSON() {
        return () -> new GsonBuilder().setPrettyPrinting().create();
    }
}
