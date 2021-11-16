package in.bytehue.osgifx.console.util.fx;

import java.util.Optional;
import java.util.function.Consumer;

import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.dialog.ProgressDialog;

import javafx.concurrent.Worker;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.StageStyle;

public final class FxDialog {

    private FxDialog() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static Alert showInfoDialog(final String header, final String content, final ClassLoader cssResLoader) {
        return showDialog(AlertType.INFORMATION, "", header, content, cssResLoader, null);
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
        dialog.getDialogPane().getStylesheets().add(cssResLoader.getResource("/css/default.css").toExternalForm());
        dialog.show();
        return dialog;
    }

    public static ProgressDialog showProgressDialog(final String header, final Worker<?> worker, final ClassLoader cssResLoader) {
        final ProgressDialog progressDialog = new ProgressDialog(worker);

        progressDialog.setHeaderText(header);
        progressDialog.initStyle(StageStyle.UNDECORATED);
        progressDialog.getDialogPane().getStylesheets().add(cssResLoader.getResource("/css/default.css").toExternalForm());
        progressDialog.show();

        return progressDialog;
    }

    public static Alert showDialog(final AlertType type, final String title, final String header, final String content,
            final ClassLoader cssResLoader, final Consumer<ButtonType> action) {

        final Alert alert = new Alert(type);
        alert.initStyle(StageStyle.UNDECORATED);
        alert.getDialogPane().getStylesheets().add(cssResLoader.getResource("/css/default.css").toExternalForm());

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
