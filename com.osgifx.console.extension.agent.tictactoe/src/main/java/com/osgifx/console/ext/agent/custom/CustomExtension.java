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
package com.osgifx.console.ext.agent.custom;

import org.osgi.service.component.annotations.Component;

import com.osgifx.console.agent.extension.AgentExtension;
import com.osgifx.console.agent.extension.AgentExtensionName;

@Component
@AgentExtensionName("my-agent-extension")
public final class CustomExtension implements AgentExtension<MyContextDTO, MyResultDTO> {

    @Override
    public MyResultDTO execute(final MyContextDTO context) {
        final String propName  = context.propName;
        final int    propValue = context.propValue;

        System.out.println(propName);

        final MyResultDTO result = new MyResultDTO();
        result.name = "custom extension result";
        if (propValue > 10) {
            result.intValue    = 20;
            result.doubleValue = 100.25d;
        } else {
            result.intValue    = 10;
            result.doubleValue = 0.00d;
        }
        return result;
    }

    @Override
    public Class<MyContextDTO> getContextType() {
        return MyContextDTO.class;
    }

    @Override
    public Class<MyResultDTO> getResultType() {
        return MyResultDTO.class;
    }

}
