package in.bytehue.osgifx.console.util.fx;

import static in.bytehue.osgifx.console.constants.FxConstants.STANDARD_CSS;

import java.util.Optional;
import java.util.function.Consumer;

import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.dialog.ProgressDialog;

import javafx.concurrent.Worker;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.image.ImageView;
import javafx.stage.StageStyle;

public final class FxDialog {

    private FxDialog() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static Alert showInfoDialog(final String header, final String content, final ClassLoader cssResLoader) {
        return showDialog(AlertType.INFORMATION, "", header, content, cssResLoader, null);
    }

    public static Alert showInfoDialog(final String header, final String content, final ClassLoader cssResLoader,
            final Consumer<ButtonType> actionOk) {
        return showDialog(AlertType.INFORMATION, "", header, content, cssResLoader, actionOk);
    }

    public static Alert showWarningDialog(final String header, final String content, final ClassLoader cssResLoader) {
        return showDialog(AlertType.WARNING, "", header, content, cssResLoader, null);
    }

    public static Alert showErrorDialog(final String header, final String content, final ClassLoader cssResLoader) {
        return showDialog(AlertType.ERROR, "", header, content, cssResLoader, null);
    }

    public static Alert showConfirmationDialog(final String header, final String content, final ClassLoader cssResLoader,
            final Consumer<ButtonType> action) {
        return showDialog(AlertType.CONFIRMATION, "", header, content, cssResLoader, action);
    }

    public static ExceptionDialog showExceptionDialog(final Throwable throwable, final ClassLoader cssResLoader) {
        final ExceptionDialog dialog = new ExceptionDialog(throwable);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.getDialogPane().getStylesheets().add(cssResLoader.getResource(STANDARD_CSS).toExternalForm());
        dialog.show();
        return dialog;
    }

    public static ProgressDialog showProgressDialog(final String header, final Worker<?> worker, final ClassLoader cssResLoader) {
        final ProgressDialog progressDialog = new ProgressDialog(worker);

        progressDialog.setHeaderText(header);
        progressDialog.initStyle(StageStyle.UNDECORATED);
        progressDialog.getDialogPane().getStylesheets().add(cssResLoader.getResource(STANDARD_CSS).toExternalForm());
        progressDialog.show();

        return progressDialog;
    }

    public static ChoiceDialog<String> showChoiceDialog(final String header, final ClassLoader resLoader, final String graphicPath,
            final Consumer<String> actionOk, final Runnable actionCancel, final String defaultChoice, final String... choices) {
        final ChoiceDialog<String> choiceDialog = new ChoiceDialog<>(defaultChoice, choices);

        choiceDialog.setHeaderText(header);
        choiceDialog.initStyle(StageStyle.UNDECORATED);
        choiceDialog.getDialogPane().getStylesheets().add(resLoader.getResource(STANDARD_CSS).toExternalForm());
        choiceDialog.setGraphic(new ImageView(resLoader.getResource(graphicPath).toString()));

        final Optional<String> returnVal = choiceDialog.showAndWait();
        if (actionOk != null && returnVal.isPresent()) {
            actionOk.accept(returnVal.get());
        } else if (actionCancel != null) {
            actionCancel.run();
        }
        return choiceDialog;
    }

    public static Alert showDialog(final AlertType type, final String title, final String header, final String content,
            final ClassLoader cssResLoader, final Consumer<ButtonType> action) {

        final Alert alert = new Alert(type);
        alert.initStyle(StageStyle.UNDECORATED);
        alert.getDialogPane().getStylesheets().add(cssResLoader.getResource(STANDARD_CSS).toExternalForm());

        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        final Optional<ButtonType> returnVal = alert.showAndWait();
        if (action != null && returnVal.isPresent()) {
            action.accept(returnVal.get());
        }
        return alert;
    }

}
