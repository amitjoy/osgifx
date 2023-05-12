package com.osgifx.console.agent.provider.mqtt;

import java.util.Optional;
import java.util.function.Consumer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.util.tracker.ServiceTracker;

public final class OSGiMqttClient {

    private final BundleContext                                            bundleContext;
    private final ServiceTracker<MessagePublisher, MessagePublisher>       publisherTracker;
    private final ServiceTracker<MessageSubscription, MessageSubscription> subscriberTracker;

    public OSGiMqttClient(final BundleContext bundleContext, final Consumer<MessageSubscription> subscriberCallback) {
        this.bundleContext = bundleContext;
        publisherTracker   = new ServiceTracker<>(bundleContext, MessagePublisher.class, null);
        subscriberTracker  = new ServiceTracker<MessageSubscription, MessageSubscription>(bundleContext,
                                                                                          MessageSubscription.class,
                                                                                          null) {
                               @Override
                               public MessageSubscription addingService(final ServiceReference<MessageSubscription> reference) {
                                   final MessageSubscription service = super.addingService(reference);
                                   subscriberCallback.accept(service);
                                   return service;
                               }
                           };
    }

    public Optional<MessagePublisher> pub() {
        return Optional.ofNullable(publisherTracker.getService());
    }

    public Optional<MessageSubscription> sub() {
        return Optional.ofNullable(subscriberTracker.getService());
    }

    public Optional<MessageContextBuilder> msgCtx() {
        final ServiceReference<MessageContextBuilder> ref            = bundleContext
                .getServiceReference(MessageContextBuilder.class);
        final ServiceObjects<MessageContextBuilder>   serviceObjects = bundleContext.getServiceObjects(ref);
        return Optional.ofNullable(serviceObjects.getService());
    }

    public void open() {
        publisherTracker.open();
        subscriberTracker.open();
    }

    public void close() {
        publisherTracker.close();
        subscriberTracker.close();
    }
}