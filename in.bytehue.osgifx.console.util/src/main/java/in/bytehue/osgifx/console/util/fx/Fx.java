package in.bytehue.osgifx.console.util.fx;

import java.io.IOException;
import java.net.URL;
import java.util.stream.Stream;

import org.controlsfx.control.Notifications;
import org.osgi.framework.BundleContext;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.util.Duration;

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

    public static <S, T> void sortBy(final TableView<S> table, final TableColumn<S, T> column) {
        column.setSortType(TableColumn.SortType.ASCENDING);
        table.getSortOrder().add(column);
        table.sort();
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

    public static void showSuccessNotification(final String title, final String text, final ClassLoader classloader) {
        final Image success = new Image(classloader.getResource("/graphic/images/success.png").toExternalForm());
        // @formatter:off
        final Notifications notification = //
                Notifications.create()
                             .title(title)
                             .graphic(new ImageView(success))
                             .text(text)
                             .hideAfter(Duration.seconds(7))
                             .position(Pos.CENTER);
        // @formatter:on
        notification.show();
    }

    public static void showErrorNotification(final String title, final String text, final ClassLoader classloader) {
        final Image success = new Image(classloader.getResource("/graphic/images/error.png").toExternalForm());
        // @formatter:off
        final Notifications notification = //
                Notifications.create()
                             .title(title)
                             .graphic(new ImageView(success))
                             .text(text)
                             .hideAfter(Duration.seconds(7))
                             .position(Pos.CENTER);
        // @formatter:on
        notification.show();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void disableSelectionModel(final TableView... tableViews) {
        Stream.of(tableViews).forEach(t -> t.setSelectionModel(new NullTableViewSelectionModel<>(t)));
    }

}
