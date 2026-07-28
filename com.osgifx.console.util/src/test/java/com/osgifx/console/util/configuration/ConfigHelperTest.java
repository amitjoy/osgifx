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
package com.osgifx.console.util.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

@ExtendWith(MockitoExtension.class)
public class ConfigHelperTest {

    interface TestConfig {
        int port();
        String host();
    }

    @Mock
    private ConfigurationAdmin cm;
    @Mock
    private Configuration configuration;

    private ConfigHelper<TestConfig> helper;

    @BeforeEach
    public void setUp() {
        helper = new ConfigHelper<>(TestConfig.class, cm);
    }

    @Test
    public void setAndGetProperties() {
        helper.set(helper.d().port(), 8080);
        helper.set(helper.d().host(), "localhost");

        assertEquals(8080, helper.getProperties().get("port"));
        assertEquals("localhost", helper.getProperties().get("host"));
    }

    @Test
    public void clearResetsProperties() {
        helper.set(helper.d().port(), 8080);
        helper.clear();

        assertTrue(helper.getProperties().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void readLoadsPropertiesFromConfiguration() throws Exception {
        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put("port", 9090);
        dict.put("host", "example.com");

        when(cm.getConfiguration("test.pid", "?")).thenReturn(configuration);
        when(configuration.getProperties()).thenReturn(dict);

        helper.read("test.pid");

        assertEquals(9090, helper.getProperties().get("port"));
        assertEquals("example.com", helper.getProperties().get("host"));
    }

    @Test
    public void readWithNullPidThrows() {
        assertThrows(NullPointerException.class, () -> helper.read(null));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void updateCallsConfigurationUpdate() throws Exception {
        when(cm.getConfiguration("test.pid", "?")).thenReturn(configuration);
        helper.read("test.pid");
        helper.set(helper.d().port(), 8080);
        helper.update();

        verify(configuration).update(any(Dictionary.class));
    }

    @Test
    public void updateWithoutReadThrows() {
        assertThrows(NullPointerException.class, () -> helper.update());
    }

    @Test
    public void deleteCallsConfigurationDelete() throws Exception {
        when(cm.getConfiguration("test.pid", "?")).thenReturn(configuration);
        helper.read("test.pid");
        helper.delete();

        verify(configuration).delete();
    }
}
