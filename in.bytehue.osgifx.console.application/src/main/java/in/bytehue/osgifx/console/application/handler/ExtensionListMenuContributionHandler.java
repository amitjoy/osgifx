package in.bytehue.osgifx.console.application.handler;

import static org.eclipse.e4.ui.workbench.modeling.EPartService.PartState.ACTIVATE;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.di.AboutToShow;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MStackElement;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.basic.MWindowElement;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

public final class ExtensionListMenuContributionHandler {

    @Log
    @Inject
    private FluentLogger  logger;
    @Inject
    private EPartService  partService;
    @Inject
    private EModelService modelService;

    @AboutToShow
    public void aboutToShow(final List<MMenuElement> items, final MWindow window) {
        for (final MStackElement tab : getRegisteredTabs(window)) {
            final MDirectMenuItem tabMenu = createViewMenu(tab);
            items.add(tabMenu);
        }
    }

    @Execute
    public void execute(final MDirectMenuItem menuItem) {
        final String partId = menuItem.getAccessibilityPhrase();
        logger.atInfo().log("Activating part '%s'", partId);
        partService.showPart(partId, ACTIVATE);
    }

    private List<MStackElement> getRegisteredTabs(final MWindow window) {
        for (final MWindowElement element : window.getChildren()) {
            final String elementId = element.getElementId();
            if ("in.bytehue.osgifx.console.ui.extensions.partstack".equals(elementId)) {
                final MPartStack partStack = (MPartStack) element;
                return partStack.getChildren();
            }
        }
        return Collections.emptyList();
    }

    private MDirectMenuItem createViewMenu(final MStackElement element) {
        final MPart           part        = (MPart) element;
        final MDirectMenuItem dynamicItem = modelService.createModelElement(MDirectMenuItem.class);

        dynamicItem.setLabel(part.getLabel());
        dynamicItem.setIconURI(part.getIconURI());
        dynamicItem.setAccessibilityPhrase(part.getElementId());
        dynamicItem.setContributorURI("platform:/plugin/in.bytehue.osgifx.console.application");
        dynamicItem.setContributionURI(
                "bundleclass://in.bytehue.osgifx.console.application/in.bytehue.osgifx.console.application.handler.ExtensionListMenuContributionHandler");

        return dynamicItem;
    }

}