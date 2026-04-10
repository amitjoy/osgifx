package com.osgifx.console.agent.admin;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointEvent;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;

import com.osgifx.console.agent.dto.RemoteServiceDirection;
import com.osgifx.console.agent.dto.XRemoteServiceDTO;
import com.osgifx.console.agent.rpc.codec.BinaryCodec;
import com.osgifx.console.agent.rpc.codec.SnapshotDecoder;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public final class RemoteServiceAdminManager extends AbstractSnapshotAdmin<XRemoteServiceDTO>
        implements EndpointEventListener {

    private final BundleContext                        context;
    private final Map<String, XRemoteServiceDTO>       cache = new ConcurrentHashMap<>();
    private ServiceRegistration<EndpointEventListener> registration;

    @Inject
    public RemoteServiceAdminManager(final BundleContext context,
                                     final BinaryCodec codec,
                                     final SnapshotDecoder decoder,
                                     final ScheduledExecutorService executor) {
        super(codec, decoder, executor);
        this.context = context;
    }

    public void init() {
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put(EndpointEventListener.ENDPOINT_LISTENER_SCOPE, "(objectClass=*)");
        registration = context.registerService(EndpointEventListener.class, this, props);
    }

    @Override
    public void stop() {
        super.stop();
        if (registration != null) {
            try {
                registration.unregister();
            } catch (final IllegalStateException e) {
                // ignore
            }
        }
    }

    @Override
    protected List<XRemoteServiceDTO> map() throws Exception {
        return new ArrayList<>(cache.values());
    }

    @Override
    public void endpointChanged(final EndpointEvent event, final String filter) {
        final EndpointDescription description = event.getEndpoint();
        final String              id          = description.getId();

        switch (event.getType()) {
            case EndpointEvent.ADDED:
            case EndpointEvent.MODIFIED:
                cache.put(id, toDTO(description));
                scheduleUpdate(pendingChangeCount.incrementAndGet());
                break;
            case EndpointEvent.REMOVED:
                cache.remove(id);
                scheduleUpdate(pendingChangeCount.incrementAndGet());
                break;
            default:
                break;
        }
    }

    private XRemoteServiceDTO toDTO(final EndpointDescription description) {
        final XRemoteServiceDTO dto = new XRemoteServiceDTO();

        dto.id            = description.getId();
        dto.objectClass   = description.getInterfaces();
        dto.frameworkUUID = description.getFrameworkUUID();

        // Find direction based on endpoint local/remote origin
        // ECF and Aries may have their own way, but usually if it's imported,
        // the endpoint description will have some imported property OR
        // frameworkUUID is different from local.
        final String localUUID = context.getProperty("org.osgi.framework.uuid");
        dto.direction = dto.frameworkUUID != null && dto.frameworkUUID.equals(localUUID) ? RemoteServiceDirection.EXPORT
                : RemoteServiceDirection.IMPORT;

        dto.properties = description.getProperties();

        // ECF specific property for provider. Aries uses something else.
        dto.provider = (String) dto.properties.get("endpoint.service.sender.id"); // ECF
        if (dto.provider == null) {
            dto.provider = (String) dto.properties.get("ecf.endpoint.id");
        }

        dto.intents        = description.getIntents();
        dto.localServiceId = description.getServiceId();

        return dto;
    }
}
