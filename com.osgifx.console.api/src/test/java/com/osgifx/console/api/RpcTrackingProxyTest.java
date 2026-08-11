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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RpcTrackingProxyTest {

    interface TestTarget {
        String execute(String input);
        void failMethod();
    }

    @Mock
    private TestTarget target;

    @Mock
    private RpcProgressTracker tracker;

    private TestTarget proxy;

    @BeforeEach
    public void setUp() {
        proxy = (TestTarget) Proxy.newProxyInstance(
                TestTarget.class.getClassLoader(),
                new Class<?>[] { TestTarget.class },
                new RpcTrackingProxy(target, tracker)
        );
    }

    @Test
    public void startRpcCalledOnMethodInvoke() {
        when(tracker.startRpc("execute", "execute")).thenReturn("task-1");
        
        proxy.execute("test");
        
        verify(tracker).startRpc("execute", "execute");
        verify(tracker).updateProgress("task-1", -1.0);
    }

    @Test
    public void completeRpcCalledOnSuccess() {
        when(tracker.startRpc("execute", "execute")).thenReturn("task-1");
        
        proxy.execute("test");
        
        verify(tracker).completeRpc("task-1");
    }

    @Test
    public void failRpcCalledOnException() {
        when(tracker.startRpc("failMethod", "failMethod")).thenReturn("task-2");
        
        RuntimeException exception = new RuntimeException("Test Error");
        doThrow(exception).when(target).failMethod();

        assertThrows(RuntimeException.class, () -> proxy.failMethod());

        verify(tracker).failRpc("task-2", "Test Error");
    }

    @Test
    public void objectMethodsNotTracked() {
        proxy.toString();
        proxy.hashCode();
        
        verify(tracker, never()).startRpc(anyString(), anyString());
    }

    @Test
    public void resultIsPassedThrough() {
        when(tracker.startRpc("execute", "execute")).thenReturn("task-1");
        when(target.execute("test")).thenReturn("result");
        
        String result = proxy.execute("test");
        
        assertEquals("result", result);
    }
}
