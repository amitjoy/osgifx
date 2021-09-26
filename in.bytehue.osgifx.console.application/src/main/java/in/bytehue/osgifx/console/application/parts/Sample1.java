package in.bytehue.osgifx.console.application.parts;

import javax.annotation.PostConstruct;

import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

public class Sample1 {

    @PostConstruct
    public void postConstruct(final BorderPane parent) {
        final Button b = new Button("Click Me Again");
        parent.getChildren().add(b);
    }

}