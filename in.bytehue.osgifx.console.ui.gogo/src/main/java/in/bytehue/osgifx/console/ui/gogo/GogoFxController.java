package in.bytehue.osgifx.console.ui.gogo;

import javax.inject.Inject;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.base.Throwables;

import in.bytehue.osgifx.console.agent.Agent;
import in.bytehue.osgifx.console.supervisor.Supervisor;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

public final class GogoFxController {

    @Log
    @Inject
    private FluentLogger       logger;
    @FXML
    private TextField          input;
    @FXML
    private TextArea           output;
    @Inject
    private Supervisor         supervisor;
    @Inject
    private GogoConsoleHistory history;
    private Agent              agent;
    private int                historyPointer;

    @FXML
    public void initialize() {
        historyPointer = 0;
        agent          = supervisor.getAgent();
        logger.atDebug().log("FXML controller has been initialized");
    }

    @FXML
    private void handleInput(final KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
            case ENTER:
                final String command = input.getText();
                if ("clear".equals(command)) {
                    output.clear();
                    input.clear();
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
            final String output = agent.shell(command);
            logger.atInfo().log("Command '%s' has been successfully executed", command);
            return output;
        } catch (final Exception e) {
            logger.atInfo().withException(e).log("Command '%s' cannot be executed properly", command);
            return Throwables.getStackTraceAsString(e);
        }
    }

}
