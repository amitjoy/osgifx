package in.bytehue.osgifx.console.ui.gogo;

import java.net.URL;
import java.util.ResourceBundle;

import javax.inject.Inject;

import com.google.common.base.Throwables;

import in.bytehue.osgifx.console.agent.Agent;
import in.bytehue.osgifx.console.supervisor.Supervisor;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

public final class GogoFxController implements Initializable {

    @FXML
    private TextField input;

    @FXML
    private TextArea output;

    @Inject
    private Supervisor supervisor;

    @Inject
    private GogoConsoleHistory history;

    private Agent agent;
    private int   historyPointer;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        historyPointer = 0;
        agent          = supervisor.getAgent();
    }

    @FXML
    private void handleInput(final KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
            case ENTER:
                final String command = input.getText();
                if ("clear".equals(command)) {
                    output.clear();
                    return;
                }
                output.appendText("$ " + command + System.lineSeparator());
                output.appendText(executeGogoCommand(command));
                output.appendText(System.lineSeparator());
                history.add(command);
                historyPointer = history.size();
                input.clear();
                break;
            case UP:
                if (historyPointer == 0) {
                    historyPointer = history.size();
                }
                historyPointer--;
                input.setText(history.get(historyPointer));
                input.selectAll();
                input.selectEnd(); // Does not change anything seemingly
                break;
            case DOWN:
                if (historyPointer == history.size() - 1) {
                    break;
                }
                historyPointer++;
                input.setText(history.get(historyPointer));
                input.selectAll();
                input.selectEnd(); // Does not change anything seemingly
                break;
            default:
                break;
        }
    }

    private String executeGogoCommand(final String command) {
        try {
            return agent.shell(command);
        } catch (final Exception e) {
            return Throwables.getStackTraceAsString(e);
        }
    }

}
