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
import static com.osgifx.console.agent.helper.OSGiCompendiumService.USER_ADMIN;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.agent.dto.XRoleDTO;
import com.osgifx.console.agent.dto.XRoleDTO.Type;
import com.osgifx.console.agent.helper.AgentHelper;

import jakarta.inject.Inject;

public final class XUserAdmin {

    private final UserAdmin userAdmin;

    @Inject
    public XUserAdmin(final Object userAdmin) {
        this.userAdmin = (UserAdmin) userAdmin;
    }

    public List<XRoleDTO> getRoles() {
        if (userAdmin == null) {
            return Collections.emptyList();
        }
        final List<XRoleDTO> dtos = new ArrayList<>();
        try {
            for (final Role role : userAdmin.getRoles(null)) {
                dtos.add(toRole(role));
            }
        } catch (final Exception e) {
            // for any exception occurs in remote runtime
            return Collections.emptyList();
        }
        return dtos;
    }

    public XResultDTO createRole(final String name, final Type type) {
        if (userAdmin == null) {
            return createResult(SKIPPED, serviceUnavailable(USER_ADMIN));
        }
        final Role role = userAdmin.createRole(name, getType(type));
        if (role != null) {
            return createResult(SUCCESS, "The role '" + name + "' has been created successfully");
        }
        return createResult(ERROR, "The role '" + name + "' could not be created");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public XResultDTO updateRole(final XRoleDTO roleDTO) {
        if (userAdmin == null) {
            return createResult(SKIPPED, serviceUnavailable(USER_ADMIN));
        }
        try {
            final Role role = getRole(roleDTO);
            if (role == null) {
                return createResult(ERROR, "The role '" + roleDTO.name + "' could not be found");
            }
            try {
                cyclesCache.add(roleDTO.name);

                final List<XRoleDTO> allMembers        = mergeMembers(roleDTO.basicMembers, roleDTO.requiredMembers);
                final boolean        hasCycleInMembers = hasCycleInMembers(allMembers);

                if (hasCycleInMembers) {
                    return createResult(ERROR, "Cyclic members found while updating '" + roleDTO.name + "'");
                }
            } finally {
                cyclesCache.clear();
            }
            // update properties
            final Dictionary          properties    = role.getProperties();
            final Map<String, Object> newProperties = roleDTO.properties;
            if (newProperties != null) {
                clear(properties);
                newProperties.forEach(properties::put);
            }
            // update credentials if user
            if (roleDTO.type == Type.USER) {
                final Dictionary          credentials    = ((User) role).getCredentials();
                final Map<String, Object> newCredentials = roleDTO.credentials;
                if (newCredentials != null) {
                    clear(credentials);
                    newCredentials.forEach(credentials::put);
                }
            }
            // update basic members if group
            if (roleDTO.type == Type.GROUP) {
                final Group  group   = (Group) role;
                final Role[] members = group.getMembers();
                if (members != null) {
                    // remove everything first
                    Stream.of(members).forEach(group::removeMember);
                }
                final List<XRoleDTO> newBasicMembers = roleDTO.basicMembers;
                if (newBasicMembers != null) {
                    newBasicMembers.forEach(m -> {
                        final Role r = getRole(m);
                        if (r != null) {
                            group.addMember(r);
                        }
                    });
                }
            }
            // update required members if group
            if (roleDTO.type == Type.GROUP) {
                final Group  group           = (Group) role;
                final Role[] requiredMembers = group.getRequiredMembers();
                if (requiredMembers != null) {
                    // remove everything first
                    Stream.of(requiredMembers).forEach(group::removeMember);
                }
                final List<XRoleDTO> newRequiredMembers = roleDTO.requiredMembers;
                if (newRequiredMembers != null) {
                    newRequiredMembers.forEach(m -> {
                        final Role r = getRole(m);
                        if (r != null) {
                            group.addRequiredMember(r);
                        }
                    });
                }
            }
            return createResult(SUCCESS, "The role '" + roleDTO.name + "' has been updated successfully");
        } catch (final Exception e) {
            return createResult(ERROR, "The role '" + roleDTO.name + "' could not be updated");
        }
    }

    List<String> cyclesCache = new ArrayList<>();

    private boolean hasCycleInMembers(final List<XRoleDTO> members) {
        if (members == null) {
            return false;
        }
        for (final XRoleDTO role : members) {
            if (cyclesCache.contains(role.name)) {
                return true;
            }
            cyclesCache.add(role.name);

            final List<XRoleDTO> allMembers = mergeMembers(role.basicMembers, role.requiredMembers);
            final boolean        isCycle    = hasCycleInMembers(allMembers);

            if (isCycle) {
                return true;
            }
        }
        return false;
    }

    public XResultDTO removeRole(final String name) {
        if (userAdmin == null) {
            return createResult(SKIPPED, serviceUnavailable(USER_ADMIN));
        }
        final boolean isRemoved = userAdmin.removeRole(name);
        return isRemoved ? createResult(SUCCESS, "The role '" + name + "' has been removed successfully")
                : createResult(ERROR, "The role '" + name + "' could not be removed");
    }

    private Role getRole(final XRoleDTO role) {
        return userAdmin.getRole(role.name);
    }

    private int getType(final Type type) {
        if (type == Type.USER) {
            return Role.USER;
        }
        if (type == Type.GROUP) {
            return Role.GROUP;
        }
        return Role.ROLE;
    }

    @SuppressWarnings("unchecked")
    private XRoleDTO toRole(final Role role) {
        final XRoleDTO roleDTO = new XRoleDTO();

        roleDTO.name       = role.getName();
        roleDTO.type       = toType(role.getType());
        roleDTO.properties = AgentHelper.valueOf(role.getProperties());

        if (roleDTO.type == Type.GROUP) {
            roleDTO.basicMembers    = toBasicMembers(role);
            roleDTO.requiredMembers = toRequiredMembers(role);
        }
        if (roleDTO.type == Type.USER) {
            roleDTO.credentials = AgentHelper.valueOf(((User) role).getCredentials());
        }
        return roleDTO;
    }

    private List<XRoleDTO> toBasicMembers(final Role role) {
        final Role[] basicMembers = ((Group) role).getMembers();
        if (basicMembers == null) {
            return null;
        }
        return Stream.of(basicMembers).map(this::toRole).collect(toList());
    }

    private List<XRoleDTO> toRequiredMembers(final Role role) {
        final Role[] requiredMembers = ((Group) role).getRequiredMembers();
        if (requiredMembers == null) {
            return null;
        }
        return Stream.of(requiredMembers).map(this::toRole).collect(toList());
    }

    private Type toType(final int type) {
        switch (type) {
            case 1:
                return Type.USER;
            case 2:
                return Type.GROUP;
            default:
                return Type.DEFAULT;
        }
    }

    private List<XRoleDTO> mergeMembers(final List<XRoleDTO> basicMembers, final List<XRoleDTO> requiredMembers) {
        // @formatter:off
        return Stream.of(basicMembers, requiredMembers)
                     .filter(Objects::nonNull)
                     .flatMap(Collection::stream)
                     .collect(toList());
        // @formatter:on
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void clear(final Dictionary dictionary) {
        Collections.list(dictionary.keys()).forEach(dictionary::remove);
    }

}
