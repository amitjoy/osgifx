package in.bytehue.osgifx.console.agent.provider;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Constants;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;

import in.bytehue.osgifx.console.agent.dto.XBundleInfoDTO;
import in.bytehue.osgifx.console.agent.dto.XComponentDTO;
import in.bytehue.osgifx.console.agent.dto.XSatisfiedReferenceDTO;
import in.bytehue.osgifx.console.agent.dto.XUnsatisfiedReferenceDTO;

public final class XComponentInfoProvider {

    private XComponentInfoProvider() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static List<XComponentDTO> get(final ServiceComponentRuntime scr) {
        final List<XComponentDTO> dtos = new ArrayList<>();
        for (final ComponentDescriptionDTO compDescDTO : scr.getComponentDescriptionDTOs()) {
            final Collection<ComponentConfigurationDTO> compConfDTOs = scr.getComponentConfigurationDTOs(compDescDTO);
            if (compConfDTOs.isEmpty()) {
                // this is for use cases when a component doesn't have any configuration yet as it is probably disabled
                dtos.add(toDTO(null, compDescDTO));
            }
            dtos.addAll(compConfDTOs.stream().map(dto -> toDTO(dto, compDescDTO)).collect(Collectors.toList()));
        }
        return dtos;
    }

    private static XComponentDTO toDTO(final ComponentConfigurationDTO compConfDTO, final ComponentDescriptionDTO compDescDTO) {
        final XComponentDTO  dto      = new XComponentDTO();
        final XBundleInfoDTO bInfoDTO = new XBundleInfoDTO();

        bInfoDTO.id           = compDescDTO.bundle.id;
        bInfoDTO.symbolicName = compDescDTO.bundle.symbolicName;

        dto.id                  = Optional.ofNullable(compConfDTO).map(a -> String.valueOf(a.id)).orElse("<NA>");
        dto.name                = compDescDTO.name;
        dto.state               = Optional.ofNullable(compConfDTO).map(a -> mapToState(a.state)).orElse("DISABLED");
        dto.registeringBundle   = bInfoDTO.symbolicName;
        dto.registeringBundleId = bInfoDTO.id;
        dto.factory             = compDescDTO.factory;
        dto.scope               = compDescDTO.scope;
        dto.implementationClass = compDescDTO.implementationClass;
        dto.configurationPolicy = compDescDTO.configurationPolicy;
        dto.serviceInterfaces   = Stream.of(compDescDTO.serviceInterfaces).collect(Collectors.toList());
        dto.configurationPid    = Stream.of(compDescDTO.configurationPid).collect(Collectors.toList());
        // @formatter:off
        dto.properties          = Optional.ofNullable(compConfDTO)
                                            .map(a -> a.properties.entrySet()
                                                                  .stream()
                                                                  .collect(toMap(Map.Entry::getKey, e -> arrayToString(e.getValue()))))
                                            .orElse(Collections.emptyMap());
        // @formatter:on
        dto.references = Stream.of(compDescDTO.references).collect(Collectors.toList());
        dto.failure    = Optional.ofNullable(compConfDTO).map(a -> a.failure).orElse("");
        dto.activate   = compDescDTO.activate;
        dto.deactivate = compDescDTO.deactivate;
        dto.modified   = compDescDTO.modified;
        // @formatter:off
        dto.satisfiedReferences = Stream.of(
                                        Optional.ofNullable(compConfDTO)
                                                .map(a -> a.satisfiedReferences)
                                                .orElse(new SatisfiedReferenceDTO[0]))
                                        .map(XComponentInfoProvider::toXS)
                                        .collect(Collectors.toList());
        dto.unsatisfiedReferences = Stream.of(
                                        Optional.ofNullable(compConfDTO)
                                                .map(a -> a.unsatisfiedReferences)
                                                .orElse(new UnsatisfiedReferenceDTO[0]))
                                        .map(XComponentInfoProvider::toXUS)
                                        .collect(Collectors.toList());
        // @formatter:on

        return dto;
    }

    private static String mapToState(final int state) {
        switch (state) {
            case ComponentConfigurationDTO.ACTIVE:
                return "ACTIVE";
            case ComponentConfigurationDTO.SATISFIED:
                return "SATISFIED";
            case ComponentConfigurationDTO.UNSATISFIED_REFERENCE:
                return "UNSATISFIED_REFERENCE";
            case ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION:
                return "UNSATISFIED_CONFIGURATION";
            case ComponentConfigurationDTO.FAILED_ACTIVATION:
                return "FAILED_ACTIVATION";
            default:
                return "<NO-MATCH>";
        }
    }

    private static XSatisfiedReferenceDTO toXS(final SatisfiedReferenceDTO dto) {
        final XSatisfiedReferenceDTO xsr = new XSatisfiedReferenceDTO();

        xsr.name        = dto.name;
        xsr.target      = dto.target;
        xsr.objectClass = prepareObjectClass(dto.boundServices);

        return xsr;
    }

    private static XUnsatisfiedReferenceDTO toXUS(final UnsatisfiedReferenceDTO dto) {
        final XUnsatisfiedReferenceDTO uxsr = new XUnsatisfiedReferenceDTO();

        uxsr.name        = dto.name;
        uxsr.target      = dto.target;
        uxsr.objectClass = prepareObjectClass(dto.targetServices);

        return uxsr;
    }

    private static String prepareObjectClass(final ServiceReferenceDTO[] services) {
        if (services == null) {
            return "";
        }
        final Set<String> finalList = new HashSet<>();
        for (final ServiceReferenceDTO dto : services) {
            final String[] objectClass = (String[]) dto.properties.get(Constants.OBJECTCLASS);
            finalList.addAll(Arrays.asList(objectClass));
        }
        return String.join(", ", finalList);
    }

    private static String arrayToString(final Object value) {
        if (value instanceof String[]) {
            return Arrays.asList((String[]) value).toString();
        }
        return value.toString();
    }

}
