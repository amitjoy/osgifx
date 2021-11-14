package in.bytehue.osgifx.console.ui.logs;

import java.util.Date;

import javax.inject.Inject;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

import in.bytehue.osgifx.console.agent.dto.XLogEntryDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public final class LogDetailsFxController {

    @Log
    @Inject
    private FluentLogger logger;
    @FXML
    private Label        receivedAtLabel;
    @FXML
    private Label        levelLabel;
    @FXML
    private Label        bundleLabel;
    @FXML
    private TextArea     messageText;
    @FXML
    private TextArea     exceptionText;
    private Converter    converter;

    @FXML
    public void initialize() {
        converter = Converters.standardConverter();
        logger.atDebug().log("FXML controller has been initialized");
    }

    public void initControls(final XLogEntryDTO logEntry) {
        receivedAtLabel.setText(formatReceivedAt(logEntry.loggedAt));
        levelLabel.setText(logEntry.level);
        bundleLabel.setText(logEntry.bundle.symbolicName);
        messageText.setText(logEntry.message);
        if (logEntry.exception != null) {
            exceptionText.setText(logEntry.exception);
        }
    }

    private String formatReceivedAt(final long receivedAt) {
        if (receivedAt == 0) {
            return "No received timestamp";
        }
        return converter.convert(receivedAt).to(Date.class).toString();
    }

}
