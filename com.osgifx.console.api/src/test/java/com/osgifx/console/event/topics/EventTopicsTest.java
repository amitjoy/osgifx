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
package com.osgifx.console.event.topics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class EventTopicsTest {

    private final Class<?>[] topicClasses = {
        BundleActionEventTopics.class,
        ComponentActionEventTopics.class,
        ConfigurationActionEventTopics.class,
        DataRetrievedEventTopics.class,
        LogReceiveEventTopics.class,
        DmtActionEventTopics.class,
        EventReceiveEventTopics.class,
        LoggerContextActionEventTopics.class,
        RoleActionEventTopics.class,
        TableFilterUpdateTopics.class
    };

    private List<String> extractConstantValues(Class<?> clazz) throws IllegalAccessException {
        List<String> values = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()) && field.getType() == String.class) {
                values.add((String) field.get(null));
            }
        }
        return values;
    }

    @Test
    public void bundleActionTopicsAreNonNull() throws Exception {
        for (String value : extractConstantValues(BundleActionEventTopics.class)) {
            assertNotNull(value);
        }
    }

    @Test
    public void componentActionTopicsAreNonNull() throws Exception {
        for (String value : extractConstantValues(ComponentActionEventTopics.class)) {
            assertNotNull(value);
        }
    }

    @Test
    public void configurationActionTopicsAreNonNull() throws Exception {
        for (String value : extractConstantValues(ConfigurationActionEventTopics.class)) {
            assertNotNull(value);
        }
    }

    @Test
    public void dataRetrievedTopicsAreNonNull() throws Exception {
        for (String value : extractConstantValues(DataRetrievedEventTopics.class)) {
            assertNotNull(value);
        }
    }

    @Test
    public void logTopicsAreNonNull() throws Exception {
        for (String value : extractConstantValues(LogReceiveEventTopics.class)) {
            assertNotNull(value);
        }
    }

    @Test
    public void allTopicsAreNonEmptyStrings() throws Exception {
        for (Class<?> clazz : topicClasses) {
            for (String value : extractConstantValues(clazz)) {
                assertFalse(value.trim().isEmpty(), "Empty constant found in " + clazz.getSimpleName());
            }
        }
    }

    @Test
    public void allTopicsAreUnique() throws Exception {
        Set<String> uniqueValues = new HashSet<>();
        for (Class<?> clazz : topicClasses) {
            for (String value : extractConstantValues(clazz)) {
                assertTrue(uniqueValues.add(value), "Duplicate constant value found: " + value);
            }
        }
    }
}
