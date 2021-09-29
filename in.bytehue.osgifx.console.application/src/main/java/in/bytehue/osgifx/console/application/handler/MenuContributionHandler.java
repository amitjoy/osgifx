
package in.bytehue.osgifx.console.application.handler;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.ui.di.AboutToShow;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MStackElement;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.basic.MWindowElement;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

public final class MenuContributionHandler {

    @Inject
    private EModelService modelService;

    @AboutToShow
    public void aboutToShow(final List<MMenuElement> items, final MWindow window) {
        for (final MStackElement tab : getRegisteredTabs(window)) {
            final MDirectMenuItem tabMenu = createViewMenu(tab);
            items.add(tabMenu);
        }
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
        dynamicItem.setAccessibilityPhrase(part.getElementId());
        dynamicItem.setContributorURI("platform:/plugin/in.bytehue.osgifx.console.application");
        dynamicItem.setContributionURI(
                "bundleclass://in.bytehue.osgifx.console.application/in.bytehue.osgifx.console.application.handler.ShowTabHandler");

        return dynamicItem;
    }

}