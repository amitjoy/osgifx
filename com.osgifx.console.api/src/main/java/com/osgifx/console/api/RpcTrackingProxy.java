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
 * <p>
 * <b>Usage Example:</b>
 * 
 * <pre>
 * Agent realAgent = supervisor.getAgent();
 * Agent trackedAgent = (Agent) Proxy.newProxyInstance(Agent.class.getClassLoader(), new Class&lt;?&gt;[] { Agent.class },
 *         new RpcTrackingProxy(realAgent, tracker));
 * </pre>
 *
 * <p>
 * <b>Test Delay Configuration:</b>
 * Set the framework property {@code osgifx.rpc.test.delay} to add an artificial delay
 * (in milliseconds) to all RPC calls. This is useful for testing UI behavior with slow
 * network connections or remote agents.
 * 
 * <pre>
 * -Dosgifx.rpc.test.delay=2000  // 2 second delay for all RPC calls
 * </pre>
 *
 * @since 11.0
 */
public class RpcTrackingProxy implements InvocationHandler {

    private static final String TEST_DELAY_PROPERTY = "osgifx.rpc.test.delay";
    private static final long   testDelay;

    static {
        // Read test delay from framework property once at class load
        long delay = 0;
        try {
            final String delayStr = System.getProperty(TEST_DELAY_PROPERTY);
            if (delayStr != null && !delayStr.trim().isEmpty()) {
                delay = Long.parseLong(delayStr.trim());
                if (delay < 0) {
                    delay = 0;
                }
            }
        } catch (NumberFormatException e) {
            // Invalid value, use 0
            delay = 0;
        }
        testDelay = delay;
    }

    private final Object             target;
    private final RpcProgressTracker tracker;

    /**
     * Creates a new RPC tracking proxy.
     *
     * @param target the real Agent implementation to wrap
     * @param tracker the progress tracker to report to
     */
    public RpcTrackingProxy(Object target, RpcProgressTracker tracker) {
        this.target  = target;
        this.tracker = tracker;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Don't track Object methods (toString, equals, hashCode, etc.)
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(target, args);
        }

        final String methodName = method.getName();
        final String trackerId  = tracker.startRpc(methodName, methodName);

        try {
            // Set indeterminate progress
            tracker.updateProgress(trackerId, -1.0);

            // Apply test delay if configured
            if (testDelay > 0) {
                Thread.sleep(testDelay);
            }

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
