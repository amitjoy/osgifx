
package in.bytehue.osgifx.console.application.handler;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MStackElement;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.basic.MWindowElement;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

public final class ShowTabHandler {

    @Inject
    private MWindow window;

    @Inject
    private EPartService partService;

    @Execute
    public void execute(@Named("part.id") final String partID) {
        boolean                    isTabClosed = true;
        final List<MWindowElement> children    = window.getChildren();
        for (final MWindowElement child : children) {
            if ("in.bytehue.osgifx.console.ui.extensions.partstack".equalsIgnoreCase(child.getElementId())) {
                final MPartStack          partStack = (MPartStack) child;
                final List<MStackElement> tabs      = partStack.getChildren();
                for (final MStackElement tab : tabs) {
                    if (tab.getElementId().equals(partID)) {
                        isTabClosed = false;
                    }
                }
            }
        }
        if (isTabClosed) {
            partService.showPart(partID, PartState.ACTIVATE);
        }
    }

}