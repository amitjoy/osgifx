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

import java.io.File;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.management.MBeanServer;

import com.osgifx.console.agent.Agent;
import com.osgifx.console.agent.dto.XHeapUsageDTO;
import com.osgifx.console.agent.dto.XHeapUsageDTO.XGarbageCollectorMXBean;
import com.osgifx.console.agent.dto.XHeapUsageDTO.XMemoryPoolMXBean;
import com.osgifx.console.agent.dto.XHeapUsageDTO.XMemoryUsage;
import com.osgifx.console.agent.dto.XHeapdumpDTO;

public final class XHeapAdmin {

	private static final String    HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";
	private static volatile Object hotspotMBean;

	public static XHeapUsageDTO init() {
		final XHeapUsageDTO heapUsage = new XHeapUsageDTO();

		final MemoryMXBean                 memoryMBean      = ManagementFactory.getMemoryMXBean();
		final RuntimeMXBean                runtimeMBean     = ManagementFactory.getRuntimeMXBean();
		final List<GarbageCollectorMXBean> gcMBeans         = ManagementFactory.getGarbageCollectorMXBeans();
		final List<MemoryPoolMXBean>       memoryPoolMBeans = ManagementFactory.getMemoryPoolMXBeans();

		heapUsage.memoryUsage     = initMemoryUsageMBean(memoryMBean.getHeapMemoryUsage());
		heapUsage.uptime          = runtimeMBean.getUptime();
		heapUsage.gcBeans         = initGcMBeans(gcMBeans);
		heapUsage.memoryPoolBeans = initMemoryPoolMBeans(memoryPoolMBeans);

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

	public static XHeapdumpDTO heapdump() {
		final File   location = getLocation();
		final String fileName = "heapdump_fx_" + System.currentTimeMillis() + ".hprof";
		final File   heapdump = new File(location, fileName);

		initHotspotMBean();
		try {
			final Class<?> clazz = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
			final Method   m     = clazz.getMethod("dumpHeap", String.class, boolean.class);
			m.invoke(hotspotMBean, heapdump.getAbsolutePath(), true);
		} catch (final RuntimeException re) {
			throw re;
		} catch (final Exception exp) {
			throw new RuntimeException(exp);
		}
		final XHeapdumpDTO dto = new XHeapdumpDTO();

		dto.size     = heapdump.length();
		dto.location = heapdump.getAbsolutePath();

		return dto;
	}

	private static void initHotspotMBean() {
		if (hotspotMBean == null) {
			synchronized (XHeapAdmin.class) {
				if (hotspotMBean == null) {
					hotspotMBean = getHotspotMBean();
				}
			}
		}
	}

	private static Object getHotspotMBean() {
		try {
			final Class<?>    clazz  = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
			final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			return ManagementFactory.newPlatformMXBeanProxy(server, HOTSPOT_BEAN_NAME, clazz);
		} catch (final RuntimeException re) {
			throw re;
		} catch (final Exception exp) {
			throw new RuntimeException(exp);
		}
	}

	private static File getLocation() {
		final String externalLocation = System.getProperty(Agent.HEAPDUMP_LOCATION_KEY);
		if (externalLocation != null) {
			final File file = new File(externalLocation);
			file.mkdirs();
			return file;
		}
		final File file = new File("./heapdumps");
		file.mkdirs();
		return file;
	}

}
