/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.util.configuration;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.fx.core.ExceptionUtils;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import aQute.lib.converter.Converter;

/**
 * Provides a helper class to read and update simple configurations in a type
 * safe way. It is based on the idea of Mockito et. al. By calling a method on
 * the configuration proxy interface we keep the method until the next
 * invocation of the {@link #set(Object, Object)} method. In the
 * {@link #set(Object, Object)} method we can then assign the value to
 * properties after converting it to the return type of the method.
 * <p>
 * Method names are mangled according to the OSGi spec.
 */
public class ConfigHelper<T> {

    final T                   delegate;
    final Class<T>            type;
    final ConfigurationAdmin  cm;
    final Map<String, Object> properties = new HashMap<>();

    Method lastInvocation;
    String pid;

    /**
     * Create a Config Helper for simple configurations.
     *
     * @param type the type of the configuration interface
     * @param cm the Configuration Admin service
     */
    @SuppressWarnings("unchecked")
    public ConfigHelper(final Class<T> type, final ConfigurationAdmin cm) {
        this.type = type;
        this.cm   = cm;
        delegate  = (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, this::invoke);
    }

    /*
     * Invocation for a method on the configuration's interface proxy
     */
    private Object invoke(final Object proxy, final Method method, final Object... args) throws Exception {
        lastInvocation = method;

        final var name  = Converter.mangleMethodName(method.getName());
        var       value = properties.get(name);
        if (value == null) {
            value = method.getDefaultValue();
        }
        return Converter.cnv(method.getGenericReturnType(), value);
    }

    /**
     * Return the proxy. Very short name so we it is not in the way in the set
     * method.
     * <p>
     * The proxy methods will return the live value of the corresponding
     * property.
     *
     * @return the proxy
     */
    public T d() {
        return delegate;
    }

    /**
     * Set the value based on the last invocation. This method is supposed to be
     * used like:
     *
     * <pre>
     * ch.set(ch.d().port(), 10000);
     * </pre>
     *
     * The last invocation on the proxy is remembered and used in the set method
     * to get the name and return type of the property.
     *
     * @param older ignored
     * @param newer the value to set
     * @return this
     */
    public <X> ConfigHelper<T> set(final X older, final X newer) {
        assert lastInvocation != null : "Missing invocation of target interface";

        final var key = Converter.mangleMethodName(lastInvocation.getName());
        Object    value;
        try {
            value = Converter.cnv(lastInvocation.getGenericReturnType(), newer);
        } catch (final Exception e) {
            throw ExceptionUtils.wrap(e);
        }
        if (value instanceof Enum<?>) {
            value = ((Enum<?>) value).name();
        }
        properties.put(key, value);
        lastInvocation = null;
        return this;
    }

    /**
     * Get the properties
     *
     * @return the properties
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Read a configuration
     *
     * @param pid a non-null PID
     * @return the properties read or empty if did not exist
     */
    public Map<String, Object> read(final String pid) {
        requireNonNull(pid, "PID cannot be null");

        this.pid = pid;
        properties.clear();
        Configuration configuration;
        try {
            configuration = cm.getConfiguration(pid, "?");
        } catch (final IOException e) {
            throw ExceptionUtils.wrap(e);
        }
        final var dict = configuration.getProperties();
        if (dict != null) {
            for (final String key : Collections.list(dict.keys())) {
                properties.put(key, dict.get(key));
            }
        }
        return properties;
    }

    /**
     * Update the current configuration. This requires a {@link #read(String)}
     * to set the proper PID.
     *
     * @throws IOException
     */
    public void update() {
        requireNonNull(pid, "First read the PID before you update");
        Configuration configuration;
        try {
            configuration = cm.getConfiguration(pid, "?");
            configuration.update(FrameworkUtil.asDictionary(properties));
        } catch (final IOException e) {
            // highly unlikely to occur
        }
    }

    /**
     * Delete the current configuration. This requires a {@link #read(String)}
     * to set the proper PID.
     *
     * @throws IOException
     */
    public void delete() {
        requireNonNull(pid, "First read the PID before you delete");
        Configuration configuration;
        try {
            configuration = cm.getConfiguration(pid, "?");
            configuration.delete();
        } catch (final IOException e) {
            // highly unlikely to occur
        }
    }

    /**
     * Clear the properties
     */
    public void clear() {
        properties.clear();
    }
}