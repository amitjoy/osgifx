package in.bytehue.osgifx.console.application.dialog;

import javax.inject.Inject;

import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.dialog.LoginDialog;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.eclipse.fx.core.ThreadSynchronize;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

public final class ConnectionDialog extends Dialog<ConnectionSettingDTO> {

    @Inject
    private ThreadSynchronize threadSync;
    private ButtonType        saveButtonType;
    private CustomTextField   txtHostname;
    private CustomTextField   txtPort;
    private CustomTextField   txtTimeout;

    public void init() {
        final DialogPane dialogPane = getDialogPane();

        initStyle(StageStyle.UNDECORATED);
        dialogPane.setHeaderText("Add Connection Settings");
        dialogPane.getStyleClass().add("login-dialog");
        dialogPane.getStylesheets().add(LoginDialog.class.getResource("dialogs.css").toExternalForm());
        dialogPane.getStylesheets().add(getClass().getResource("/css/default.css").toExternalForm());
        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL);

        txtHostname = (CustomTextField) TextFields.createClearableTextField();
        txtHostname.setLeft(new ImageView(getClass().getResource("/graphic/icons/hostname.png").toExternalForm()));

        txtPort = (CustomTextField) TextFields.createClearableTextField();
        txtPort.setLeft(new ImageView(getClass().getResource("/graphic/icons/port.png").toExternalForm()));

        txtTimeout = (CustomTextField) TextFields.createClearableTextField();
        txtTimeout.setLeft(new ImageView(getClass().getResource("/graphic/icons/timeout.png").toExternalForm()));

        final Label lbMessage = new Label("");
        lbMessage.getStyleClass().addAll("message-banner");
        lbMessage.setVisible(false);
        lbMessage.setManaged(false);

        final VBox content = new VBox(10);

        content.getChildren().add(lbMessage);
        content.getChildren().add(txtHostname);
        content.getChildren().add(txtPort);
        content.getChildren().add(txtTimeout);

        dialogPane.setContent(content);

        saveButtonType = new javafx.scene.control.ButtonType("Save", ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(saveButtonType);
        final Button loginButton = (Button) dialogPane.lookupButton(saveButtonType);
        loginButton.setOnAction(actionEvent -> {
            try {
                lbMessage.setVisible(false);
                lbMessage.setManaged(false);
                hide();
            } catch (final Exception ex) {
                lbMessage.setVisible(true);
                lbMessage.setManaged(true);
                lbMessage.setText(ex.getMessage());
                final ExceptionDialog dialog = new ExceptionDialog(ex);
                dialog.initStyle(StageStyle.UNDECORATED);
                dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/default.css").toExternalForm());
                dialog.show();
            }
        });
        final String hostnameCaption = "Hostname";
        final String portCaption     = "Port (between 1 to 65536)";
        final String timeoutCaption  = "Timeout in millis";

        txtHostname.setPromptText(hostnameCaption);
        txtPort.setPromptText(portCaption);
        txtTimeout.setPromptText(timeoutCaption);

        final ValidationSupport validationSupport = new ValidationSupport();
        threadSync.asyncExec(() -> {
            final String requiredFormat       = "'%s' is required";
            final String requiredPortFormat   = "'%s' should be a valid port number";
            final String requiredNumberFormat = "'%s' should be a valid integer number";
            validationSupport.registerValidator(txtHostname,
                    Validator.createEmptyValidator(String.format(requiredFormat, hostnameCaption)));
            validationSupport.registerValidator(txtPort, Validator.createEmptyValidator(String.format(requiredFormat, portCaption)));
            validationSupport.registerValidator(txtPort, Validator.createPredicateValidator(value -> {
                try {
                    final int integer = Integer.parseInt(value.toString());
                    return integer > 0 || integer > 65536;
                } catch (final Exception e) {
                    return false;
                }
            }, String.format(requiredPortFormat, portCaption)));
            validationSupport.registerValidator(txtTimeout, Validator.createEmptyValidator(String.format(requiredFormat, timeoutCaption)));
            validationSupport.registerValidator(txtTimeout, Validator.createPredicateValidator(value -> {
                try {
                    Integer.parseInt(value.toString());
                    return true;
                } catch (final Exception e) {
                    return false;
                }
            }, String.format(requiredNumberFormat, portCaption)));
        });
        setResultConverter(
                dialogButton -> dialogButton == saveButtonType
                        ? new ConnectionSettingDTO(txtHostname.getText(), Integer.parseInt(txtPort.getText()),
                                Integer.parseInt(txtTimeout.getText()))
                        : null);
    }

}
