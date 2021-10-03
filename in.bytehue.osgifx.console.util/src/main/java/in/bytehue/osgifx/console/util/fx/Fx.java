package in.bytehue.osgifx.console.util.fx;

import java.io.IOException;
import java.net.URL;

import org.osgi.framework.BundleContext;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

public final class Fx {

    private Fx() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static Node loadFXML(final FXMLLoader loader, final BundleContext bundleContext, final String resourceName) {
        final URL fxml = bundleContext.getBundle().getResource(resourceName);
        loader.setLocation(fxml);
        try {
            return loader.load();
        } catch (final IOException e) {
        }
        return null;
    }

}
