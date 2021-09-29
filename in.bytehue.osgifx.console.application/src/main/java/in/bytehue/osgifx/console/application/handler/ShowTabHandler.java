
package in.bytehue.osgifx.console.application.handler;

import static org.eclipse.e4.ui.workbench.modeling.EPartService.PartState.ACTIVATE;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

public final class ShowTabHandler {

    @Inject
    private EPartService partService;

    @Execute
    public void execute(final MWindow window, @Named("part.id") final String partId) {
        partService.showPart(partId, ACTIVATE);
    }

}