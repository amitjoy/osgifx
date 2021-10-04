package in.bytehue.osgifx.console.application.fxml.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javax.inject.Inject;

import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

public final class AddConnectionWindowController implements Initializable {

    private static final String ADD_CONNECTION_WINDOW_ID = "in.bytehue.osgifx.console.window.addconnections";

    @Inject
    private EModelService model;

    @Inject
    private MApplication application;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
    }

    @FXML
    public void handleClose(final ActionEvent event) {
        final MWindow addConnectionWindow = (MWindow) model.find(ADD_CONNECTION_WINDOW_ID, application);
        addConnectionWindow.setVisible(false);
        addConnectionWindow.setOnTop(false);
    }

    @FXML
    public void addConnection(final ActionEvent event) {
    }

}
