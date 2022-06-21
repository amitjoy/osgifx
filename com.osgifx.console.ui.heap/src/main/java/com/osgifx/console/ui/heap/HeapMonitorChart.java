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
package com.osgifx.console.ui.heap;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.osgi.framework.FrameworkUtil;

import com.osgifx.console.agent.dto.XHeapUsageDTO.XMemoryUsage;

import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;

public final class HeapMonitorChart extends BorderPane {

	private static final long   KB_CONVERSION          = 1000 * 1000L;
	private static final String MEMORY_USAGE_CHART_CSS = "css/heap.css";
	private static final int    Y_AXIS_TICK_COUNT      = 16;
	private static final long   MAX_MILLI              = 120_000L;

	private static final int         X_AXIS_TICK_UNIT = 10_000;
	private static DateTimeFormatter FORMATTER        = DateTimeFormatter.ofPattern("HH:mm:ss");

	private final long startCounter;
	private final long initialUpperBound;

	private NumberAxis                     xAxis;
	private final String                   title;
	private final AtomicLong               counter;
	private XYChart.Series<Number, Number> usageSeries;
	private XYChart.Series<Number, Number> maxMemorySeries;
	private final Supplier<XMemoryUsage>   memoryUsageSupplier;

	private final AtomicBoolean firstUpdateCall;
	private NumberAxis          yAxis;

	public HeapMonitorChart(final String title, final Supplier<XMemoryUsage> memoryUsageSupplier, final long startCounter) {
		this.title               = title;
		this.memoryUsageSupplier = memoryUsageSupplier;
		counter                  = new AtomicLong(startCounter);
		firstUpdateCall          = new AtomicBoolean(true);
		this.startCounter        = startCounter;
		initialUpperBound        = startCounter + MAX_MILLI;
		setCenter(createContent());
	}

	private Parent createContent() {
		final var memoryUsage = memoryUsageSupplier.get();
		final var used        = memoryUsage.used / KB_CONVERSION;
		final var max         = memoryUsage.max / KB_CONVERSION;

		xAxis = new NumberAxis(startCounter, initialUpperBound, X_AXIS_TICK_UNIT);

		final double tickSize = max / Y_AXIS_TICK_COUNT;
		final var    rounding = 10d;

		yAxis = new NumberAxis(0, Math.max(max, used), Math.round(tickSize / rounding) * rounding);

		final var chart             = new AreaChart<>(xAxis, yAxis);
		final var bundle            = FrameworkUtil.getBundle(getClass());
		final var stockLineChartCss = bundle.getResource(MEMORY_USAGE_CHART_CSS).toExternalForm();

		chart.getStylesheets().add(stockLineChartCss);
		chart.setCreateSymbols(false);
		chart.setAnimated(false);
		chart.setLegendVisible(true);

		chart.setTitle(title);
		xAxis.setLabel("Time");
		xAxis.setForceZeroInRange(false);
		xAxis.setTickLabelFormatter(new StringConverter<Number>() {
			@Override
			public String toString(final Number object) {
				final var millis = object.longValue();
				final var date   = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
				return date.format(FORMATTER);
			}

			@Override
			public Number fromString(final String string) {
				return null;
			}
		});

		yAxis.setLabel("Memory (MB)");
		yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis, "", null));

		chart.setLegendSide(Side.TOP);

		// add starting data
		maxMemorySeries = new XYChart.Series<>();
		maxMemorySeries.setName("Max");
		usageSeries = new XYChart.Series<>();
		usageSeries.setName("Used");

		chart.getData().add(maxMemorySeries);
		chart.getData().add(usageSeries);
		return chart;
	}

	private synchronized void updateMemoryUsage() {
		final var memoryUsage = memoryUsageSupplier.get();
		final var used        = memoryUsage.used / KB_CONVERSION;
		final var max         = memoryUsage.max / KB_CONVERSION;

		counter.set(System.currentTimeMillis());

		if (firstUpdateCall.get()) {
			xAxis.setLowerBound(counter.get());
			firstUpdateCall.set(false);
		}
		yAxis.setUpperBound(Math.max(used, max));

		final var usedHeapSizeList = usageSeries.getData();
		final var maxHeapSizeList  = maxMemorySeries.getData();

		usedHeapSizeList.add(new XYChart.Data<>(counter.get(), used));
		maxHeapSizeList.add(new XYChart.Data<>(counter.get(), max));

		// if we go over upper bound, delete old data, and change the bounds
		if (counter.get() > initialUpperBound) {
			final var numberNumberData = usedHeapSizeList.get(1);
			final var secondValue      = numberNumberData.getXValue();
			xAxis.setLowerBound(secondValue.doubleValue());
			xAxis.setUpperBound(counter.get());
			usedHeapSizeList.remove(0);
			maxHeapSizeList.remove(0);

		}
	}

	void update() {
		updateMemoryUsage();
	}
}
