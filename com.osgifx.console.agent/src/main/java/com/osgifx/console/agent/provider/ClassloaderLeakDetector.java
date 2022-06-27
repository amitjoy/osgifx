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
package com.osgifx.console.agent.provider;

import static java.lang.Long.toHexString;
import static java.util.Objects.hash;
import static java.util.stream.Collectors.toSet;
import static org.osgi.framework.Bundle.ACTIVE;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.BundleTracker;

import com.osgifx.console.agent.admin.XBundleAdmin;
import com.osgifx.console.agent.dto.XBundleDTO;

/**
 * A support for tracing classloader leaks which occur due to improper cleanup
 * in bundles.
 *
 * <p>
 * This registers a Felix Configuration Printer which dumps out a list of
 * suspected classloaders which are not getting garbage collected. Apart from
 * that, consumer can also use {@link #reportWith(Consumer)} to report the
 * suspected classloaders when required.
 *
 * <p>
 * To detect if a classloader leak exists, the {@link #start(BundleContext)}
 * needs to be executed once and to shutdown the detection functionality, the
 * {@link #stop()} needs to be executed.
 *
 * <p>
 * <b>IMPLEMENTATION NOTES</b>
 *
 * <p>
 * This class relies on {@link System#identityHashCode(Object)} to create a
 * unique id for every classloader instances.
 * {@link System#identityHashCode(Object)} does not guarantee that it will not
 * generate the same number for different objects, but in practice the chance of
 * collision is rare.
 *
 * <p>
 * {@link ClassloaderLeakDetector} uses {@link PhantomReference}s to detect
 * leaks. {@link PhantomReference}s can be used to be notified when some object
 * got out of scope to do some cleanup of resources. Remember that the
 * {@link Object#finalize()} method is not guaranteed to be called at the end of
 * live of an object, so if you need to close files or free resources, you can
 * rely on {@link PhantomReference}s. Since these references don't have a link
 * to the actual object, a typical pattern is to derive your own Reference type
 * from Phantom and adding some info useful for the final free, for example
 * filename. Using {@link PhantomReference}s is better than overriding
 * {@link #finalize()} and works also in those cases where {@link #finalize()}
 * is not overridable. Since in {@link SoftReference}s and
 * {@link WeakReference}, the referent will be nullified before they are
 * enqueued in the {@link ReferenceQueue}, the garbage collector will finalize
 * these referent at some future time. On the contrary, for
 * {@link PhantomReference}s garbage collector will finish the finalization of
 * referent and then enqueue the reference, but it will not nullify the
 * referent.
 */
public final class ClassloaderLeakDetector implements Runnable {

    private final Set<Reference<?>>           refs        = ConcurrentHashMap.newKeySet();
    private final ReferenceQueue<ClassLoader> queue       = new ReferenceQueue<>();
    private final Map<Long, BundleInfo>       bundleInfos = new ConcurrentHashMap<>();

    private BundleContext                   context;
    private Thread                          referencePoller;
    private BundleTracker<Bundle>           bundleTracker;
    private final BundleStartTimeCalculator bundleStartTimeCalculator;

    public ClassloaderLeakDetector(final BundleStartTimeCalculator bundleStartTimeCalculator) {
        this.bundleStartTimeCalculator = bundleStartTimeCalculator;
    }

    public void start(final BundleContext context) {
        this.context = context;

        bundleTracker = new LeakDetectorBundleTracker(context);
        bundleTracker.open();

        referencePoller = new Thread(this, "classloader-leak-detector");
        referencePoller.setDaemon(true);
        referencePoller.start();
    }

    public void stop() {
        bundleTracker.close();
        referencePoller.interrupt();
    }

    private class LeakDetectorBundleTracker extends BundleTracker<Bundle> {

        public LeakDetectorBundleTracker(final BundleContext context) {
            // track only started bundles
            super(context, ACTIVE, null);
        }

        @Override
        public Bundle addingBundle(final Bundle bundle, final BundleEvent event) {
            final ClassLoader cl = classloader(bundle);
            // classloader would be null for fragments
            if (cl != null) {
                final BundleReference ref = new BundleReference(bundle, cl);
                refs.add(ref);

                // Note that a bundle can be started multiple times e.g. when refreshed
                // so we need to account for that also
                final BundleInfo bi = bundleInfos.computeIfAbsent(bundle.getBundleId(), id -> new BundleInfo(bundle));
                bi.incrementUsageCount(ref);
            }
            return bundle;
        }

        private ClassLoader classloader(final Bundle b) {
            final BundleWiring bw = b.adapt(BundleWiring.class);
            if (bw != null) {
                return bw.getClassLoader();
            }
            return null;
        }
    }

    // GC callback
    @Override
    public void run() {
        BundleReference ref;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                ref = (BundleReference) queue.remove();
                if (ref != null) {
                    removeBundle(ref);
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // drain out the queue
        while (queue.poll() != null) {
            // ensuring the unreachability via the phantom references
        }
    }

    private void removeBundle(final BundleReference ref) {
        final BundleInfo bi = bundleInfos.get(ref.bundleId);

        // bi cannot be null
        bi.decrementUsageCount(ref);
        refs.remove(ref);
    }

    public Set<XBundleDTO> getSuspiciousBundles() {
        final Set<Long>        activeBundleIds   = Stream.of(context.getBundles()).map(Bundle::getBundleId).collect(toSet());
        final List<BundleInfo> suspiciousBundles = new ArrayList<>(bundleInfos.values());
        // filter out ACTIVE bundles that have only one classloader created for them
        suspiciousBundles.removeIf(bi -> bi.hasSingleInstance() && activeBundleIds.contains(bi.bundleId));
        return suspiciousBundles.stream().map(this::toDTO).collect(Collectors.toSet());
    }

    private XBundleDTO toDTO(final BundleInfo bundleInfo) {
        final Bundle bundle = context.getBundle(bundleInfo.bundleId);
        return XBundleAdmin.toDTO(bundle, bundleStartTimeCalculator);
    }

    private static class BundleInfo {

        final long    bundleId;
        final Version version;
        final String  symbolicName;

        private final Set<ClassloaderInfo> classloaderInfos = ConcurrentHashMap.newKeySet();

        public BundleInfo(final Bundle b) {
            bundleId     = b.getBundleId();
            version      = b.getVersion();
            symbolicName = b.getSymbolicName();
        }

        public void incrementUsageCount(final BundleReference ref) {
            classloaderInfos.add(ref.classloaderInfo);
        }

        public void decrementUsageCount(final BundleReference ref) {
            classloaderInfos.remove(ref.classloaderInfo);
        }

        public boolean hasSingleInstance() {
            return classloaderInfos.size() == 1;
        }

        @Override
        public String toString() {
            return String.format("%s (%s) - Classloader Count [%s]", symbolicName, version, classloaderInfos.size());
        }
    }

    private static class ClassloaderInfo {

        final long creationTime;
        final long systemHashCode;

        private ClassloaderInfo(final ClassLoader cl) {
            creationTime   = System.currentTimeMillis();
            systemHashCode = System.identityHashCode(cl);
        }

        public String address() {
            return toHexString(systemHashCode);
        }

        public String creationDate() {
            final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
            return dateFormat.format(new Date(creationTime));
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final ClassloaderInfo that = (ClassloaderInfo) o;
            return systemHashCode == that.systemHashCode;
        }

        @Override
        public int hashCode() {
            return hash(systemHashCode);
        }

        @Override
        public String toString() {
            return String.format("Identity HashCode - %s, Creation Time %s", address(), creationDate());
        }
    }

    private class BundleReference extends PhantomReference<ClassLoader> {

        final long            bundleId;
        final ClassloaderInfo classloaderInfo;

        public BundleReference(final Bundle bundle, final ClassLoader classloader) {
            super(classloader, queue);
            bundleId        = bundle.getBundleId();
            classloaderInfo = new ClassloaderInfo(classloader);
        }
    }

}