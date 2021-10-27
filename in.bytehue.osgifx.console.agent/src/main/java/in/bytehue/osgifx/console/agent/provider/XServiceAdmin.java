package in.bytehue.osgifx.console.agent.provider;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;

import in.bytehue.osgifx.console.agent.dto.XBundleInfoDTO;
import in.bytehue.osgifx.console.agent.dto.XServiceDTO;

public class XServiceAdmin {

    private XServiceAdmin() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static List<XServiceDTO> get(final BundleContext context) {
        requireNonNull(context);
        final FrameworkDTO dto = context.getBundle(Constants.SYSTEM_BUNDLE_ID).adapt(FrameworkDTO.class);
        return dto.services.stream().map(s -> toDTO(s, context)).collect(toList());
    }

    private static XServiceDTO toDTO(final ServiceReferenceDTO refDTO, final BundleContext context) {
        final XServiceDTO dto = new XServiceDTO();

        final XBundleInfoDTO bundleInfo = new XBundleInfoDTO();
        bundleInfo.id           = refDTO.bundle;
        bundleInfo.symbolicName = bsn(refDTO.bundle, context);

        dto.id                = refDTO.id;
        dto.bundleId          = bundleInfo.id;
        dto.registeringBundle = bundleInfo.symbolicName;
        dto.properties        = refDTO.properties.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> arrayToString(e.getValue())));
        dto.usingBundles      = getUsingBundles(refDTO.usingBundles, context);
        dto.types             = getObjectClass(refDTO.properties);

        return dto;
    }

    private static List<String> getObjectClass(final Map<String, Object> properties) {
        final Object objectClass = properties.get(Constants.OBJECTCLASS);
        return Arrays.asList((String[]) objectClass);
    }

    private static String arrayToString(final Object value) {
        if (value instanceof String[]) {
            return Arrays.asList((String[]) value).toString();
        }
        return value.toString();
    }

    private static List<XBundleInfoDTO> getUsingBundles(final long[] usingBundles, final BundleContext context) {
        final List<XBundleInfoDTO> bundles = new ArrayList<>();
        for (final long id : usingBundles) {
            final String bsn = bsn(id, context);

            final XBundleInfoDTO dto = new XBundleInfoDTO();
            dto.id           = id;
            dto.symbolicName = bsn;

            bundles.add(dto);
        }
        return bundles;
    }

    private static String bsn(final long id, final BundleContext context) {
        for (final Bundle b : context.getBundles()) {
            if (b.getBundleId() == id) {
                return b.getSymbolicName();
            }
        }
        return null;
    }

}
