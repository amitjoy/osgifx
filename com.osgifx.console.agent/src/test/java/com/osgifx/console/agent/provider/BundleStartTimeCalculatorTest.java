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
package com.osgifx.console.agent.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.osgi.framework.BundleEvent.STARTED;
import static org.osgi.framework.BundleEvent.STARTING;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;

import com.osgifx.console.agent.provider.BundleStartTimeCalculator.BundleStartDuration;

@ExtendWith(MockitoExtension.class)
public class BundleStartTimeCalculatorTest {

    @Mock
    private BundleContext mockContext;

    @Mock
    private Bundle mockOurBundle;

    @Mock
    private Bundle mockBundle;

    @Mock
    private Bundle mockBundle2;

    private BundleStartTimeCalculator calculator;

    private static final long OUR_BUNDLE_ID = 10L;

    @BeforeEach
    public void setUp() {
        when(mockContext.getBundle()).thenReturn(mockOurBundle);
        when(mockOurBundle.getBundleId()).thenReturn(OUR_BUNDLE_ID);

        calculator = new BundleStartTimeCalculator(mockContext);
    }

    @Test
    public void startingEventRecorded() {
        when(mockBundle.getBundleId()).thenReturn(1L);
        when(mockBundle.getSymbolicName()).thenReturn("com.test.bundle");

        BundleEvent event = new BundleEvent(STARTING, mockBundle);
        calculator.bundleChanged(event);

        Optional<BundleStartDuration> duration = calculator.getBundleStartDuration(1L);
        assertTrue(duration.isPresent());
        assertEquals("com.test.bundle", duration.get().getSymbolicName());
    }

    @Test
    public void startedEventComputesDuration() throws InterruptedException {
        when(mockBundle.getBundleId()).thenReturn(2L);
        when(mockBundle.getSymbolicName()).thenReturn("com.test.bundle2");

        calculator.bundleChanged(new BundleEvent(STARTING, mockBundle));
        
        Thread.sleep(5); // Ensure clock advances

        calculator.bundleChanged(new BundleEvent(STARTED, mockBundle));

        Optional<BundleStartDuration> duration = calculator.getBundleStartDuration(2L);
        assertTrue(duration.isPresent());
        assertTrue(duration.get().getStartedAfter().toMillis() > 0);
    }

    @Test
    public void systemBundleIgnored() {
        when(mockBundle.getBundleId()).thenReturn(0L); // System Bundle ID is 0
        
        calculator.bundleChanged(new BundleEvent(STARTING, mockBundle));

        List<BundleStartDuration> durations = calculator.getBundleStartDurations();
        assertTrue(durations.isEmpty());
    }

    @Test
    public void ourBundleIgnored() {
        when(mockBundle.getBundleId()).thenReturn(OUR_BUNDLE_ID);
        
        calculator.bundleChanged(new BundleEvent(STARTING, mockBundle));

        List<BundleStartDuration> durations = calculator.getBundleStartDurations();
        assertTrue(durations.isEmpty());
    }

    @Test
    public void getBundleStartDurationsReturnsEntries() {
        when(mockBundle.getBundleId()).thenReturn(1L);
        when(mockBundle.getSymbolicName()).thenReturn("bundle.1");
        
        when(mockBundle2.getBundleId()).thenReturn(2L);
        when(mockBundle2.getSymbolicName()).thenReturn("bundle.2");

        calculator.bundleChanged(new BundleEvent(STARTING, mockBundle));
        calculator.bundleChanged(new BundleEvent(STARTING, mockBundle2));

        List<BundleStartDuration> durations = calculator.getBundleStartDurations();
        assertEquals(2, durations.size());
    }

    @Test
    public void getBundleStartDurationByIdPresent() {
        when(mockBundle.getBundleId()).thenReturn(5L);
        when(mockBundle.getSymbolicName()).thenReturn("bundle.5");

        calculator.bundleChanged(new BundleEvent(STARTING, mockBundle));
        
        assertTrue(calculator.getBundleStartDuration(5L).isPresent());
    }

    @Test
    public void getBundleStartDurationMissingId() {
        assertFalse(calculator.getBundleStartDuration(99L).isPresent());
    }

    @Test
    public void onlyStartedWithoutStartingNotRecorded() {
        when(mockBundle.getBundleId()).thenReturn(7L);
        // We only send STARTED, skipping STARTING
        calculator.bundleChanged(new BundleEvent(STARTED, mockBundle));

        assertFalse(calculator.getBundleStartDuration(7L).isPresent());
    }

}
