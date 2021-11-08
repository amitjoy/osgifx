package in.bytehue.osgifx.console.util.fx;

import org.controlsfx.dialog.ExceptionDialog;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.StageStyle;

public final class FxDialog {

    private FxDialog() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static void showInfoDialog(final String header, final String content, final ClassLoader cssResLoader) {
        showDialog(AlertType.INFORMATION, "", header, content, cssResLoader);
    }

    public static void showWarningDialog(final String header, final String content, final ClassLoader cssResLoader) {
        showDialog(AlertType.WARNING, "", header, content, cssResLoader);
    }

    public static void showErrorDialog(final String header, final String content, final ClassLoader cssResLoader) {
        showDialog(AlertType.ERROR, "", header, content, cssResLoader);
    }

    public static void showExceptionDialog(final Throwable throwable, final ClassLoader cssResLoader) {
        final ExceptionDialog dialog = new ExceptionDialog(throwable);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.getDialogPane().getStylesheets().add(cssResLoader.getResource("/css/default.css").toExternalForm());
        dialog.show();
    }

    public static void showDialog(final AlertType type, final String title, final String header, final String content,
            final ClassLoader cssResLoader) {

        final Alert alert = new Alert(type);
        alert.initStyle(StageStyle.UNDECORATED);
        alert.getDialogPane().getStylesheets().add(cssResLoader.getResource("/css/default.css").toExternalForm());

        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        alert.showAndWait();
    }

}
