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
package com.osgifx.console.agent.reflect;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.processing.Processor;

public final class CompileOptions {

	final List<? extends Processor> processors;
	final List<String>              options;

	public CompileOptions() {
		this(Collections.emptyList(), Collections.emptyList());
	}

	private CompileOptions(final List<? extends Processor> processors, final List<String> options) {
		this.processors = processors;
		this.options    = options;
	}

	public final CompileOptions processors(final Processor... newProcessors) {
		return processors(Arrays.asList(newProcessors));
	}

	public CompileOptions processors(final List<? extends Processor> newProcessors) {
		return new CompileOptions(newProcessors, options);
	}

	public final CompileOptions options(final String... newOptions) {
		return options(Arrays.asList(newOptions));
	}

	public CompileOptions options(final List<String> newOptions) {
		return new CompileOptions(processors, newOptions);
	}
}
