package in.bytehue.osgifx.console.application.fxml.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javax.inject.Inject;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.dialog.ExceptionDialog;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

import in.bytehue.osgifx.console.agent.ConsoleAgent;
import in.bytehue.osgifx.console.application.dto.ConnectionSettingDTO;
import in.bytehue.osgifx.console.supervisor.ConsoleSupervisor;
import in.bytehue.osgifx.console.util.fx.DTOCellValueFactory;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.StageStyle;

public final class ConnectionSettingsWindowController implements Initializable {

    private static final String CONNECTION_WINDOW_ID     = "in.bytehue.osgifx.console.window.connection";
    private static final String ADD_CONNECTION_WINDOW_ID = "in.bytehue.osgifx.console.window.addconnections";

    @Inject
    private EModelService model;

    @Inject
    private MApplication application;

    @Inject
    private ConsoleSupervisor supervisor;

    @FXML
    private Button connectButton;

    @FXML
    private Button addConnectionButton;

    @FXML
    private Button removeConnectionButton;

    @FXML
    private TableView<ConnectionSettingDTO> connectionTable;

    @FXML
    private TableColumn<ConnectionSettingDTO, String> hostColumn;

    @FXML
    private TableColumn<ConnectionSettingDTO, Integer> portColumn;

    @FXML
    private TableColumn<ConnectionSettingDTO, Integer> timeoutColumn;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        hostColumn.setCellValueFactory(new DTOCellValueFactory<>("host", String.class));
        portColumn.setCellValueFactory(new DTOCellValueFactory<>("port", Integer.class));
        timeoutColumn.setCellValueFactory(new DTOCellValueFactory<>("timeout", Integer.class));

        connectionTable.setItems(getStoredConnections());
        connectionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                connectButton.setDisable(false);
                removeConnectionButton.setDisable(false);
            }
        });
        TableFilter.forTableView(connectionTable).apply();
    }

    @FXML
    public void handleClose(final ActionEvent event) {
        Platform.exit();
    }

    @FXML
    public void addConnection(final ActionEvent event) {
        final MWindow addConnectionWindow = (MWindow) model.find(ADD_CONNECTION_WINDOW_ID, application);
        addConnectionWindow.setVisible(true);
        addConnectionWindow.setOnTop(true);
    }

    @FXML
    public void removeConnection(final ActionEvent event) {
        final MWindow addConnectionWindow = (MWindow) model.find(ADD_CONNECTION_WINDOW_ID, application);
        addConnectionWindow.setVisible(true);
        addConnectionWindow.setOnTop(true);
    }

    @FXML
    public void connectAgent(final ActionEvent event) {
        try {
            final ConnectionSettingDTO selectedConnection = connectionTable.getSelectionModel().getSelectedItem();
            supervisor.connect(selectedConnection.host, selectedConnection.port, selectedConnection.timeout);

            final ConsoleAgent agent = supervisor.getAgent();
            System.out.println(agent.getAllBundles());
            final MWindow connectionChooserWindow = (MWindow) model.find(CONNECTION_WINDOW_ID, application);
            connectionChooserWindow.setVisible(false);
        } catch (final Exception e) {
            final ExceptionDialog dialog = new ExceptionDialog(e);
            dialog.initStyle(StageStyle.UNDECORATED);
            dialog.show();
        }
    }

    private ObservableList<ConnectionSettingDTO> getStoredConnections() {
        final ConnectionSettingDTO dto = new ConnectionSettingDTO("localhost", 2000, 400);
        return FXCollections.observableArrayList(dto);
    }

}
