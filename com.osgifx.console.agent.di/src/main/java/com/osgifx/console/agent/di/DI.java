/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
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

import static java.util.stream.Collectors.toList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

public final class DI {

    public static class DiException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public DiException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public DiException(final String message) {
            super(message);
        }
    }

    /**
     * A checklist for all class types that were requested to get instances from.
     */
    private final Set<Class<?>> requestedClasses = new HashSet<>();

    /**
     * A checklist with all class types that were successfully instantiated.
     */
    private final Set<Class<?>> instantiableClasses = new HashSet<>();

    /**
     * A map with all classes that are marked as singleton and the actual singleton instance.
     */
    private final Map<Class<?>, Object> singletonInstances = new HashMap<>();

    /**
     * A set of classes that are marked to be treated as singleton even if they aren't annotated as singleton.
     */
    private final Set<Class<?>> singletonClasses = new HashSet<>();

    /**
     * This map stores the implementation type (value) that should be used for an interface type (key).
     */
    private final Map<Class<?>, Class<?>> interfaceMappings = new HashMap<>();

    /**
     * This map stores providers for given class types.
     */
    private final Map<Class<?>, Provider<?>> providers = new HashMap<>();

    /**
     * Get an instance of the given class type.
     *
     * @param requestedType the class type of which an instance is retrieved.
     * @param <T> the generic type of the class.
     * @return an instance of the given type.
     * @throws java.lang.IllegalArgumentException if there is a misconfiguration or a requested class can't be
     *             instantiated.
     */
    public <T> T getInstance(final Class<T> requestedType) {
        return getInstance(requestedType, null);
    }

    @SuppressWarnings("unchecked")
    private <T> T getInstance(final Class<T> requestedType, final Class<?> parent) {
        try {
            Class<?> type = requestedType;

            if (requestedType.isInterface()) {
                if (interfaceMappings.containsKey(requestedType)) {
                    // replace the interface type with the implementing class type.
                    type = interfaceMappings.get(requestedType);
                } else if (providers.containsKey(requestedType)) {
                    return getInstanceFromProvider(requestedType);
                } else {
                    throw new DiException(createErrorMessageStart(requestedType)
                            + "It is an interface and there was no implementation class mapping defined for this type. "
                            + "Please use the 'bindInterface' method of DI to define what implementing class should be used for a given interface.");
                }
            }

            if (isAbstractClass(requestedType)) {
                if (providers.containsKey(requestedType)) {
                    return getInstanceFromProvider(requestedType);
                }
                throw new DiException(createErrorMessageStart(requestedType)
                        + "It is an abstract class and there is no provider for this class available. "
                        + "Please define a provider with the `bindProvider` method for this abstract class type.");
            }

            // If a class was already requested before...
            if (requestedClasses.contains(type)) {
                // ... we should have been able to instantiate it in the past ...
                if (!instantiableClasses.contains(type)) {

                    // if not, this means a cyclic dependency and is an error
                    throw new DiException(createErrorMessageStart(type) + "A cyclic dependency was detected.");
                }
            } else {
                // if this class wasn't requested before we now add it to the checklist.
                requestedClasses.add(type);
            }

            // If we have an existing singleton instance for this type...
            if (singletonInstances.containsKey(type)) {
                // ... we immediately return it.
                return (T) singletonInstances.get(type);
            }

            // check if there is a provider available
            if (providers.containsKey(type)) {
                final Object instanceFromProvider = getInstanceFromProvider(type);
                markAsInstantiable(type);

                if (isSingleton(type)) {
                    singletonInstances.put(type, instanceFromProvider);
                }
                return (T) instanceFromProvider;
            }
            return (T) createNewInstance(type);
        } catch (final DiException rootCause) {
            final StringBuilder errorMessage = new StringBuilder("DI wasn't able to create your class hierarchy. ");
            if (parent != null) {
                errorMessage.append("\nCannot instantiate the class [").append(parent.getName()).append("]. ")
                        .append("At least one of the constructor parameters of type [").append(requestedType)
                        .append("] can't be instantiated. ");
            }
            errorMessage.append("See the root cause exception for a detailed explanation.");
            throw new IllegalStateException(errorMessage.toString(), rootCause);
        }
    }

    /**
     * Create a new instance of the given type.
     */
    private <T> T createNewInstance(final Class<T> type) {
        final Constructor<T> constructor = findConstructor(type);
        final Parameter[]    parameters  = constructor.getParameters();

        // recursively get all constructor arguments
        final List<Object> arguments = Arrays.stream(parameters).map(param -> {
            if (param.getType().equals(Provider.class)) {
                return getProviderArgument(param, type);
            }
            return getInstance(param.getType(), type);
        }).collect(toList());

        try {
            final T newInstance = constructor.newInstance(arguments.toArray());

            markAsInstantiable(type);

            // when the class is marked as singleton it's instance is now added to the singleton map
            if (isSingleton(type)) {
                singletonInstances.put(type, newInstance);
            }
            return newInstance;
        } catch (final Exception e) {
            throw new DiException(createErrorMessageStart(type) + "An Exception was thrown during the instantiation.",
                                  e);
        }
    }

    /**
     * This method is used to define what implementing class should be used for a given interface.
     * <p>
     * This way you can use interface types as dependencies in your classes and doesn't have to
     * depend on specific implementations.
     * <p>
     * But DI needs to know what implementing class should be used when an interface type is
     * defined as dependency.
     * <p>
     *
     * <strong>Hint:</strong> The second parameter has to be an actual implementing class of the interface.
     * It may not be an abstract class!
     *
     * <p>
     * Alternatively to this method you can:
     * <ul>
     * <li>
     * use the {@link #bindInstance(Class, Object)} method to define an instance of the interface that is used
     * </li>
     * <li>
     * use the {@link #bindProvider(Class, Provider)} method to define a provider for this interface.
     * </li>
     * </ul>
     *
     * @param interfaceType the class type of the interface.
     * @param implementationType the class type of the implementing class.
     * @param <T> the generic type of the interface.
     * @throws java.lang.IllegalArgumentException if the first parameter is <b>not</b> an interface or the second
     *             parameter <b>is</b> an interface or an abstract class.
     */
    public <T> void bindInterface(final Class<T> interfaceType, final Class<? extends T> implementationType) {
        if (!interfaceType.isInterface()) {
            throw new IllegalArgumentException("The given type is not an interface. Expecting the first argument to be an interface.");
        }
        if (implementationType.isInterface()) {
            throw new IllegalArgumentException("The given type is an interface. Expecting the second argument to not be an interface but an actual class");
        }
        if (isAbstractClass(implementationType)) {
            throw new IllegalArgumentException("The given type is an abstract class. Expecting the second argument to be an actual implementing class");
        }
        interfaceMappings.put(interfaceType, implementationType);
    }

    /**
     * This method is used to define a {@link jakarta.inject.Provider} for a given type.
     * <p>
     * The type can either be an interface or class type. This is a good way to integrate
     * third-party classes that aren't suitable for injection by default (i.e. have no public constructor...).
     * <p>
     * Another use-case is when you need to make some configuration for new instance before it is used for dependency
     * injection.
     * <p>
     *
     * Providers can be combined with {@link jakarta.inject.Singleton}'s.
     * When a type is marked as singleton (has the annotation {@link jakarta.inject.Singleton}) and there is a provider
     * defined for this type, then this provider will only be executed exactly one time when the type is requested the
     * first time.
     *
     * @param classType the type of the class for which the provider is used.
     * @param provider the provider that will be called to get an instance of the given type.
     * @param <T> the generic type of the class/interface.
     */
    public <T> void bindProvider(final Class<T> classType, final Provider<T> provider) {
        providers.put(classType, provider);
    }

    /**
     * This method is used to define an instance that is used every time the given
     * class type is requested.
     * <p>
     * This way the given instance is effectively a singleton.
     * <p>
     * This method can also be used to define instances for interfaces or abstract classes
     * that otherwise couldn't be instantiated without further configuration.
     *
     * @param classType the class type for that the instance will be bound.
     * @param instance the instance that will be bound.
     * @param <T> the generic type of the class.
     */
    public <T> void bindInstance(final Class<T> classType, final T instance) {
        bindProvider(classType, () -> instance);
    }

    /**
     * This method can be used to mark a class as singleton.
     * <p>
     * It is an alternative for situations when you can't use the {@link jakarta.inject.Singleton} annotation.
     * For example when you want a class from a third-party library to be a singleton.
     * <p>
     * It is not possible to mark interfaces as singleton.
     *
     * @param type the type that will be marked as singleton.
     */
    public void markAsSingleton(final Class<?> type) {
        if (type.isInterface()) {
            throw new IllegalArgumentException("The given type is an interface. Expecting the param to be an actual class");
        }
        singletonClasses.add(type);
    }

    /**
     * This helper method returns {@code true} only if the given
     * class type is an abstract class.
     *
     * @param type the class type to check
     * @return {@code true} if the given type is an abstract class, otherwise {@code false}
     */
    static boolean isAbstractClass(final Class<?> type) {
        return !type.isInterface() && Modifier.isAbstract(type.getModifiers());
    }

    /**
     * This method is used to create a {@link jakarta.inject.Provider} instance when such a provider
     * is declared as constructor parameter.
     *
     * @param param the parameter declared by the constructor
     * @param requestedType the type that was requested by the user. This is used to generate a proper error messages.
     * @return the created provider.
     */
    private Provider<?> getProviderArgument(final Parameter param, final Class<?> requestedType) {
        final Type type = param.getParameterizedType();
        if (type instanceof ParameterizedType) {
            final ParameterizedType typeParam    = (ParameterizedType) type;
            final Type              providerType = typeParam.getActualTypeArguments()[0];
            return () -> DI.this.getInstance((Class<?>) providerType);
        }
        throw new DiException(createErrorMessageStart(requestedType)
                + "There is a javax.inject.Provider without a type parameter declared as dependency. "
                + "When using javax.inject.Provider as dependency "
                + "you need to define a type parameter for this provider!");
    }

    /**
     * Mark the given type as instantiable.
     */
    private void markAsInstantiable(final Class<?> type) {
        if (!instantiableClasses.contains(type)) {
            instantiableClasses.add(type);
        }
    }

    /**
     * Check if the given class type is marked as singleton.
     */
    private boolean isSingleton(final Class<?> type) {
        return type.isAnnotationPresent(Singleton.class) || singletonClasses.contains(type);
    }

    /**
     * Get an instance of the given type from a provider. This method takes care for Exception handling when the
     * provider throws an exception.
     */
    @SuppressWarnings("unchecked")
    private <T> T getInstanceFromProvider(final Class<T> type) {
        try {
            final Provider<T> provider = (Provider<T>) providers.get(type);
            return provider.get();
        } catch (final Exception e) {
            throw new DiException(createErrorMessageStart(type) + "An Exception was thrown by the provider.", e);
        }

    }

    /**
     * Find out the constructor that will be used for instantiation.
     * <p>
     * If there is only one public constructor, it will be used.
     * <p>
     * If there are more then one public constructors, the one with an {@link jakarta.inject.Inject}
     * annotation is used.
     * <p>
     *
     * In all other cases an {@link java.lang.IllegalStateException} is thrown.
     *
     * @param type the class of which the constructor is searched for.
     * @param <T> the generic type of the class.
     * @return the constructor to use
     * @throws java.lang.IllegalStateException when no constructor can be found.
     */
    @SuppressWarnings("unchecked")
    private <T> Constructor<T> findConstructor(final Class<T> type) {
        final Constructor<?>[] constructors = type.getConstructors();
        if (constructors.length == 0) {
            throw new DiException(createErrorMessageStart(type) + "The class has no public constructor.");
        }
        if (constructors.length <= 1) {
            return (Constructor<T>) constructors[0];
        }
        final List<Constructor<?>> constructorsWithInject = Arrays.stream(constructors)
                .filter(c -> c.isAnnotationPresent(Inject.class)).collect(toList());

        if (constructorsWithInject.isEmpty()) {
            throw new DiException(createErrorMessageStart(type)
                    + "There is more than one public constructor defined so I don't know which one to use. "
                    + "Fix this by either make only one constructor public "
                    + "or annotate exactly one constructor with the javax.inject.Inject annotation.");
        }

        if (constructorsWithInject.size() != 1) {
            throw new DiException(createErrorMessageStart(type)
                    + "There is more than one public constructor marked with @Inject so I don't know which one to use. "
                    + "Fix this by either make only one constructor public "
                    + "or annotate exactly one constructor with the javax.inject.Inject annotation.");
        }
        // we are not modifying the constructor array so we can safely cast here.
        return (Constructor<T>) constructorsWithInject.get(0);
    }

    /**
     * We need this string for most error messages.
     */
    private String createErrorMessageStart(final Class<?> type) {
        return "DI can't create an instance of the class [" + type + "]. ";
    }

}