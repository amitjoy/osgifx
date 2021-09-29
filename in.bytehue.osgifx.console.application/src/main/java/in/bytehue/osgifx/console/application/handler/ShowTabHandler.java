
package in.bytehue.osgifx.console.application.handler;

import static org.eclipse.e4.ui.workbench.modeling.EPartService.PartState.ACTIVATE;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

public final class ShowTabHandler {

    @Inject
    private EPartService partService;

    /**
     * This could have been much easier with HandledMenuItem as it can take parameter
     * but HandledMenuItem doesn't work properly with e(fx)clipse
     */
    @Execute
    public void execute(final MDirectMenuItem menuItem) {
        partService.showPart(menuItem.getAccessibilityPhrase(), ACTIVATE);
    }

}