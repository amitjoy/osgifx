package in.bytehue.osgifx.console.ui.threads;

import static in.bytehue.osgifx.console.supervisor.Supervisor.CONNECTED_AGENT;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.MaskerPane;
import org.controlsfx.control.StatusBar;
import org.controlsfx.glyphfont.Glyph;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.fx.core.di.LocalInstance;
import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.util.fx.Fx;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

public final class ThreadsFxUI {

    @Inject
    @Named("in.bytehue.osgifx.console.ui.threads")
    private BundleContext context;

    private final StatusBar  statusBar    = new StatusBar();
    private final MaskerPane progressPane = new MaskerPane();

    @PostConstruct
    public void postConstruct(final BorderPane parent, @LocalInstance final FXMLLoader loader) {
        createControls(parent, loader);
    }

    @Focus
    public void focus(final BorderPane parent, @LocalInstance final FXMLLoader loader) {
        createControls(parent, loader);
    }

    private void createControls(final BorderPane parent, final FXMLLoader loader) {
        final Task<?> task = new Task<Void>() {

            Node tabContent = null;

            @Override
            protected Void call() throws Exception {
                progressPane.setVisible(true);
                tabContent = Fx.loadFXML(loader, context, "/fxml/tab-content.fxml");
                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                parent.getChildren().clear();
                parent.setCenter(tabContent);
                initStatusBar(parent);
                progressPane.setVisible(false);
            }
        };
        parent.getChildren().clear();
        parent.setCenter(progressPane);
        initStatusBar(parent);

        final Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void initStatusBar(final BorderPane parent) {
        final Button button = new Button("", new Glyph("FontAwesome", "DESKTOP"));
        button.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, new CornerRadii(2), new Insets(4))));
        statusBar.getLeftItems().clear();
        statusBar.getLeftItems().add(button);
        statusBar.getLeftItems().add(new Separator(Orientation.VERTICAL));
        statusBar.setText("Connected to " + System.getProperty(CONNECTED_AGENT));
        parent.setBottom(statusBar);
    }

}