/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
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
package com.osgifx.console.util.fx;

import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

public final class Services {

    private Services() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static <T> ServiceRegistration<T> register(final Class<T> type, final T instance, final Object... props) {
        final var ht = new Hashtable<String, Object>();
        for (var i = 0; i < props.length; i += 2) {
            final var key   = (String) props[i];
            Object    value = null;
            if (i + 1 < props.length) {
                value = props[i + 1];
            }
            ht.put(key, value);
        }
        return bundleContext().registerService(type, instance, ht);
    }

    private static BundleContext bundleContext() {
        return FrameworkUtil.getBundle(Services.class).getBundleContext();
    }

}
