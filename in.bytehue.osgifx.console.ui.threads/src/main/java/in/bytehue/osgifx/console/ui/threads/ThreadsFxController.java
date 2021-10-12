package in.bytehue.osgifx.console.ui.threads;

import java.net.URL;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.eclipse.fx.core.di.LocalInstance;
import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.agent.dto.XThreadDTO;
import in.bytehue.osgifx.console.ui.service.DataProvider;
import in.bytehue.osgifx.console.util.fx.DTOCellValueFactory;
import in.bytehue.osgifx.console.util.fx.NullTableViewSelectionModel;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class ThreadsFxController implements Initializable {

    @Inject
    @LocalInstance
    private FXMLLoader loader;

    @Inject
    private DataProvider dataProvider;

    @FXML
    private TableView<XThreadDTO> table;

    @Inject
    @Named("in.bytehue.osgifx.console.ui.threads")
    private BundleContext context;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        table.setSelectionModel(new NullTableViewSelectionModel<>(table));
        createControls();
    }

    private void createControls() {
        final TableColumn<XThreadDTO, String> nameColumn = new TableColumn<>("Name");

        nameColumn.setPrefWidth(500);
        nameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));

        final TableColumn<XThreadDTO, String> idColumn = new TableColumn<>("ID");

        idColumn.setPrefWidth(90);
        idColumn.setCellValueFactory(new DTOCellValueFactory<>("id", String.class));

        final TableColumn<XThreadDTO, String> priorityColumn = new TableColumn<>("Priority");

        priorityColumn.setPrefWidth(90);
        priorityColumn.setCellValueFactory(new DTOCellValueFactory<>("priority", String.class));

        final TableColumn<XThreadDTO, String> stateColumn = new TableColumn<>("State");

        stateColumn.setPrefWidth(140);
        stateColumn.setCellValueFactory(new DTOCellValueFactory<>("state", String.class));

        final TableColumn<XThreadDTO, String> isInterruptedColumn = new TableColumn<>("Is Interrupted?");

        isInterruptedColumn.setPrefWidth(140);
        isInterruptedColumn.setCellValueFactory(new DTOCellValueFactory<>("isInterrupted", String.class));

        final TableColumn<XThreadDTO, String> isAliveColumn = new TableColumn<>("Is Alive?");

        isAliveColumn.setPrefWidth(90);
        isAliveColumn.setCellValueFactory(new DTOCellValueFactory<>("isAlive", String.class));

        final TableColumn<XThreadDTO, String> isDaemonColumn = new TableColumn<>("Is Daemon?");

        isDaemonColumn.setPrefWidth(100);
        isDaemonColumn.setCellValueFactory(new DTOCellValueFactory<>("isDaemon", String.class));

        table.getColumns().add(nameColumn);
        table.getColumns().add(idColumn);
        table.getColumns().add(priorityColumn);
        table.getColumns().add(stateColumn);
        table.getColumns().add(isInterruptedColumn);
        table.getColumns().add(isAliveColumn);
        table.getColumns().add(isDaemonColumn);

        final ObservableList<XThreadDTO> threads = dataProvider.threads();
        table.setItems(threads);

        TableFilter.forTableView(table).apply();
    }

}
