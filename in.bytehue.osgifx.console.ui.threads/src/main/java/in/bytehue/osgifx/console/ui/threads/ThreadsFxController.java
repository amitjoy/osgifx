package in.bytehue.osgifx.console.ui.threads;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import javax.inject.Inject;

import org.controlsfx.control.table.TableFilter;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Capability;

import in.bytehue.osgifx.console.agent.dto.XThreadDTO;
import in.bytehue.osgifx.console.data.provider.DataProvider;
import in.bytehue.osgifx.console.util.fx.DTOCellValueFactory;
import in.bytehue.osgifx.console.util.fx.Fx;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

@Capability(namespace = SERVICE_NAMESPACE, attribute = "objectClass:List<String>=in.bytehue.osgifx.console.data.provider.DataProvider")
public final class ThreadsFxController {

    @Log
    @Inject
    private FluentLogger                    logger;
    @FXML
    private TableView<XThreadDTO>           table;
    @FXML
    private TableColumn<XThreadDTO, String> nameColumn;
    @FXML
    private TableColumn<XThreadDTO, String> idColumn;
    @FXML
    private TableColumn<XThreadDTO, String> priorityColumn;
    @FXML
    private TableColumn<XThreadDTO, String> stateColumn;
    @FXML
    private TableColumn<XThreadDTO, String> isInterruptedColumn;
    @FXML
    private TableColumn<XThreadDTO, String> isAliveColumn;
    @FXML
    private TableColumn<XThreadDTO, String> isDaemonColumn;
    @Inject
    private DataProvider                    dataProvider;

    @FXML
    public void initialize() {
        initCells();
        Fx.addContextMenuToCopyContent(table);
        logger.atDebug().log("FXML controller has been initialized");
    }

    private void initCells() {
        nameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));
        idColumn.setCellValueFactory(new DTOCellValueFactory<>("id", String.class));
        priorityColumn.setCellValueFactory(new DTOCellValueFactory<>("priority", String.class));
        stateColumn.setCellValueFactory(new DTOCellValueFactory<>("state", String.class));
        isInterruptedColumn.setCellValueFactory(new DTOCellValueFactory<>("isInterrupted", String.class));
        isAliveColumn.setCellValueFactory(new DTOCellValueFactory<>("isAlive", String.class));
        isDaemonColumn.setCellValueFactory(new DTOCellValueFactory<>("isDaemon", String.class));

        table.setItems(dataProvider.threads());
        Fx.sortBy(table, nameColumn);

        TableFilter.forTableView(table).apply();
    }

}
