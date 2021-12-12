package in.bytehue.osgifx.console.application.handler;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.di.AboutToShow;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.service.prefs.BackingStoreException;

import in.bytehue.osgifx.console.supervisor.EventListener;
import in.bytehue.osgifx.console.supervisor.Supervisor;
import in.bytehue.osgifx.console.util.fx.Fx;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=in.bytehue.osgifx.console.supervisor.EventListener)")
public final class EventReceiveMenuContributionHandler {

    @Log
    @Inject
    private FluentLogger        logger;
    @Inject
    private Supervisor          supervisor;
    @Inject
    private EventListener       eventListener;
    @Inject
    private EModelService       modelService;
    @Inject
    @Preference(nodePath = "osgi.fx.event")
    private IEclipsePreferences preferences;

    @PostConstruct
    public void init() {
        final boolean currentState = getCurrentState();
        if (currentState) {
            supervisor.addOSGiEventListener(eventListener);
            logger.atInfo().throttleByCount(10).log("OSGi event listener has been added");
        } else {
            supervisor.removeOSGiEventListener(eventListener);
            logger.atInfo().throttleByCount(10).log("OSGi event listener has been removed");
        }
    }

    @AboutToShow
    public void aboutToShow(final List<MMenuElement> items, final MWindow window) {
        prepareMenu(items, getCurrentState());
    }

    @Execute
    public void execute(final MDirectMenuItem menuItem) throws BackingStoreException {
        final boolean accessibilityPhrase = Boolean.parseBoolean(menuItem.getAccessibilityPhrase());
        preferences.putBoolean("action", accessibilityPhrase);
        preferences.flush();

        if (accessibilityPhrase) {
            supervisor.addOSGiEventListener(eventListener);
            Fx.showSuccessNotification("Event Notification", "Events will now be received");
            logger.atInfo().log("OSGi events will now be received");
        } else {
            supervisor.removeOSGiEventListener(eventListener);
            Fx.showSuccessNotification("Event Notification", "Events will not be received anymore");
            logger.atInfo().log("OSGi events will not be received anymore");
        }
    }

    private boolean getCurrentState() {
        return preferences.getBoolean("action", false);
    }

    private void prepareMenu(final List<MMenuElement> items, final boolean value) {
        final MDirectMenuItem eventActionMenu;
        if (value) {
            eventActionMenu = createEventActionMenu(Type.STOP);
        } else {
            eventActionMenu = createEventActionMenu(Type.START);
        }
        items.add(eventActionMenu);
    }

    private MDirectMenuItem createEventActionMenu(final Type type) {
        String label;
        String icon;
        String accessibilityPhrase;
        if (type == Type.STOP) {
            label               = "Stop Receiving Events";
            icon                = "stop.png";
            accessibilityPhrase = "false";
        } else {
            label               = "Start Receiving Events";
            icon                = "start.png";
            accessibilityPhrase = "true";
        }
        final MDirectMenuItem dynamicItem = modelService.createModelElement(MDirectMenuItem.class);

        dynamicItem.setLabel(label);
        dynamicItem.setIconURI("platform:/plugin/in.bytehue.osgifx.console.application/graphic/icons/" + icon);
        dynamicItem.setAccessibilityPhrase(accessibilityPhrase);
        dynamicItem.setContributorURI("platform:/plugin/in.bytehue.osgifx.console.application");
        dynamicItem.setContributionURI(
                "bundleclass://in.bytehue.osgifx.console.application/in.bytehue.osgifx.console.application.handler.EventReceiveMenuContributionHandler");

        return dynamicItem;
    }

    private enum Type {
        START,
        STOP
    }

}