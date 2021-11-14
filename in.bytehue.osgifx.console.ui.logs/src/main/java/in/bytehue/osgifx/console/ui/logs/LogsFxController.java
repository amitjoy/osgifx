package in.bytehue.osgifx.console.ui.logs;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.controlsfx.control.table.TableRowExpanderColumn.TableRowDataFeatures;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.agent.dto.XLogEntryDTO;
import in.bytehue.osgifx.console.data.provider.DataProvider;
import in.bytehue.osgifx.console.util.fx.DTOCellValueFactory;
import in.bytehue.osgifx.console.util.fx.Fx;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=in.bytehue.osgifx.console.data.provider.DataProvider)")
public final class LogsFxController {

    @Log
    @Inject
    private FluentLogger                       logger;
    @Inject
    @LocalInstance
    private FXMLLoader                         loader;
    @FXML
    private TableView<XLogEntryDTO>            table;
    @Inject
    private DataProvider                       dataProvider;
    @Inject
    @Named("in.bytehue.osgifx.console.ui.logs")
    private BundleContext                      context;
    private TableRowDataFeatures<XLogEntryDTO> selectedLog;

    @FXML
    public void initialize() {
        try {
            createControls();
            Fx.disableSelectionModel(table);
            logger.atDebug().log("FXML controller has been initialized");
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private void createControls() {
        final GridPane                             expandedNode   = (GridPane) Fx.loadFXML(loader, context,
                "/fxml/expander-column-content.fxml");
        final LogDetailsFxController               controller     = loader.getController();
        final TableRowExpanderColumn<XLogEntryDTO> expanderColumn = new TableRowExpanderColumn<>(expandedLog -> {
                                                                      controller.initControls(expandedLog.getValue());
                                                                      if (selectedLog != null && selectedLog.isExpanded()) {
                                                                          selectedLog.toggleExpanded();
                                                                      }
                                                                      selectedLog = expandedLog;
                                                                      return expandedNode;
                                                                  });

        final TableColumn<XLogEntryDTO, Date> loggedAtColumn = new TableColumn<>("Logged At");

        loggedAtColumn.setPrefWidth(270);
        loggedAtColumn.setCellValueFactory(new DTOCellValueFactory<>("loggedAt", Date.class));

        final TableColumn<XLogEntryDTO, String> logLevelColumn = new TableColumn<>("Level");

        logLevelColumn.setPrefWidth(110);
        logLevelColumn.setCellValueFactory(new DTOCellValueFactory<>("level", String.class));

        final TableColumn<XLogEntryDTO, String> messageColumn = new TableColumn<>("Message");

        messageColumn.setPrefWidth(750);
        messageColumn.setCellValueFactory(new DTOCellValueFactory<>("message", String.class));

        table.getColumns().add(expanderColumn);
        table.getColumns().add(loggedAtColumn);
        table.getColumns().add(logLevelColumn);
        table.getColumns().add(messageColumn);

        final ObservableList<XLogEntryDTO> logs = dataProvider.logs();
        table.setItems(logs);

        TableFilter.forTableView(table).apply();
    }

}
