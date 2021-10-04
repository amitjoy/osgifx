package in.bytehue.osgifx.console.agent.provider;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

import in.bytehue.osgifx.console.agent.dto.XBundleInfoDTO;
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
        final XComponentDTO  dto      = new XComponentDTO();
        final XBundleInfoDTO bInfoDTO = new XBundleInfoDTO();

        bInfoDTO.id           = compDescDTO.bundle.id;
        bInfoDTO.symbolicName = compDescDTO.bundle.symbolicName;

        dto.id                    = compConfDTO.id;
        dto.name                  = compDescDTO.name;
        dto.bundle                = bInfoDTO;
        dto.factory               = compDescDTO.factory;
        dto.scope                 = compDescDTO.scope;
        dto.implementationClass   = compDescDTO.implementationClass;
        dto.configurationPolicy   = compDescDTO.configurationPolicy;
        dto.serviceInterfaces     = Stream.of(compDescDTO.serviceInterfaces).collect(Collectors.toList());
        dto.configurationPid      = Stream.of(compDescDTO.configurationPid).collect(Collectors.toList());
        dto.properties            = compConfDTO.properties.entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> (String) e.getValue()));
        dto.references            = Stream.of(compDescDTO.references).collect(Collectors.toList());
        dto.failure               = compConfDTO.failure;
        dto.activate              = compDescDTO.activate;
        dto.deactivate            = compDescDTO.deactivate;
        dto.modified              = compDescDTO.modified;
        dto.satisfiedReferences   = Stream.of(compConfDTO.satisfiedReferences).collect(Collectors.toList());
        dto.unsatisfiedReferences = Stream.of(compConfDTO.unsatisfiedReferences).collect(Collectors.toList());

        return dto;

    }

}
