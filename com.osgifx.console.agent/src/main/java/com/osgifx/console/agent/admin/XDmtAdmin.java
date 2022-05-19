package com.osgifx.console.agent.admin;

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

import com.osgifx.console.agent.dto.XDmtNodeDTO;

public final class XDmtAdmin {

	private XDmtNodeDTO parent;

	private final DmtAdmin dmtAdmin;

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

	private XDmtNodeDTO createNode(final DmtSession session, final String uri, final XDmtNodeDTO parent, final boolean isRoot)
	        throws DmtException {
		final List<String> data = extractData(session, uri, isRoot);
		final XDmtNodeDTO  node = new XDmtNodeDTO();

		node.uri       = uri;
		node.value     = data.get(0);
		node.format    = data.get(1);
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

}
