/**
 * Copyright (c) 2004-2011 QOS.ch
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.slf4j.simple;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.ILoggerFactory;

/**
 * An implementation of {@link ILoggerFactory} which always returns
 * {@link SimpleLogger} instances.
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public class SimpleLoggerFactory implements ILoggerFactory {

    private final ConcurrentMap<String, SimpleLogger> loggerMap;

    public SimpleLoggerFactory() {
        loggerMap = new ConcurrentHashMap<>();
        SimpleLogger.lazyInit();
    }

    /**
     * Return an appropriate {@link SimpleLogger} instance by name.
     */
    @Override
    public SimpleLogger getLogger(final String name) {
        final SimpleLogger simpleLogger = loggerMap.get(name);
        if (simpleLogger != null) {
            return simpleLogger;
        } else {
            final SimpleLogger newInstance = new SimpleLogger(name, getBundleId(name));
            final SimpleLogger oldInstance = loggerMap.putIfAbsent(name, newInstance);
            return oldInstance == null ? newInstance : oldInstance;
        }
    }

    /**
     * Clear the internal logger cache.
     *
     * This method is intended to be called by classes (in the same package) for
     * testing purposes. This method is internal. It can be modified, renamed or
     * removed at any time without notice.
     *
     * You are strongly discouraged from calling this method in production code.
     */
    void reset() {
        loggerMap.clear();
    }

    private long getBundleId(final String name) {
        final BundleContext context = getBundleContext();
        // context can be null if the bundle activator start method execution is not yet
        // finished. This can primarily happen during tests but not in the container as the
        // start level of the log forwarder bundle has been kept at top so that it is
        // started before all others. Otherwise when any other bundle using slf4j packages
        // is started before this bundle, we will not be able to retrieve the bundle ID to
        // add to the log messages.
        if (context == null) {
            return -1L;
        }
        final Bundle[] bundles = context.getBundles();
        // we assume that the logger name will always be the fully qualified class name (in QIVICON for sure)
        for (final Bundle b : bundles) {
            try {
                final BundleWiring wiring = b.adapt(BundleWiring.class);
                // wiring can be null for non-started bundles
                // actually for those whose Activator#start(..) ain't yet finished
                if (wiring == null) {
                    continue;
                }
                final ClassLoader cl = wiring.getClassLoader();
                // classloader can be null for fragments
                if (cl == null) {
                    continue;
                }
                // if the class can be loaded by the classloader, the class belongs to the bundle 'b'
                cl.loadClass(name);
                return b.getBundleId();
            } catch (final ClassNotFoundException e) {
                // nothing to do as the class cannot be loaded by this bundle 'b'
            }
        }
        return -1L;
    }

    private BundleContext getBundleContext() {
        final Bundle bundle = FrameworkUtil.getBundle(getClass());
        // bundle can only be null during tests
        if (bundle == null) {
            return null;
        }
        return bundle.getBundleContext();
    }

}