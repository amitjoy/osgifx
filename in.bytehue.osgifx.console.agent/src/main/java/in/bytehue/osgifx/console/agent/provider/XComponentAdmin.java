package in.bytehue.osgifx.console.agent.provider;

import static in.bytehue.osgifx.console.agent.dto.XResultDTO.ERROR;
import static in.bytehue.osgifx.console.agent.dto.XResultDTO.SUCCESS;
import static in.bytehue.osgifx.console.agent.provider.AgentServer.createResult;

import java.lang.reflect.InvocationTargetException;
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
import org.osgi.service.component.runtime.dto.ReferenceDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;

import in.bytehue.osgifx.console.agent.dto.XBundleInfoDTO;
import in.bytehue.osgifx.console.agent.dto.XComponentDTO;
import in.bytehue.osgifx.console.agent.dto.XReferenceDTO;
import in.bytehue.osgifx.console.agent.dto.XResultDTO;
import in.bytehue.osgifx.console.agent.dto.XSatisfiedReferenceDTO;
import in.bytehue.osgifx.console.agent.dto.XUnsatisfiedReferenceDTO;

public class XComponentAdmin {

    private static final long PROCESSING_TIMEOUT = 10_000L;

    private final ServiceComponentRuntime scr;

    public XComponentAdmin(final Object scr) {
        this.scr = (ServiceComponentRuntime) scr;
    }

    public List<XComponentDTO> getComponents() {
        if (scr == null) {
            return Collections.emptyList();
        }
        final List<XComponentDTO> dtos = new ArrayList<>();
        for (final ComponentDescriptionDTO compDescDTO : scr.getComponentDescriptionDTOs()) {
            final Collection<ComponentConfigurationDTO> compConfDTOs = scr.getComponentConfigurationDTOs(compDescDTO);
            if (compConfDTOs.isEmpty()) {
                // this is for use cases when a component doesn't have any
                // configuration yet as it is probably disabled
                dtos.add(toDTO(null, compDescDTO));
            }
            dtos.addAll(compConfDTOs.stream().map(dto -> toDTO(dto, compDescDTO)).collect(Collectors.toList()));
        }
        return dtos;
    }

    public XResultDTO enableComponent(final long id) {
        if (scr == null) {
            return createResult(XResultDTO.SKIPPED, "Required service is unavailable to process the request");
        }
        final StringBuilder                       builder         = new StringBuilder();
        final Collection<ComponentDescriptionDTO> descriptionDTOs = scr.getComponentDescriptionDTOs();

        for (final ComponentDescriptionDTO dto : descriptionDTOs) {
            final Collection<ComponentConfigurationDTO> configurationDTOs = scr.getComponentConfigurationDTOs(dto);
            for (final ComponentConfigurationDTO configDTO : configurationDTOs) {
                if (configDTO.id == id) {
                    try {
                        scr.enableComponent(dto).onFailure(e -> builder.append(e.getMessage()).append(System.lineSeparator()))
                                .timeout(PROCESSING_TIMEOUT).getValue();
                    } catch (InvocationTargetException | InterruptedException e) {
                        builder.append(e.getMessage()).append(System.lineSeparator());
                    }
                    final String response = builder.toString();
                    return response.isEmpty() ? createResult(SUCCESS, "Component with id '" + id + "' has been successfully enabled")
                            : createResult(ERROR, response);
                }
            }
        }
        return createResult(SUCCESS, "Component with id '" + id
                + "' has not been found. Probably the component has not yet been enabled and that's why there is no associated id yet. "
                + "Try to disable the component by name.");
    }

    public XResultDTO enableComponent(final String name) {
        if (scr == null) {
            return createResult(XResultDTO.SKIPPED, "Required service is unavailable to process the request");
        }
        final StringBuilder                       builder         = new StringBuilder();
        final Collection<ComponentDescriptionDTO> descriptionDTOs = scr.getComponentDescriptionDTOs();

        for (final ComponentDescriptionDTO dto : descriptionDTOs) {
            if (dto.name.equals(name)) {
                try {
                    scr.enableComponent(dto).onFailure(e -> builder.append(e.getMessage()).append(System.lineSeparator()))
                            .timeout(PROCESSING_TIMEOUT).getValue();
                } catch (InvocationTargetException | InterruptedException e) {
                    builder.append(e.getMessage()).append(System.lineSeparator());
                }
                final String response = builder.toString();
                return response.isEmpty() ? createResult(SUCCESS, "Component with name '" + name + "' has been successfully enabled")
                        : createResult(ERROR, response);
            }
        }
        return createResult(SUCCESS, "Component with name '" + name + "' has not been found");
    }

    public XResultDTO disableComponent(final long id) {
        if (scr == null) {
            return createResult(XResultDTO.SKIPPED, "Required service is unavailable to process the request");
        }
        final StringBuilder                       builder         = new StringBuilder();
        final Collection<ComponentDescriptionDTO> descriptionDTOs = scr.getComponentDescriptionDTOs();

        for (final ComponentDescriptionDTO dto : descriptionDTOs) {
            final Collection<ComponentConfigurationDTO> configurationDTOs = scr.getComponentConfigurationDTOs(dto);
            for (final ComponentConfigurationDTO configDTO : configurationDTOs) {
                if (configDTO.id == id) {
                    try {
                        scr.disableComponent(dto).onFailure(e -> builder.append(e.getMessage()).append(System.lineSeparator()))
                                .timeout(PROCESSING_TIMEOUT).getValue();
                    } catch (InvocationTargetException | InterruptedException e) {
                        builder.append(e.getMessage()).append(System.lineSeparator());
                    }
                    final String response = builder.toString();
                    return response.isEmpty() ? createResult(SUCCESS, "Component with id '" + id + "' has been successfully disabled")
                            : createResult(ERROR, response);
                }
            }
        }
        return createResult(SUCCESS, "Component with id '" + id + "' has not been found");
    }

    public XResultDTO disableComponent(final String name) {
        if (scr == null) {
            return createResult(XResultDTO.SKIPPED, "Required service is unavailable to process the request");
        }
        final StringBuilder                       builder         = new StringBuilder();
        final Collection<ComponentDescriptionDTO> descriptionDTOs = scr.getComponentDescriptionDTOs();

        for (final ComponentDescriptionDTO dto : descriptionDTOs) {
            if (dto.name.equals(name)) {
                try {
                    scr.disableComponent(dto).onFailure(e -> builder.append(e.getMessage()).append(System.lineSeparator()))
                            .timeout(PROCESSING_TIMEOUT).getValue();
                } catch (InvocationTargetException | InterruptedException e) {
                    builder.append(e.getMessage()).append(System.lineSeparator());
                }
                final String response = builder.toString();
                return response.isEmpty() ? createResult(SUCCESS, "Component with name '" + name + "' has been successfully disable")
                        : createResult(ERROR, response);
            }
        }
        return createResult(SUCCESS, "Component with name '" + name + "' has not been found");
    }

    private XComponentDTO toDTO(final ComponentConfigurationDTO compConfDTO, final ComponentDescriptionDTO compDescDTO) {
        final XComponentDTO  dto      = new XComponentDTO();
        final XBundleInfoDTO bInfoDTO = new XBundleInfoDTO();

        bInfoDTO.id           = compDescDTO.bundle.id;
        bInfoDTO.symbolicName = compDescDTO.bundle.symbolicName;

        dto.id                    = Optional.ofNullable(compConfDTO).map(a -> String.valueOf(a.id)).orElse("<NA>");
        dto.name                  = compDescDTO.name;
        dto.state                 = Optional.ofNullable(compConfDTO).map(a -> mapToState(a.state)).orElse("DISABLED");
        dto.registeringBundle     = bInfoDTO.symbolicName;
        dto.registeringBundleId   = bInfoDTO.id;
        dto.factory               = compDescDTO.factory;
        dto.scope                 = compDescDTO.scope;
        dto.implementationClass   = compDescDTO.implementationClass;
        dto.configurationPolicy   = compDescDTO.configurationPolicy;
        dto.serviceInterfaces     = Stream.of(compDescDTO.serviceInterfaces).collect(Collectors.toList());
        dto.configurationPid      = Stream.of(compDescDTO.configurationPid).collect(Collectors.toList());
        dto.properties            = Optional.ofNullable(compConfDTO)
                .map(a -> a.properties.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> arrayToString(e.getValue()))))
                .orElse(Collections.emptyMap());
        dto.references            = Stream.of(compDescDTO.references).map(this::toRef).collect(Collectors.toList());
        dto.failure               = Optional.ofNullable(compConfDTO).map(a -> a.failure).orElse("");
        dto.activate              = compDescDTO.activate;
        dto.deactivate            = compDescDTO.deactivate;
        dto.modified              = compDescDTO.modified;
        dto.satisfiedReferences   = Stream
                .of(Optional.ofNullable(compConfDTO).map(a -> a.satisfiedReferences).orElse(new SatisfiedReferenceDTO[0])).map(this::toXS)
                .collect(Collectors.toList());
        dto.unsatisfiedReferences = Stream
                .of(Optional.ofNullable(compConfDTO).map(a -> a.unsatisfiedReferences).orElse(new UnsatisfiedReferenceDTO[0]))
                .map(this::toXUS).collect(Collectors.toList());

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

    private XSatisfiedReferenceDTO toXS(final SatisfiedReferenceDTO dto) {
        final XSatisfiedReferenceDTO xsr = new XSatisfiedReferenceDTO();

        xsr.name              = dto.name;
        xsr.target            = dto.target;
        xsr.objectClass       = prepareObjectClass(dto.boundServices);
        xsr.serviceReferences = dto.boundServices;

        return xsr;
    }

    private XUnsatisfiedReferenceDTO toXUS(final UnsatisfiedReferenceDTO dto) {
        final XUnsatisfiedReferenceDTO uxsr = new XUnsatisfiedReferenceDTO();

        uxsr.name        = dto.name;
        uxsr.target      = dto.target;
        uxsr.objectClass = prepareObjectClass(dto.targetServices);

        return uxsr;
    }

    private String prepareObjectClass(final ServiceReferenceDTO[] services) {
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

    private String arrayToString(final Object value) {
        if (value instanceof String[]) {
            return Arrays.asList((String[]) value).toString();
        }
        return value.toString();
    }

    private XReferenceDTO toRef(final ReferenceDTO reference) {
        final XReferenceDTO ref = new XReferenceDTO();

        ref.name           = reference.name;
        ref.interfaceName  = reference.interfaceName;
        ref.cardinality    = reference.cardinality;
        ref.policy         = reference.policy;
        ref.policyOption   = reference.policyOption;
        ref.target         = reference.target;
        ref.bind           = reference.bind;
        ref.unbind         = reference.unbind;
        ref.updated        = reference.updated;
        ref.field          = reference.field;
        ref.fieldOption    = reference.fieldOption;
        ref.scope          = reference.scope;
        ref.parameter      = reference.parameter;
        ref.collectionType = reference.collectionType;

        return ref;
    }

}
