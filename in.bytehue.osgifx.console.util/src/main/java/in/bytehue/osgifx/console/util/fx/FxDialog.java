package in.bytehue.osgifx.console.util.fx;

import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.dialog.ProgressDialog;

import javafx.concurrent.Worker;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.StageStyle;

public final class FxDialog {

    private FxDialog() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static Alert showInfoDialog(final String header, final String content, final ClassLoader cssResLoader) {
        return showDialog(AlertType.INFORMATION, "", header, content, cssResLoader);
    }

    public static Alert showWarningDialog(final String header, final String content, final ClassLoader cssResLoader) {
        return showDialog(AlertType.WARNING, "", header, content, cssResLoader);
    }

    public static Alert showErrorDialog(final String header, final String content, final ClassLoader cssResLoader) {
        return showDialog(AlertType.ERROR, "", header, content, cssResLoader);
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
            final ClassLoader cssResLoader) {

        final Alert alert = new Alert(type);
        alert.initStyle(StageStyle.UNDECORATED);
        alert.getDialogPane().getStylesheets().add(cssResLoader.getResource("/css/default.css").toExternalForm());

        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        alert.showAndWait();

        return alert;
    }

}
