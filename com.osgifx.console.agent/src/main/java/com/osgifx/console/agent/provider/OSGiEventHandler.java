/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.osgifx.console.agent.dto.XEventDTO;
import com.osgifx.console.supervisor.Supervisor;

import aQute.lib.converter.Converter;
import aQute.lib.converter.TypeReference;

public class OSGiEventHandler implements EventHandler {

	private final Supervisor supervisor;

	public OSGiEventHandler(final Supervisor supervisor) {
		this.supervisor = supervisor;
	}

	@Override
	public void handleEvent(final Event event) {
		final XEventDTO dto = new XEventDTO();

		dto.received   = System.currentTimeMillis();
		dto.properties = initProperties(event);
		dto.topic      = event.getTopic();

		supervisor.onOSGiEvent(dto);
	}

	private Map<String, String> initProperties(final Event event) {
		final Map<String, String> properties = new HashMap<>();

		for (final String propertyName : event.getPropertyNames()) {
			final Object propertyValue = event.getProperty(propertyName);
			try {
				properties.put(propertyName, processElement(propertyValue));
			} catch (final Exception e) {
				// nothing to do
			}
		}
		return properties;
	}

	private String processElement(final Object propertyValue) throws Exception {
		if (propertyValue instanceof boolean[]) {
			final List<Boolean> arr = Converter.cnv(new TypeReference<List<Boolean>>() {
			}, propertyValue);
			return arr.toString();
		}
		if (propertyValue instanceof int[]) {
			final List<Integer> arr = Converter.cnv(new TypeReference<List<Integer>>() {
			}, propertyValue);
			return arr.toString();
		}
		if (propertyValue instanceof float[]) {
			final List<Float> arr = Converter.cnv(new TypeReference<List<Float>>() {
			}, propertyValue);
			return arr.toString();
		}
		if (propertyValue instanceof double[]) {
			final List<Double> arr = Converter.cnv(new TypeReference<List<Double>>() {
			}, propertyValue);
			return arr.toString();
		}
		if (propertyValue instanceof long[]) {
			final List<Long> arr = Converter.cnv(new TypeReference<List<Long>>() {
			}, propertyValue);
			return arr.toString();
		}
		if (propertyValue instanceof String[]) {
			final List<String> arr = Converter.cnv(new TypeReference<List<String>>() {
			}, propertyValue);
			return arr.toString();
		}
		if (propertyValue instanceof char[]) {
			final List<Character> arr = Converter.cnv(new TypeReference<List<Character>>() {
			}, propertyValue);
			return arr.toString();
		}
		return String.valueOf(propertyValue);
	}

}
