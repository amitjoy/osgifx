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
package com.osgifx.console.ui.chaos.engine;

import java.time.Instant;

public final class ChaosEvent {

    public enum TargetType {
        BUNDLE,
        COMPONENT
    }

    public String     targetId;
    public String     targetName;
    public TargetType type;
    public Instant    disruptedAt;
    public Instant    scheduledRevertAt;
    public boolean    isReverted;

    public ChaosEvent(final String targetId,
                      final String targetName,
                      final TargetType type,
                      final Instant disruptedAt,
                      final Instant scheduledRevertAt) {
        this.targetId          = targetId;
        this.targetName        = targetName;
        this.type              = type;
        this.disruptedAt       = disruptedAt;
        this.scheduledRevertAt = scheduledRevertAt;
        this.isReverted        = false;
    }

}
