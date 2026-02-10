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
package com.osgifx.console.agent.admin;

import java.io.File;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.MBeanServer;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.dto.XHeapUsageDTO;
import com.osgifx.console.agent.dto.XHeapUsageDTO.XGarbageCollectorMXBean;
import com.osgifx.console.agent.dto.XHeapUsageDTO.XMemoryPoolMXBean;
import com.osgifx.console.agent.dto.XHeapUsageDTO.XMemoryUsage;

public final class XJmxAdmin {

    private static final String        HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";
    private static final ReentrantLock initLock          = new ReentrantLock();
    private static volatile Object     hotspotMBean;
    private final FluentLogger         logger            = LoggerFactory.getFluentLogger(getClass());

    public XHeapUsageDTO init() {
        final XHeapUsageDTO heapUsage = new XHeapUsageDTO();
        try {
            final MemoryMXBean                 memoryMBean      = ManagementFactory.getMemoryMXBean();
            final RuntimeMXBean                runtimeMBean     = ManagementFactory.getRuntimeMXBean();
            final List<GarbageCollectorMXBean> gcMBeans         = ManagementFactory.getGarbageCollectorMXBeans();
            final List<MemoryPoolMXBean>       memoryPoolMBeans = ManagementFactory.getMemoryPoolMXBeans();

            heapUsage.memoryUsage     = initMemoryUsageMBean(memoryMBean.getHeapMemoryUsage());
            heapUsage.uptime          = runtimeMBean.getUptime();
            heapUsage.gcBeans         = initGcMBeans(gcMBeans);
            heapUsage.memoryPoolBeans = initMemoryPoolMBeans(memoryPoolMBeans);
        } catch (final Exception e) {
            logger.atError().msg("Error occurred while retrieving heap usage").throwable(e).log();
        }
        return heapUsage;
    }

    private static XMemoryUsage initMemoryUsageMBean(final MemoryUsage memoryUsage) {
        final XMemoryUsage memUsage = new XMemoryUsage();

        memUsage.max  = memoryUsage.getMax();
        memUsage.used = memoryUsage.getUsed();

        return memUsage;
    }

    private static XMemoryPoolMXBean[] initMemoryPoolMBeans(final List<MemoryPoolMXBean> memoryPoolMBeans) {
        final Collection<XMemoryPoolMXBean> beans = new ArrayList<>();
        for (final MemoryPoolMXBean mpBean : memoryPoolMBeans) {
            final XMemoryPoolMXBean bean = new XMemoryPoolMXBean();
            bean.name        = mpBean.getName();
            bean.type        = mpBean.getType().name();
            bean.memoryUsage = initMemoryUsageMBean(mpBean.getUsage());
            beans.add(bean);
        }
        return beans.toArray(new XMemoryPoolMXBean[0]);
    }

    private static XGarbageCollectorMXBean[] initGcMBeans(final List<GarbageCollectorMXBean> gcMBeans) {
        final Collection<XGarbageCollectorMXBean> beans = new ArrayList<>();
        for (final GarbageCollectorMXBean gc : gcMBeans) {
            final XGarbageCollectorMXBean bean = new XGarbageCollectorMXBean();
            bean.name            = gc.getName();
            bean.collectionCount = gc.getCollectionCount();
            bean.collectionTime  = gc.getCollectionTime();
            beans.add(bean);
        }
        return beans.toArray(new XGarbageCollectorMXBean[0]);
    }

    public byte[] heapdump() throws Exception {
        final File location = new File(System.getProperty("user.dir"));
        final File heapdump = new File(location, "" + System.currentTimeMillis() + ".hprof");

        initHotspotMBean();
        try {
            final Class<?> clazz = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
            final Method   m     = clazz.getMethod("dumpHeap", String.class, boolean.class);
            m.invoke(hotspotMBean, heapdump.getAbsolutePath(), true);

            return Files.readAllBytes(heapdump.toPath());
        } finally {
            heapdump.delete();
        }
    }

    public void gc() throws Exception {
        initHotspotMBean();
        final Class<?> clazzBean     = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
        final Class<?> clazzVMOption = Class.forName("com.sun.management.VMOption");
        final Method   method1       = clazzBean.getMethod("getVMOption", String.class);
        final Method   method2       = clazzVMOption.getMethod("getValue");
        final Object   vmOption      = method1.invoke(hotspotMBean, "DisableExplicitGC");
        final Object   vmOptionValue = method2.invoke(vmOption);

        if (vmOptionValue != null && "true".equalsIgnoreCase(vmOptionValue.toString())) {
            logger.atInfo().msg("Explicit GC invocation is disabled").log();
            return;
        }
        System.gc();
    }

    private static void initHotspotMBean() throws Exception {
        if (hotspotMBean == null) {
            initLock.lock();
            try {
                if (hotspotMBean == null) {
                    hotspotMBean = getHotspotMBean();
                }
            } finally {
                initLock.unlock();
            }
        }
    }

    private static Object getHotspotMBean() throws Exception {
        final Class<?>    clazz  = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        return ManagementFactory.newPlatformMXBeanProxy(server, HOTSPOT_BEAN_NAME, clazz);
    }

}
