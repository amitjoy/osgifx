package in.bytehue.osgifx.console.application;

import org.eclipse.fx.ui.workbench.fx.E4Application;

import javafx.application.Application;

public final class ConsoleFxApplication extends E4Application {

    @Override
    protected Class<? extends Application> getJfxApplicationClass() {
        return ConsoleFxStage.class;
    }

}
