package in.bytehue.osgifx.console.util.fx;

import java.io.IOException;
import java.net.URL;

import org.osgi.framework.BundleContext;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;

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

    public static void autoResizeColumns(final TableView<?> table) {
        // Set the right policy
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.getColumns().stream().forEach(column -> {
            // Minimal width = columnheader
            Text   t   = new Text(column.getText());
            double max = t.getLayoutBounds().getWidth();
            for (int i = 0; i < table.getItems().size(); i++) {
                // cell must not be empty
                if (column.getCellData(i) != null) {
                    t = new Text(column.getCellData(i).toString());
                    final double calcwidth = t.getLayoutBounds().getWidth();
                    // remember new max-width
                    if (calcwidth > max) {
                        max = calcwidth;
                    }
                }
            }
            // set the new max-widht with some extra space
            column.setPrefWidth(max + 10.0d);
        });
    }

}
