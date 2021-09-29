package in.bytehue.osgifx.console.ui.bundles.handler;

import static in.bytehue.osgifx.console.ui.bundles.util.Constants.TAB_ID;
import static in.bytehue.osgifx.console.ui.bundles.util.Constants.TAB_SHOW_COMMAND_ID;
import static in.bytehue.osgifx.console.ui.bundles.util.Constants.TAB_SHOW_COMMAND_PARAM;

import java.util.Collections;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.fx.core.command.CommandService;

public final class ShowBundlesHandler {

    @Inject
    private CommandService commandService;

    @Execute
    public void execute() {
        commandService.execute(TAB_SHOW_COMMAND_ID, Collections.singletonMap(TAB_SHOW_COMMAND_PARAM, TAB_ID));
    }

}