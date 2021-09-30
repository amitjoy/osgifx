package in.bytehue.osgifx.console.application.themes;

import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;

import org.eclipse.fx.ui.services.theme.Stylesheet;
import org.eclipse.fx.ui.services.theme.Theme;
import org.eclipse.fx.ui.theme.AbstractTheme;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = Theme.class)
public final class DefaultTheme extends AbstractTheme {

    @Activate
    public DefaultTheme(final BundleContext context) {
        super("theme.default", "Default Theme", context.getBundle().getResource("css/default.css"));
    }

    @Override
    @Reference(cardinality = MULTIPLE, policy = DYNAMIC)
    public void registerStylesheet(final Stylesheet stylesheet) {
        super.registerStylesheet(stylesheet);
    }

    @Override
    public void unregisterStylesheet(final Stylesheet stylesheet) {
        super.unregisterStylesheet(stylesheet);
    }

}
