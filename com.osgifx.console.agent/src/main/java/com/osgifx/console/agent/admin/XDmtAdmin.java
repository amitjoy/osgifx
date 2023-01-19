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
import static com.osgifx.console.agent.helper.OSGiCompendiumService.DMT;
import static org.osgi.service.dmt.DmtSession.LOCK_TYPE_EXCLUSIVE;
import static org.osgi.service.dmt.MetaNode.CMD_GET;
import static org.osgi.service.dmt.Uri.PATH_SEPARATOR;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.osgi.service.dmt.DmtAdmin;
import org.osgi.service.dmt.DmtData;
import org.osgi.service.dmt.DmtException;
import org.osgi.service.dmt.DmtSession;
import org.osgi.service.dmt.MetaNode;

import com.osgifx.console.agent.dto.DmtDataType;
import com.osgifx.console.agent.dto.XDmtNodeDTO;
import com.osgifx.console.agent.dto.XResultDTO;

import jakarta.inject.Inject;

public final class XDmtAdmin {

    private XDmtNodeDTO    parent;
    private final DmtAdmin dmtAdmin;

    @Inject
    public XDmtAdmin(final Object dmtAdmin) {
        this.dmtAdmin = (DmtAdmin) dmtAdmin;
    }

    public XDmtNodeDTO readDmtNode(final String rootURI) {
        if (dmtAdmin == null) {
            return null;
        }
        processNode(rootURI, parent);
        return parent;
    }

    public XResultDTO updateDmtNode(final String uri, final Object value, final DmtDataType format) {
        if (dmtAdmin == null) {
            return createResult(SKIPPED, serviceUnavailable(DMT));
        }
        DmtSession session = null;
        try {
            session = dmtAdmin.getSession(uri, LOCK_TYPE_EXCLUSIVE);
            if (!session.isNodeUri(uri) || !session.isLeafNode(uri)) {
                return createResult(SKIPPED, "The specified URI is not associated with a leaf node");
            }
            final MetaNode metaNode = session.getMetaNode(uri);
            if (metaNode == null) {
                return createResult(SKIPPED, "Unable to retrieve meta info of the node");
            }
            return updateNode(session, uri, value, format);
        } catch (final Exception e) {
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

    private void processNode(final String uri, final XDmtNodeDTO parent) {
        try {
            final DmtSession session = dmtAdmin.getSession(uri, LOCK_TYPE_EXCLUSIVE);
            if (!session.isNodeUri(uri)) {
                return;
            }
            if (parent == null) {
                // it's a root node
                this.parent = createNode(session, uri, null, true);
            }
            final XDmtNodeDTO startNode     = parent == null ? this.parent : createNode(session, uri, parent, false);
            final String[]    childrenNodes = session.getChildNodeNames(uri);
            session.close();
            for (final String childNode : childrenNodes) {
                final String childPath = uri.isEmpty() ? childNode : uri + PATH_SEPARATOR + childNode;
                processNode(childPath, startNode);
            }
        } catch (final Exception e) {
            // nothing to do
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
            // nothing to do
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
