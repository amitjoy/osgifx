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
package com.osgifx.console.ui.gogo;

import java.util.List;

import org.osgi.service.component.annotations.Component;

import com.google.common.collect.Lists;

@Component(service = GogoConsoleHistory.class)
public final class GogoConsoleHistory {

    private final List<String> history = Lists.newCopyOnWriteArrayList();

    public void add(final String command) {
        if (history.size() == 20) {
            // evicting last element
            history.remove(history.size() - 1);
        }
        history.add(command);
    }

    public void clear() {
        history.clear();
    }

    public int size() {
        return history.size();
    }

    public String get(final int index) {
        if (history.isEmpty()) {
            return "";
        }
        return history.get(index);
    }

}
