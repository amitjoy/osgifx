package in.bytehue.osgifx.console.agent.provider;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;

import aQute.libg.tuple.Pair;
import in.bytehue.osgifx.console.agent.dto.XComponentDTO;

public final class XComponentInfoProvider {

    private XComponentInfoProvider() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static List<XComponentDTO> get(final BundleContext context, final ServiceComponentRuntime scr) {
        final List<XComponentDTO> dtos = new ArrayList<>();
        for (final ComponentDescriptionDTO compDescDTO : scr.getComponentDescriptionDTOs(context.getBundles())) {
            final Collection<ComponentConfigurationDTO> compConfDTOs = scr.getComponentConfigurationDTOs(compDescDTO);
            dtos.addAll(compConfDTOs.stream().map(dto -> toDTO(dto, compDescDTO)).collect(Collectors.toList()));
        }
        return dtos;
    }

    private static XComponentDTO toDTO(final ComponentConfigurationDTO compConfDTO, final ComponentDescriptionDTO compDescDTO) {
        final XComponentDTO dto = new XComponentDTO();

        dto.id                    = compConfDTO.id;
        dto.name                  = compDescDTO.name;
        dto.bundle                = Pair.newInstance(compDescDTO.bundle.symbolicName, compDescDTO.bundle.id);
        dto.factory               = compDescDTO.factory;
        dto.scope                 = compDescDTO.scope;
        dto.implementationClass   = compDescDTO.implementationClass;
        dto.configurationPolicy   = compDescDTO.configurationPolicy;
        dto.serviceInterfaces     = Stream.of(compDescDTO.serviceInterfaces).collect(Collectors.toList());
        dto.configurationPid      = Stream.of(compDescDTO.configurationPid).collect(Collectors.toList());
        dto.properties            = compConfDTO.properties.entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> (String) e.getValue()));
        dto.references            = toRefDTOs(compDescDTO.references);
        dto.failure               = compConfDTO.failure;
        dto.activate              = compDescDTO.activate;
        dto.deactivate            = compDescDTO.deactivate;
        dto.modified              = compDescDTO.modified;
        dto.satisfiedReferences   = satisfiedRefsToMap(compConfDTO.satisfiedReferences);
        dto.unsatisfiedReferences = unsatisfiedRefsToMap(compConfDTO.unsatisfiedReferences);

        return dto;

    }

    private static List<Map<String, String>> toRefDTOs(final ReferenceDTO[] references) {
        return Stream.of(references).map(XComponentInfoProvider::fromRefDTOToMap).collect(Collectors.toList());
    }

    private static Map<String, String> fromRefDTOToMap(final ReferenceDTO dto) {
        final Map<String, String> map = new HashMap<>();

        map.put("name", dto.name);
        map.put("interfaceName", dto.interfaceName);
        map.put("cardinality", dto.cardinality);
        map.put("policy", dto.policy);
        map.put("policyOption", dto.policyOption);
        map.put("target", dto.target);
        map.put("bind", dto.bind);
        map.put("unbind", dto.unbind);
        map.put("updated", dto.updated);
        map.put("field", dto.field);
        map.put("fieldOption", dto.fieldOption);
        map.put("scope", dto.scope);
        map.put("collectionType", dto.collectionType);
        map.put("parameter", String.valueOf(dto.parameter));

        return map;
    }

    private static Map<String, String> satisfiedRefsToMap(final SatisfiedReferenceDTO[] references) {
        final Map<String, String> refDTOs = new HashMap<>();
        for (final SatisfiedReferenceDTO dto : references) {
            refDTOs.put(dto.name, dto.target);
        }
        return refDTOs;
    }

    private static Map<String, String> unsatisfiedRefsToMap(final UnsatisfiedReferenceDTO[] references) {
        final Map<String, String> refDTOs = new HashMap<>();
        for (final UnsatisfiedReferenceDTO dto : references) {
            refDTOs.put(dto.name, dto.target);
        }
        return refDTOs;
    }

}
