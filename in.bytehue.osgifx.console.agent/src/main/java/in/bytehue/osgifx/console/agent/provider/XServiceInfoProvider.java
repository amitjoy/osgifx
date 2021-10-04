package in.bytehue.osgifx.console.agent.provider;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;

import aQute.libg.tuple.Pair;
import in.bytehue.osgifx.console.agent.dto.XServiceDTO;

public final class XServiceInfoProvider {

    private XServiceInfoProvider() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static List<XServiceDTO> get(final BundleContext context) {
        final FrameworkDTO dto = context.getBundle(Constants.SYSTEM_BUNDLE_ID).adapt(FrameworkDTO.class);
        return dto.services.stream().map(s -> toDTO(s, context)).collect(toList());
    }

    private static XServiceDTO toDTO(final ServiceReferenceDTO refDTO, final BundleContext context) {
        final XServiceDTO dto = new XServiceDTO();

        dto.bundle       = Pair.newInstance(ConsoleAgentHelper.bsn(refDTO.bundle, context), refDTO.bundle);
        dto.id           = refDTO.id;
        dto.properties   = refDTO.properties.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> (String) e.getValue()));
        dto.usingBundles = getUsingBundles(refDTO.usingBundles, context);

        return dto;
    }

    private static Map<Long, String> getUsingBundles(final long[] usingBundles, final BundleContext context) {
        final Map<Long, String> bundles = new HashMap<>();
        for (final long id : usingBundles) {
            final String bsn = ConsoleAgentHelper.bsn(id, context);
            bundles.put(id, bsn);
        }
        return bundles;
    }

}
