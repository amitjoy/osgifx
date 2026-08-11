/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.di;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

class DITest {

    private DI di;

    @BeforeEach
    void setUp() {
        di = new DI();
    }

    @Test
    void getInstanceOfConcreteClass() {
        ConcreteClass instance = di.getInstance(ConcreteClass.class);
        assertNotNull(instance);
    }

    @Test
    void getInstanceReturnsSameInstanceForSingleton() {
        SingletonClass instance1 = di.getInstance(SingletonClass.class);
        SingletonClass instance2 = di.getInstance(SingletonClass.class);
        assertSame(instance1, instance2);
    }

    @Test
    void getInstanceReturnsDifferentInstancesForNonSingleton() {
        ConcreteClass instance1 = di.getInstance(ConcreteClass.class);
        ConcreteClass instance2 = di.getInstance(ConcreteClass.class);
        assertNotSame(instance1, instance2);
    }

    @Test
    void injectConstructorDependency() {
        ClassWithDependency instance = di.getInstance(ClassWithDependency.class);
        assertNotNull(instance);
        assertNotNull(instance.dependency);
    }

    @Test
    void bindInterfaceToImplementation() {
        di.bindInterface(MyInterface.class, MyInterfaceImpl.class);
        MyInterface instance = di.getInstance(MyInterface.class);
        assertNotNull(instance);
        assertEquals(MyInterfaceImpl.class, instance.getClass());
    }

    @Test
    void bindProviderForInterface() {
        di.bindProvider(MyInterface.class, () -> new MyInterfaceImpl());
        MyInterface instance = di.getInstance(MyInterface.class);
        assertNotNull(instance);
        assertEquals(MyInterfaceImpl.class, instance.getClass());
    }

    @Test
    void bindSingletonClass() {
        di.markAsSingleton(ConcreteClass.class);
        ConcreteClass instance1 = di.getInstance(ConcreteClass.class);
        ConcreteClass instance2 = di.getInstance(ConcreteClass.class);
        assertSame(instance1, instance2);
    }

    @Test
    void cyclicDependencyThrowsException() {
        assertThrows(IllegalStateException.class, () -> di.getInstance(CyclicA.class));
    }

    @Test
    void missingInterfaceMappingThrowsException() {
        assertThrows(IllegalStateException.class, () -> di.getInstance(MyInterface.class));
    }

    @Test
    void abstractClassWithoutProviderThrows() {
        assertThrows(IllegalStateException.class, () -> di.getInstance(MyAbstractClass.class));
    }

    @Test
    void providerForConcreteClassIsRespected() {
        ConcreteClass providedInstance = new ConcreteClass();
        di.bindProvider(ConcreteClass.class, () -> providedInstance);
        ConcreteClass instance = di.getInstance(ConcreteClass.class);
        assertSame(providedInstance, instance);
    }

    @Test
    void providerParameterInjected() {
        ClassWithProviderDependency instance = di.getInstance(ClassWithProviderDependency.class);
        assertNotNull(instance);
        assertNotNull(instance.provider);
        assertNotNull(instance.provider.get());
    }

    // --- Dummy classes and interfaces for testing ---

    public static class ConcreteClass {
        public ConcreteClass() {
        }
    }

    @Singleton
    public static class SingletonClass {
        public SingletonClass() {
        }
    }

    public static class ClassWithDependency {
        public final ConcreteClass dependency;

        @Inject
        public ClassWithDependency(ConcreteClass dependency) {
            this.dependency = dependency;
        }
    }

    public interface MyInterface {
    }

    public static class MyInterfaceImpl implements MyInterface {
        public MyInterfaceImpl() {
        }
    }

    public static class CyclicA {
        @Inject
        public CyclicA(CyclicB b) {
        }
    }

    public static class CyclicB {
        @Inject
        public CyclicB(CyclicA a) {
        }
    }

    public static abstract class MyAbstractClass {
    }

    public static class ClassWithProviderDependency {
        public final Provider<ConcreteClass> provider;

        @Inject
        public ClassWithProviderDependency(Provider<ConcreteClass> provider) {
            this.provider = provider;
        }
    }
}
