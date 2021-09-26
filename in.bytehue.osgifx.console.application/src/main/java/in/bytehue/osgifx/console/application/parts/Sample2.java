package in.bytehue.osgifx.console.application.parts;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.fx.core.di.LocalInstance;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

public class Sample2 {

    @Inject
    @LocalInstance
    private FXMLLoader fxmlLoader;

    @PostConstruct
    public void postConstruct(final BorderPane parent) {
        final Button b = new Button("Click Me");
        parent.getChildren().add(b);
    }

}