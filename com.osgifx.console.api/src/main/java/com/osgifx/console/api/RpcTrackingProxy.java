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
package com.osgifx.console.api;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Dynamic proxy that wraps Agent interface calls to automatically track RPC progress.
 * This enables transparent progress tracking without modifying controller code.
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * Agent realAgent = supervisor.getAgent();
 * Agent trackedAgent = (Agent) Proxy.newProxyInstance(
 *     Agent.class.getClassLoader(),
 *     new Class&lt;?&gt;[] { Agent.class },
 *     new RpcTrackingProxy(realAgent, tracker)
 * );
 * </pre>
 *
 * @since 11.0
 */
public class RpcTrackingProxy implements InvocationHandler {

    private final Object target;
    private final RpcProgressTracker tracker;

    /**
     * Creates a new RPC tracking proxy.
     *
     * @param target the real Agent implementation to wrap
     * @param tracker the progress tracker to report to
     */
    public RpcTrackingProxy(Object target, RpcProgressTracker tracker) {
        this.target = target;
        this.tracker = tracker;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Don't track Object methods (toString, equals, hashCode, etc.)
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(target, args);
        }

        final String methodName = method.getName();
        final String trackerId = tracker.startRpc(methodName, "Executing " + methodName + "()");

        try {
            // Set indeterminate progress
            tracker.updateProgress(trackerId, -1.0);

            // Execute the actual RPC call
            Object result = method.invoke(target, args);

            // Mark as complete
            tracker.completeRpc(trackerId);

            return result;
        } catch (Exception e) {
            // Mark as failed
            String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            tracker.failRpc(trackerId, errorMsg);
            throw e;
        }
    }
}
