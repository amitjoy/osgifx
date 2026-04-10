package com.osgifx.console.agent.rsa.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointEvent;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;

@Component(immediate = true)
public class RsaTopologyMock {

    private EndpointDescription               exportDesc;
    private EndpointDescription               importDesc1;
    private EndpointDescription               importDesc2;
    private final List<EndpointEventListener> listeners = new CopyOnWriteArrayList<>();

    @Activate
    public void activate(BundleContext context) {
        String localUUID = context.getProperty("org.osgi.framework.uuid");
        if (localUUID == null) {
            localUUID = UUID.randomUUID().toString();
        }

        String remoteUUID1 = UUID.randomUUID().toString();
        String remoteUUID2 = UUID.randomUUID().toString();

        Map<String, Object> exportProps = new HashMap<>();
        exportProps.put("objectClass", new String[] { "com.osgifx.mock.PaymentService" });
        exportProps.put("endpoint.id", "uuid:" + UUID.randomUUID().toString());
        exportProps.put("endpoint.service.id", 101L);
        exportProps.put("endpoint.framework.uuid", localUUID);
        exportProps.put("service.imported.configs", "ecf.generic.server");
        exportProps.put("ecf.endpoint.id", "ecf://localhost:3282/payment");
        exportDesc = new EndpointDescription(exportProps);

        Map<String, Object> importProps1 = new HashMap<>();
        importProps1.put("objectClass", new String[] { "com.osgifx.mock.InventoryService" });
        importProps1.put("endpoint.id", "uuid:" + UUID.randomUUID().toString());
        importProps1.put("endpoint.service.id", 202L);
        importProps1.put("endpoint.framework.uuid", remoteUUID1);
        importProps1.put("service.imported.configs", "ecf.generic.client");
        importProps1.put("ecf.endpoint.id", "ecf://inventory-server:3282/inventory");
        importDesc1 = new EndpointDescription(importProps1);

        Map<String, Object> importProps2 = new HashMap<>();
        importProps2.put("objectClass", new String[] { "com.osgifx.mock.NotificationService" });
        importProps2.put("endpoint.id", "uuid:" + UUID.randomUUID().toString());
        importProps2.put("endpoint.service.id", 303L);
        importProps2.put("endpoint.framework.uuid", remoteUUID2);
        importProps2.put("service.imported.configs", "ecf.generic.client");
        importProps2.put("ecf.endpoint.id", "ecf://notification-server:3282/notify");
        importDesc2 = new EndpointDescription(importProps2);

        // Now that the descriptions are initialized, notify any listeners that bound early
        for (EndpointEventListener listener : listeners) {
            notifyListener(listener, EndpointEvent.ADDED);
        }
    }

    @Deactivate
    public void deactivate() {
        for (EndpointEventListener listener : listeners) {
            notifyListener(listener, EndpointEvent.REMOVED);
        }
        exportDesc  = null;
        importDesc1 = null;
        importDesc2 = null;
        listeners.clear();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void bindEndpointEventListener(EndpointEventListener listener) {
        listeners.add(listener);
        // If bind is called late (after activate), descriptions are not null, so notify immediately
        if (exportDesc != null) {
            notifyListener(listener, EndpointEvent.ADDED);
        }
    }

    public void unbindEndpointEventListener(EndpointEventListener listener) {
        if (exportDesc != null) {
            notifyListener(listener, EndpointEvent.REMOVED);
        }
        listeners.remove(listener);
    }

    private void notifyListener(EndpointEventListener listener, int eventType) {
        if (exportDesc != null) {
            listener.endpointChanged(new EndpointEvent(eventType, exportDesc), "(objectClass=*)");
        }
        if (importDesc1 != null) {
            listener.endpointChanged(new EndpointEvent(eventType, importDesc1), "(objectClass=*)");
        }
        if (importDesc2 != null) {
            listener.endpointChanged(new EndpointEvent(eventType, importDesc2), "(objectClass=*)");
        }
    }
}
