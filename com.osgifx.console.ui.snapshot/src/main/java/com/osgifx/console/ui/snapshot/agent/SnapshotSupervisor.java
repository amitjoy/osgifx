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
package com.osgifx.console.ui.snapshot.agent;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.SatisfyingConditionTarget;

import com.osgifx.console.agent.Agent;
import com.osgifx.console.agent.dto.XEventDTO;
import com.osgifx.console.agent.dto.XLogEntryDTO;
import com.osgifx.console.supervisor.EventListener;
import com.osgifx.console.supervisor.LogEntryListener;
import com.osgifx.console.supervisor.Supervisor;

@Component
@SatisfyingConditionTarget("snapshot-agent")
public final class SnapshotSupervisor implements Supervisor {

    @Override
    public boolean stdout(final String out) throws Exception {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean stderr(final String out) throws Exception {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void connect(final String host, final int port, final int timeout) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOSGiEvent(final XEventDTO event) {
        // TODO Auto-generated method stub

    }

    @Override
    public void logged(final XLogEntryDTO event) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addOSGiEventListener(final EventListener eventListener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeOSGiEventListener(final EventListener eventListener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addOSGiLogListener(final LogEntryListener logEntryListener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeOSGiLogListener(final LogEntryListener logEntryListener) {
        // TODO Auto-generated method stub

    }

    @Override
    public Agent getAgent() {
        // TODO Auto-generated method stub
        return null;
    }

}
