/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
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
import static com.osgifx.console.agent.helper.OSGiCompendiumService.DMT;
import static org.osgi.service.dmt.DmtSession.LOCK_TYPE_EXCLUSIVE;
import static org.osgi.service.dmt.MetaNode.CMD_GET;
import static org.osgi.service.dmt.Uri.PATH_SEPARATOR;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import org.osgi.service.dmt.DmtAdmin;
import org.osgi.service.dmt.DmtData;
import org.osgi.service.dmt.DmtException;
import org.osgi.service.dmt.DmtSession;
import org.osgi.service.dmt.MetaNode;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.dto.DmtDataType;
import com.osgifx.console.agent.dto.XDmtNodeDTO;
import com.osgifx.console.agent.dto.XResultDTO;

import jakarta.inject.Inject;

public final class XDmtAdmin {

    private final Supplier<Object> dmtAdminSupplier;
    private final FluentLogger     logger = LoggerFactory.getFluentLogger(getClass());

    @Inject
    public XDmtAdmin(final Supplier<Object> dmtAdminSupplier) {
        this.dmtAdminSupplier = dmtAdminSupplier;
    }

    public XDmtNodeDTO readDmtNode(final String rootURI) {
        final DmtAdmin dmtAdmin = (DmtAdmin) dmtAdminSupplier.get();
        if (dmtAdmin == null) {
            logger.atWarn().msg(serviceUnavailable(DMT)).log();
            return null;
        }
        DmtSession session = null;
        try {
            session = dmtAdmin.getSession(rootURI, LOCK_TYPE_EXCLUSIVE);
            if (!session.isNodeUri(rootURI)) {
                return null;
            }
            final XDmtNodeDTO rootNode = createNode(session, rootURI, null, true);
            processNode(session, rootURI, rootNode);
            return rootNode;
        } catch (final Exception e) {
            logger.atError().msg("Error occurred while reading DMT node").throwable(e).log();
            return null;
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (final DmtException e) {
                    // nothing to do
                }
            }
        }
    }

    public XResultDTO updateDmtNode(final String uri, final Object value, final DmtDataType format) {
        final DmtAdmin dmtAdmin = (DmtAdmin) dmtAdminSupplier.get();
        if (dmtAdmin == null) {
            logger.atWarn().msg(serviceUnavailable(DMT)).log();
            return createResult(SKIPPED, serviceUnavailable(DMT));
        }
        DmtSession session = null;
        try {
            session = dmtAdmin.getSession(uri, LOCK_TYPE_EXCLUSIVE);
            if (!session.isNodeUri(uri) || !session.isLeafNode(uri)) {
                logger.atInfo().msg("The specified URI '%s' is not associated with a leaf node").arg(uri).log();
                return createResult(SKIPPED, "The specified URI is not associated with a leaf node");
            }
            final MetaNode metaNode = session.getMetaNode(uri);
            if (metaNode == null) {
                logger.atInfo().msg("Unable to retrieve meta info of the node - '%s'").arg(uri).log();
                return createResult(SKIPPED, "Unable to retrieve meta info of the node");
            }
            return updateNode(session, uri, value, format);
        } catch (final Exception e) {
            logger.atError().msg("Error occurred while updating DMT node").throwable(e).log();
            return createResult(ERROR, "The DMT node cannot be updated");
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (final DmtException e) {
                    // nothing to do
                }
            }
        }
    }

    private XResultDTO updateNode(final DmtSession session,
                                  final String uri,
                                  final Object value,
                                  final DmtDataType format) throws DmtException {
        session.setNodeValue(uri, convertValue(value, format));
        return createResult(SUCCESS, "DMT node has been successfully updated");
    }

    private DmtData convertValue(final Object value, final DmtDataType format) {
        switch (format) {
            case BOOLEAN:
                return new DmtData(Boolean.parseBoolean(value.toString()));
            case FLOAT:
                return new DmtData(Float.parseFloat(value.toString()));
            case INTEGER:
                return new DmtData(Integer.parseInt(value.toString()));
            case LONG:
                return new DmtData(Long.parseLong(value.toString()));
            case STRING:
            case DATE:
            case DATE_TIME:
            case XML:
            case TIME:
                return new DmtData(value.toString());
            case BASE64:
            case BINARY:
            case NULL:
            default:
                return null;
        }
    }

    private void processNode(final DmtSession session, final String uri, final XDmtNodeDTO parent) {
        try {
            if (!session.isNodeUri(uri)) {
                return;
            }
            final String[] childrenNodes = session.getChildNodeNames(uri);
            for (final String childNode : childrenNodes) {
                final String      childPath = uri.isEmpty() ? childNode : uri + PATH_SEPARATOR + childNode;
                final XDmtNodeDTO childDTO  = createNode(session, childPath, parent, false);
                processNode(session, childPath, childDTO);
            }
        } catch (final Exception e) {
            logger.atError().msg("Error occurred").throwable(e).log();
        }
    }

    private XDmtNodeDTO createNode(final DmtSession session,
                                   final String uri,
                                   final XDmtNodeDTO parent,
                                   final boolean isRoot) throws DmtException {
        final List<String> data = extractData(session, uri, isRoot);
        final XDmtNodeDTO  node = new XDmtNodeDTO();

        node.uri       = uri;
        node.value     = data.get(0);
        node.format    = dataType(data.get(1));
        node.createdAt = data.get(2);
        node.children  = new ArrayList<>();

        if (parent != null) {
            parent.children.add(node);
        }
        return node;
    }

    private List<String> extractData(final DmtSession session, String uri, final boolean isRoot) throws DmtException {
        final List<String> data = new ArrayList<>();

        uri = isRoot ? "" : uri;
        String format    = null;
        String value     = null;
        Date   createdAt = null;
        try {
            if (session.isLeafNode(uri)) {
                final MetaNode metaNode = session.getMetaNode(uri);

                if (metaNode != null && metaNode.can(CMD_GET)) {
                    final DmtData dmtValue = session.getNodeValue(uri);
                    format = dmtValue.getFormatName();
                    value  = dmtValue.toString();
                }
            }
            createdAt = session.getNodeTimestamp(uri);
        } catch (final Exception e) {
            logger.atError().msg("Error occurred").throwable(e).log();
        }
        data.add(value);
        data.add(format);
        data.add(createdAt != null ? createdAt.toString() : null);
        return data;
    }

    private DmtDataType dataType(final String name) {
        for (final DmtDataType e : DmtDataType.values()) {
            if (e.name().equalsIgnoreCase(name)) {
                return e;
            }
        }
        return null;
    }

}
