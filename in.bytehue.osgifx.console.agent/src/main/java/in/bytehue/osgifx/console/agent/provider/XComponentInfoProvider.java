package in.bytehue.osgifx.console.agent.provider;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.BundleContext;
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

    public static List<XComponentDTO> get(final BundleContext context, final ServiceComponentRuntime scr) {
        final List<XComponentDTO> dtos = new ArrayList<>();
        for (final ComponentDescriptionDTO compDescDTO : scr.getComponentDescriptionDTOs(context.getBundles())) {
            final Collection<ComponentConfigurationDTO> compConfDTOs = scr.getComponentConfigurationDTOs(compDescDTO);
            dtos.addAll(compConfDTOs.stream().map(dto -> toDTO(dto, compDescDTO)).collect(Collectors.toList()));
        }
        return dtos;
    }

    private static XComponentDTO toDTO(final ComponentConfigurationDTO compConfDTO, final ComponentDescriptionDTO compDescDTO) {
        final XComponentDTO  dto      = new XComponentDTO();
        final XBundleInfoDTO bInfoDTO = new XBundleInfoDTO();

        bInfoDTO.id           = compDescDTO.bundle.id;
        bInfoDTO.symbolicName = compDescDTO.bundle.symbolicName;

        dto.id                    = compConfDTO.id;
        dto.name                  = compDescDTO.name;
        dto.state                 = mapToState(compConfDTO.state);
        dto.registeringBundle     = bInfoDTO.symbolicName;
        dto.registeringBundleId   = bInfoDTO.id;
        dto.factory               = compDescDTO.factory;
        dto.scope                 = compDescDTO.scope;
        dto.implementationClass   = compDescDTO.implementationClass;
        dto.configurationPolicy   = compDescDTO.configurationPolicy;
        dto.serviceInterfaces     = Stream.of(compDescDTO.serviceInterfaces).collect(Collectors.toList());
        dto.configurationPid      = Stream.of(compDescDTO.configurationPid).collect(Collectors.toList());
        dto.properties            = compConfDTO.properties.entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> arrayToString(e.getValue())));
        dto.references            = Stream.of(compDescDTO.references).collect(Collectors.toList());
        dto.failure               = compConfDTO.failure;
        dto.activate              = compDescDTO.activate;
        dto.deactivate            = compDescDTO.deactivate;
        dto.modified              = compDescDTO.modified;
        dto.satisfiedReferences   = Stream.of(compConfDTO.satisfiedReferences).map(XComponentInfoProvider::toXS)
                .collect(Collectors.toList());
        dto.unsatisfiedReferences = Stream.of(compConfDTO.unsatisfiedReferences).map(XComponentInfoProvider::toXUS)
                .collect(Collectors.toList());

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
        xsr.objectClass = "";        // TODO

        return xsr;
    }

    private static XUnsatisfiedReferenceDTO toXUS(final UnsatisfiedReferenceDTO dto) {
        final XUnsatisfiedReferenceDTO uxsr = new XUnsatisfiedReferenceDTO();

        uxsr.name        = dto.name;
        uxsr.target      = dto.target;
        uxsr.objectClass = "";        // TODO

        return uxsr;
    }

    private static String arrayToString(final Object value) {
        if (value instanceof String[]) {
            return Arrays.asList((String[]) value).toString();
        }
        return value.toString();
    }

}
