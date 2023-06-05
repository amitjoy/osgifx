/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.admin;

import static com.osgifx.console.agent.dto.XResultDTO.ERROR;
import static com.osgifx.console.agent.dto.XResultDTO.SKIPPED;
import static com.osgifx.console.agent.dto.XResultDTO.SUCCESS;
import static com.osgifx.console.agent.helper.AgentHelper.createResult;
import static com.osgifx.console.agent.helper.AgentHelper.serviceUnavailable;
import static com.osgifx.console.agent.helper.OSGiCompendiumService.SCR;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.component.runtime.dto.ComponentConfigurationDTO.ACTIVE;
import static org.osgi.service.component.runtime.dto.ComponentConfigurationDTO.SATISFIED;
import static org.osgi.service.component.runtime.dto.ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION;
import static org.osgi.service.component.runtime.dto.ComponentConfigurationDTO.UNSATISFIED_REFERENCE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;
import org.osgi.util.promise.Promise;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.dto.XBundleInfoDTO;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.agent.dto.XReferenceDTO;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.agent.dto.XSatisfiedReferenceDTO;
import com.osgifx.console.agent.dto.XUnsatisfiedReferenceDTO;
import com.osgifx.console.agent.helper.Reflect;

import jakarta.inject.Inject;

public final class XComponentAdmin {

    private final ServiceComponentRuntime scr;
    private final FluentLogger            logger = LoggerFactory.getFluentLogger(getClass());

    @Inject
    public XComponentAdmin(final Object scr) {
        this.scr = (ServiceComponentRuntime) scr;
    }

    public List<XComponentDTO> getComponents() {
        if (scr == null) {
            logger.atWarn().msg(serviceUnavailable(SCR)).log();
            return Collections.emptyList();
        }
        final List<XComponentDTO> dtos = new ArrayList<>();
        try {
            for (final ComponentDescriptionDTO compDescDTO : scr.getComponentDescriptionDTOs()) {
                final Collection<ComponentConfigurationDTO> compConfDTOs = scr
                        .getComponentConfigurationDTOs(compDescDTO);
                if (compConfDTOs.isEmpty()) {
                    // this is for use cases when a component doesn't have any
                    // configuration yet as it is probably disabled
                    dtos.add(toDTO(null, compDescDTO));
                }
                dtos.addAll(compConfDTOs.stream().map(dto -> toDTO(dto, compDescDTO)).collect(toList()));
            }
        } catch (final Exception e) {
            logger.atError().msg("Error occurred while retrieving components").throwable(e).log();
            return Collections.emptyList();
        }
        return dtos;
    }

    public XResultDTO enableComponent(final long id) {
        if (scr == null) {
            logger.atWarn().msg(serviceUnavailable(SCR)).log();
            return createResult(SKIPPED, serviceUnavailable(SCR));
        }
        final StringBuilder                       builder         = new StringBuilder();
        final Collection<ComponentDescriptionDTO> descriptionDTOs = scr.getComponentDescriptionDTOs();

        for (final ComponentDescriptionDTO dto : descriptionDTOs) {
            final Collection<ComponentConfigurationDTO> configurationDTOs = scr.getComponentConfigurationDTOs(dto);
            for (final ComponentConfigurationDTO configDTO : configurationDTOs) {
                if (configDTO.id == id) {
                    try {
                        final Promise<Void> promise = scr.enableComponent(dto);
                        promise.getValue();
                    } catch (final Exception e) {
                        builder.append(e.getMessage()).append(System.lineSeparator());
                    }
                    final String response = builder.toString();
                    return response.isEmpty()
                            ? createResult(SUCCESS, "Component with id '" + id + "' has been successfully enabled")
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
            logger.atWarn().msg(serviceUnavailable(SCR)).log();
            return createResult(SKIPPED, serviceUnavailable(SCR));
        }
        final StringBuilder                       builder         = new StringBuilder();
        final Collection<ComponentDescriptionDTO> descriptionDTOs = scr.getComponentDescriptionDTOs();

        for (final ComponentDescriptionDTO dto : descriptionDTOs) {
            if (dto.name.equals(name)) {
                try {
                    final Promise<Void> promise = scr.enableComponent(dto);
                    promise.getValue();
                } catch (final Exception e) {
                    builder.append(e.getMessage()).append(System.lineSeparator());
                }
                final String response = builder.toString();
                return response.isEmpty()
                        ? createResult(SUCCESS, "Component with name '" + name + "' has been successfully enabled")
                        : createResult(ERROR, response);
            }
        }
        return createResult(SUCCESS, "Component with name '" + name + "' has not been found");
    }

    public XResultDTO disableComponent(final long id) {
        if (scr == null) {
            logger.atWarn().msg(serviceUnavailable(SCR)).log();
            return createResult(SKIPPED, serviceUnavailable(SCR));
        }
        final StringBuilder                       builder         = new StringBuilder();
        final Collection<ComponentDescriptionDTO> descriptionDTOs = scr.getComponentDescriptionDTOs();

        for (final ComponentDescriptionDTO dto : descriptionDTOs) {
            final Collection<ComponentConfigurationDTO> configurationDTOs = scr.getComponentConfigurationDTOs(dto);
            for (final ComponentConfigurationDTO configDTO : configurationDTOs) {
                if (configDTO.id == id) {
                    try {
                        final Promise<Void> promise = scr.disableComponent(dto);
                        promise.getValue();
                    } catch (final Exception e) {
                        builder.append(e.getMessage()).append(System.lineSeparator());
                    }
                    final String response = builder.toString();
                    return response.isEmpty()
                            ? createResult(SUCCESS, "Component with id '" + id + "' has been successfully disabled")
                            : createResult(ERROR, response);
                }
            }
        }
        return createResult(SUCCESS, "Component with id '" + id + "' has not been found");
    }

    public XResultDTO disableComponent(final String name) {
        if (scr == null) {
            logger.atWarn().msg(serviceUnavailable(SCR)).log();
            return createResult(SKIPPED, serviceUnavailable(SCR));
        }
        final StringBuilder                       builder         = new StringBuilder();
        final Collection<ComponentDescriptionDTO> descriptionDTOs = scr.getComponentDescriptionDTOs();

        for (final ComponentDescriptionDTO dto : descriptionDTOs) {
            if (dto.name.equals(name)) {
                try {
                    final Promise<Void> promise = scr.disableComponent(dto);
                    promise.getValue();
                } catch (final Exception e) {
                    builder.append(e.getMessage()).append(System.lineSeparator());
                }
                final String response = builder.toString();
                return response.isEmpty()
                        ? createResult(SUCCESS, "Component with name '" + name + "' has been successfully disabled")
                        : createResult(ERROR, response);
            }
        }
        return createResult(SUCCESS, "Component with name '" + name + "' has not been found");
    }

    private XComponentDTO toDTO(final ComponentConfigurationDTO compConfDTO,
                                final ComponentDescriptionDTO compDescDTO) {
        final XComponentDTO  dto      = new XComponentDTO();
        final XBundleInfoDTO bInfoDTO = new XBundleInfoDTO();

        bInfoDTO.id           = compDescDTO.bundle.id;
        bInfoDTO.symbolicName = compDescDTO.bundle.symbolicName;

        dto.id                  = Optional.ofNullable(compConfDTO).map(a -> a.id).orElse(-1L);
        dto.name                = compDescDTO.name;
        dto.state               = Optional.ofNullable(compConfDTO).map(a -> mapToState(a.state)).orElse("DISABLED");
        dto.registeringBundle   = bInfoDTO.symbolicName;
        dto.registeringBundleId = bInfoDTO.id;
        dto.factory             = compDescDTO.factory;
        dto.scope               = compDescDTO.scope;
        dto.implementationClass = compDescDTO.implementationClass;
        dto.configurationPolicy = compDescDTO.configurationPolicy;
        dto.serviceInterfaces   = Stream.of(compDescDTO.serviceInterfaces).collect(toList());
        dto.configurationPid    = Stream.of(compDescDTO.configurationPid).collect(toList());

        // @formatter:off
        dto.properties          = Optional.ofNullable(compConfDTO)
                                          .map(a -> a.properties.entrySet().stream()
                                          .collect(toMap(Map.Entry::getKey, e -> arrayToString(e.getValue()))))
                                          .orElse(emptyMap());
        dto.references          = Stream.of(compDescDTO.references).map(this::toRef).collect(toList());
       // @formatter:on

        final String failure = getR7field(compConfDTO, "failure");
        dto.failure = Optional.ofNullable(failure).orElse("");

        dto.activate   = compDescDTO.activate;
        dto.deactivate = compDescDTO.deactivate;
        dto.modified   = compDescDTO.modified;

        // @formatter:off
        dto.satisfiedReferences   = Stream.of(
                                        Optional.ofNullable(compConfDTO)
                                                .map(a -> a.satisfiedReferences)
                                                .orElse(new SatisfiedReferenceDTO[0]))
                                          .map(this::toXS)
                                          .collect(toList());

        dto.unsatisfiedReferences = Stream.of(
                                        Optional.ofNullable(compConfDTO)
                                                .map(a -> a.unsatisfiedReferences)
                                                .orElse(new UnsatisfiedReferenceDTO[0]))
                                          .map(this::toXUS)
                                          .collect(toList());
        // @formatter:on

        return dto;
    }

    private static String mapToState(final int state) {
        switch (state) {
            case ACTIVE:
                return "ACTIVE";
            case SATISFIED:
                return "SATISFIED";
            case UNSATISFIED_REFERENCE:
                return "UNSATISFIED_REFERENCE";
            case UNSATISFIED_CONFIGURATION:
                return "UNSATISFIED_CONFIGURATION";
            case 16: // ComponentConfigurationDTO.FAILED_ACTIVATION OSGi R7
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
            if (dto != null) {
                final String[] objectClass = (String[]) dto.properties.get(OBJECTCLASS);
                finalList.addAll(Arrays.asList(objectClass));
            }
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

        ref.name          = reference.name;
        ref.interfaceName = reference.interfaceName;
        ref.cardinality   = reference.cardinality;
        ref.policy        = reference.policy;
        ref.policyOption  = reference.policyOption;
        ref.target        = reference.target;
        ref.bind          = reference.bind;
        ref.unbind        = reference.unbind;
        ref.updated       = reference.updated;
        ref.field         = reference.field;
        ref.fieldOption   = reference.fieldOption;
        ref.scope         = reference.scope;

        final Integer parameter = getR7field(reference, "parameter");
        ref.parameter = parameter;

        final String collectionType = getR7field(reference, "collectionType");
        ref.collectionType = collectionType;

        return ref;
    }

    private <T> T getR7field(final Object object, final String fieldName) {
        if (object == null) {
            return null;
        }
        try {
            return Reflect.on(object).field(fieldName).get();
        } catch (final Exception e) {
            return null;
        }
    }

}
