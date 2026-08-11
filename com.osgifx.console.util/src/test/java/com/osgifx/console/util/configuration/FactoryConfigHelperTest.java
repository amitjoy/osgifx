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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Dictionary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

@ExtendWith(MockitoExtension.class)
public class FactoryConfigHelperTest {

    interface TestConfig {
        int port();
        String host();
    }

    @Mock
    private ConfigurationAdmin cm;
    @Mock
    private Configuration configuration;

    private FactoryConfigHelper<TestConfig> helper;

    @BeforeEach
    public void setUp() {
        helper = new FactoryConfigHelper<>(TestConfig.class, cm, "test.factory.pid");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void createFactoryConfiguration() throws Exception {
        when(cm.createFactoryConfiguration("test.factory.pid", "?")).thenReturn(configuration);
        when(configuration.getPid()).thenReturn("test.pid.123");

        helper.create();

        verify(cm).createFactoryConfiguration("test.factory.pid", "?");
        verify(configuration).update(any(Dictionary.class));
    }

    @Test
    public void setAndGetProperties() {
        helper.set(helper.d().port(), 8080);
        helper.set(helper.d().host(), "localhost");

        assertEquals(8080, helper.getProperties().get("port"));
        assertEquals("localhost", helper.getProperties().get("host"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void updateCallsConfigurationUpdate() throws Exception {
        when(cm.createFactoryConfiguration("test.factory.pid", "?")).thenReturn(configuration);
        when(configuration.getPid()).thenReturn("test.pid.123");
        
        helper.create();
        helper.set(helper.d().port(), 8080);
        
        when(cm.getConfiguration("test.pid.123", "?")).thenReturn(configuration);
        helper.update();

        verify(configuration, org.mockito.Mockito.times(2)).update(any(Dictionary.class));
    }
}
