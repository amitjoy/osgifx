package in.bytehue.osgifx.console.agent.provider;

import static org.osgi.framework.Constants.SYSTEM_BUNDLE_ID;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.FrameworkDTO;

import in.bytehue.osgifx.console.agent.dto.XPropertyDTO;

public final class XPropertytInfoProvider {

    private XPropertytInfoProvider() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static List<XPropertyDTO> get(final BundleContext context) {
        final FrameworkDTO dto = context.getBundle(SYSTEM_BUNDLE_ID).adapt(FrameworkDTO.class);
        return prepareProperties(dto.properties);
    }

    private static List<XPropertyDTO> prepareProperties(final Map<String, Object> properties) {
        final Map<String, XPropertyDTO> allProperties = new HashMap<>();

        for (final Entry<String, Object> property : properties.entrySet()) {
            final String       key   = property.getKey();
            final String       value = property.getValue().toString();
            final XPropertyDTO dto   = createPropertyDTO(key, value, "Framework");
            allProperties.put(key, dto);
        }

        @SuppressWarnings("rawtypes")
        final Map                        systemProperties = System.getProperties();
        @SuppressWarnings("unchecked")
        final Set<Entry<String, String>> sets             = ((Map<String, String>) systemProperties).entrySet();
        for (final Entry<String, String> property : sets) {
            final String       key   = property.getKey();
            final String       value = property.getValue();
            final XPropertyDTO dto   = createPropertyDTO(key, value, "System");
            allProperties.put(key, dto);
        }
        return allProperties.values().stream().collect(Collectors.toList());
    }

    private static XPropertyDTO createPropertyDTO(final String name, final String value, final String type) {
        final XPropertyDTO dto = new XPropertyDTO();
        dto.name  = name;
        dto.value = value;
        dto.type  = type;
        return dto;
    }

}
