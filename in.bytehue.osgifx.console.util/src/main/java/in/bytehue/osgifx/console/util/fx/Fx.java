package in.bytehue.osgifx.console.util.fx;

import static in.bytehue.osgifx.console.supervisor.Supervisor.CONNECTED_AGENT;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.scene.paint.Color.GREEN;
import static javafx.scene.paint.Color.TRANSPARENT;

import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.controlsfx.control.Notifications;
import org.controlsfx.control.StatusBar;
import org.controlsfx.glyphfont.Glyph;
import org.osgi.framework.BundleContext;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
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

    @SuppressWarnings("rawtypes")
    public static <S> void addContextMenuToCopyContent(final TableView<S> table) {
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        final MenuItem item = new MenuItem("Copy");
        item.setOnAction(event -> {

            final Set<Integer> rows = new TreeSet<>();
            for (final TablePosition tablePosition : table.getSelectionModel().getSelectedCells()) {
                rows.add(tablePosition.getRow());
            }
            final StringBuilder strb     = new StringBuilder();
            boolean             firstRow = true;
            for (final Integer row : rows) {
                if (!firstRow) {
                    strb.append('\n');
                }
                firstRow = false;
                boolean firstCol = true;
                for (final TableColumn<?, ?> column : table.getColumns()) {
                    if (!firstCol) {
                        strb.append('\t');
                    }
                    firstCol = false;
                    final Object cellData = column.getCellData(row);
                    strb.append(cellData == null ? "" : cellData.toString());
                }
            }
            final ClipboardContent content = new ClipboardContent();

            content.putString(strb.toString());
            Clipboard.getSystemClipboard().setContent(content);
        });
        final ContextMenu menu = new ContextMenu();
        menu.getItems().add(item);
        table.setContextMenu(menu);
    }

    public static <S> void addContextMenuToCopyContent(final ListView<S> list) {
        list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        final MenuItem item = new MenuItem("Copy");
        item.setOnAction(event -> {
            final StringBuilder strb = new StringBuilder();
            for (final int index : list.getSelectionModel().getSelectedIndices()) {
                final S content = list.getItems().get(index);
                strb.append(content == null ? "" : content.toString());
            }
            final ClipboardContent content = new ClipboardContent();

            content.putString(strb.toString());
            Clipboard.getSystemClipboard().setContent(content);
        });
        final ContextMenu menu = new ContextMenu();
        menu.getItems().add(item);
        list.setContextMenu(menu);
    }

    public static void initStatusBar(final BorderPane parent, final StatusBar statusBar) {
        final Glyph glyph = new Glyph("FontAwesome", "DESKTOP");
        glyph.useGradientEffect();
        glyph.useHoverEffect();

        final Button button = new Button("", glyph);
        button.setBackground(new Background(new BackgroundFill(TRANSPARENT, new CornerRadii(2), new Insets(4))));
        statusBar.getLeftItems().clear();
        statusBar.getLeftItems().add(button);
        statusBar.getLeftItems().add(new Separator(VERTICAL));

        final String property = System.getProperty(CONNECTED_AGENT);
        final String statusBarText;
        if (property != null) {
            glyph.color(GREEN);
            statusBarText = "Connected to " + property;
        } else {
            statusBarText = "Disconnected";
        }
        statusBar.setText(statusBarText);
        parent.setBottom(statusBar);
    }
}
