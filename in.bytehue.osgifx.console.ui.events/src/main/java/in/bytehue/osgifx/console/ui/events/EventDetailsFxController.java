package in.bytehue.osgifx.console.ui.events;

import java.net.URL;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import in.bytehue.osgifx.console.agent.dto.XEventDTO;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class EventDetailsFxController implements Initializable {

    @FXML
    private Label receivedAtLabel;

    @FXML
    private Label topicLabel;

    @FXML
    private TableView<Entry<String, String>> propertiesTable;

    @FXML
    private TableColumn<XEventDTO, String> propertiesKeyTableColumn;

    @FXML
    private TableColumn<XEventDTO, String> propertiesValueTableColumn;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
    }

    public void initControls(final XEventDTO event) {
        // TODO Auto-generated method stub

    }

}
