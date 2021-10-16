package in.bytehue.osgifx.console.application.fxml.controller;

import javax.inject.Inject;

import org.controlsfx.control.HyperlinkLabel;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

public final class AboutApplicationDialogController {

    @FXML
    private ImageView      header;
    @FXML
    private GridPane       content;
    @FXML
    private HyperlinkLabel hyperlink;
    @Inject
    private Application    jfxApplication;

    @FXML
    public void initialize() {
        hyperlink.setOnAction(event -> {
            final Hyperlink link           = (Hyperlink) event.getSource();
            final String    eclipseWebLink = link.getText();
            jfxApplication.getHostServices().showDocument(eclipseWebLink);
        });
    }

    public Node getHeader() {
        return header;
    }

    public Node getBody() {
        return content;
    }

}
