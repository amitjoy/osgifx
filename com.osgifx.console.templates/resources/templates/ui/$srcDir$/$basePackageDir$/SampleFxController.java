package $basePackageName$;

import jakarta.inject.Inject;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;

public final class SampleFxController {

	@Log
	@Inject
	private FluentLogger logger;
	@FXML
	private AnchorPane   rootPane;

	@FXML
	public void initialize() {
		logger.atDebug().log("FXML controller has been initialized");
	}

}
