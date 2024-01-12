/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
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
package com.osgifx.console.ui.gogo;

import org.osgi.service.component.annotations.Component;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Lists;

@Component(service = GogoConsoleHistory.class)
public final class GogoConsoleHistory {

    private final EvictingQueue<String> history = EvictingQueue.create(20);

    public synchronized void add(final String command) {
        history.add(command);
    }

    public synchronized void clear() {
        history.clear();
    }

    public synchronized int size() {
        return history.size();
    }

    public synchronized String get(final int index) {
        if (history.isEmpty()) {
            return "";
        }
        return Lists.newArrayList(history).get(index);
    }

}
