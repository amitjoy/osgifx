package in.bytehue.osgifx.console.ui.events;

import java.util.Date;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

import in.bytehue.osgifx.console.agent.dto.XEventDTO;
import in.bytehue.osgifx.console.util.fx.Fx;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class EventDetailsFxController {

    @Log
    @Inject
    private FluentLogger                               logger;
    @FXML
    private Label                                      receivedAtLabel;
    @FXML
    private Label                                      topicLabel;
    @FXML
    private TableView<Entry<String, String>>           propertiesTable;
    @FXML
    private TableColumn<Entry<String, String>, String> propertiesKeyTableColumn;
    @FXML
    private TableColumn<Entry<String, String>, String> propertiesValueTableColumn;
    private Converter                                  converter;

    @FXML
    public void initialize() {
        converter = Converters.standardConverter();
        logger.atDebug().log("FXML controller has been initialized");
    }

    public void initControls(final XEventDTO event) {
        receivedAtLabel.setText(formatReceivedAt(event.received));
        topicLabel.setText(event.topic);

        propertiesKeyTableColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getKey()));
        propertiesValueTableColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue()));
        propertiesTable.setItems(FXCollections.observableArrayList(event.properties.entrySet()));

        Fx.addContextMenuToCopyContent(propertiesTable);
    }

    private String formatReceivedAt(final long receivedAt) {
        if (receivedAt == 0) {
            return "No received timestamp";
        }
        return converter.convert(receivedAt).to(Date.class).toString();
    }

}
