package in.bytehue.osgifx.console.lauchner;

import static java.util.Collections.emptyMap;
import static org.osgi.framework.Constants.SERVICE_PID;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;

import org.osgi.service.application.ApplicationDescriptor;
import org.osgi.service.application.ApplicationException;
import org.osgi.service.application.ApplicationHandle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import aQute.lib.exceptions.Exceptions;
import in.bytehue.osgifx.console.propertytypes.MainThread;

@Component
@MainThread
public final class Launcher implements Runnable {

    private static final String APPLICATION_ID = "in.bytehue.osgifx.console.application.osgifx";

    @Reference(target = "(" + SERVICE_PID + "=" + APPLICATION_ID + ")", cardinality = OPTIONAL)
    private volatile ApplicationDescriptor applicationDescriptor;

    @Override
    public void run() {
        try {
            if (applicationDescriptor != null) {
                final ApplicationHandle handle = applicationDescriptor.launch(emptyMap());
                handle.getExitValue(0);
            }
        } catch (final ApplicationException e) {
            Exceptions.duck(e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}