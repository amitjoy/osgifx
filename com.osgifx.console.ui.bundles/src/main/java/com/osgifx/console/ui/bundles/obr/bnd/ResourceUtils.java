/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
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
package com.osgifx.console.ui.bundles.obr.bnd;

import static java.lang.invoke.MethodHandles.publicLookup;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.osgi.framework.namespace.AbstractWiringNamespace;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.namespace.contract.ContractNamespace;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.osgi.Constants;
import aQute.lib.converter.Converter;
import aQute.lib.filter.Filter;
import aQute.lib.strings.Strings;

public class ResourceUtils {

	public static final String CONTENT_NAMESPACE        = "osgi.content";
	public static final String CAPABILITY_URL_ATTRIBUTE = "url";

	/**
	 * A comparator that compares the identity versions
	 */
	public static final Comparator<? super Resource> IDENTITY_VERSION_COMPARATOR = //
	        (o1, o2) -> {
		        if (o1 == o2) {
			        return 0;
		        }

		        if (o1 == null) {
			        return -1;
		        }

		        if (o2 == null) {
			        return 1;
		        }

		        if (o1.equals(o2)) {
			        return 0;
		        }

		        final var v1 = getIdentityVersion(o1);
		        final var v2 = getIdentityVersion(o2);

		        if (v1 == v2) {
			        return 0;
		        }

		        if (v1 == null) {
			        return -1;
		        }

		        if (v2 == null) {
			        return 1;
		        }

		        return new Version(v1).compareTo(new Version(v2));
	        };

	private static final Comparator<? super Resource> RESOURCE_COMPARATOR = //
	        (o1, o2) -> {
		        if (o1 == o2) {
			        return 0;
		        }

		        if (o1 == null) {
			        return -1;
		        }
		        if (o2 == null) {
			        return 1;
		        }

		        if (o1.equals(o2)) {
			        return 0;
		        }

		        if (o1 instanceof ResourceImpl && o2 instanceof ResourceImpl) {
			        return ((ResourceImpl) o1).compareTo(o2);
		        }

		        return o1.toString().compareTo(o2.toString());
	        };

	public static final Resource DUMMY_RESOURCE      = new ResourceBuilder().build();
	public static final String   WORKSPACE_NAMESPACE = "bnd.workspace.project";

	private static final Converter cnv = new Converter().hook(Version.class, (dest, o) -> toVersion(o));

	public interface IdentityCapability extends Capability {
		public enum Type {
			bundle(IdentityNamespace.TYPE_BUNDLE), fragment(IdentityNamespace.TYPE_FRAGMENT), unknown(IdentityNamespace.TYPE_UNKNOWN);

			private final String s;

			Type(final String s) {
				this.s = s;
			}

			@Override
			public String toString() {
				return s;
			}

		}

		String osgi_identity();

		boolean singleton();

		Version version();

		Type type();

		URI uri();

		String copyright();

		String description(String string);

		String documentation();

		String license();
	}

	public interface ContentCapability extends Capability {
		String osgi_content();

		URI url();

		long size();

		String mime();
	}

	public interface BundleCap extends Capability {
		String osgi_wiring_bundle();

		boolean singleton();

		Version bundle_version();
	}

	private static Stream<Capability> capabilityStream(final Resource resource, final String namespace) {
		return resource.getCapabilities(namespace).stream();
	}

	private static <T extends Capability> Stream<T> capabilityStream(final Resource resource, final String namespace, final Class<T> type) {
		return capabilityStream(resource, namespace).map(c -> as(c, type));
	}

	public static ContentCapability getContentCapability(final Resource resource) {
		return capabilityStream(resource, CONTENT_NAMESPACE, ContentCapability.class).findFirst().orElse(null);
	}

	public static Optional<URI> getURI(final Resource resource) {
		return capabilityStream(resource, CONTENT_NAMESPACE, ContentCapability.class).findFirst().map(ContentCapability::url);
	}

	public static List<ContentCapability> getContentCapabilities(final Resource resource) {
		return capabilityStream(resource, CONTENT_NAMESPACE, ContentCapability.class).collect(toList());
	}

	public static IdentityCapability getIdentityCapability(final Resource resource) {
		return capabilityStream(resource, IdentityNamespace.IDENTITY_NAMESPACE, IdentityCapability.class).findFirst().orElse(null);
	}

	public static String getIdentityVersion(final Resource resource) {
		return capabilityStream(resource, IdentityNamespace.IDENTITY_NAMESPACE, IdentityCapability.class).findFirst()
		        .map(c -> c.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE)).map(Object::toString).orElse(null);
	}

	public static BundleCap getBundleCapability(final Resource resource) {
		return capabilityStream(resource, BundleNamespace.BUNDLE_NAMESPACE, BundleCap.class).findFirst().orElse(null);
	}

	public static Version toVersion(final Object v) {
		if (v instanceof Version) {
			return (Version) v;
		}

		if (v instanceof final org.osgi.framework.Version o) {
			final var q = o.getQualifier();
			return q.isEmpty() ? new Version(o.getMajor(), o.getMinor(), o.getMicro())
			        : new Version(o.getMajor(), o.getMinor(), o.getMicro(), q);
		}

		if (v instanceof String && Version.isVersion((String) v)) {
			return Version.valueOf((String) v);
		}

		return null;
	}

	public static final Version getVersion(final Capability cap) {
		final var attr = getVersionAttributeForNamespace(cap.getNamespace());
		if (attr == null) {
			return null;
		}
		final var v = cap.getAttributes().get(attr);
		return toVersion(v);
	}

	public static URI getURI(final Capability contentCapability) {
		final var uriObj = contentCapability.getAttributes().get(CAPABILITY_URL_ATTRIBUTE);
		if (uriObj == null) {
			return null;
		}

		if (uriObj instanceof URI) {
			return (URI) uriObj;
		}

		try {
			if (uriObj instanceof URL) {
				return ((URL) uriObj).toURI();
			}

			if (uriObj instanceof String) {
				try {
					final var url = new URL((String) uriObj);
					return url.toURI();
				} catch (final MalformedURLException mfue) {
					// Ignore
				}

				final var f = new File((String) uriObj);
				if (f.isFile()) {
					return f.toURI();
				}
				return new URI((String) uriObj);
			}

		} catch (final URISyntaxException e) {
			throw new IllegalArgumentException("Resource content capability has illegal URL attribute", e);
		}

		return null;
	}

	public static String getVersionAttributeForNamespace(final String namespace) {
		switch (namespace) {
		case IdentityNamespace.IDENTITY_NAMESPACE:
			return IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE;
		case BundleNamespace.BUNDLE_NAMESPACE:
		case HostNamespace.HOST_NAMESPACE:
			return AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
		case PackageNamespace.PACKAGE_NAMESPACE:
			return PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE;
		case ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE:
			return ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE;
		case NativeNamespace.NATIVE_NAMESPACE:
			return NativeNamespace.CAPABILITY_OSVERSION_ATTRIBUTE;
		case ExtenderNamespace.EXTENDER_NAMESPACE:
			return ExtenderNamespace.CAPABILITY_VERSION_ATTRIBUTE;
		case ContractNamespace.CONTRACT_NAMESPACE:
			return ContractNamespace.CAPABILITY_VERSION_ATTRIBUTE;
		case ImplementationNamespace.IMPLEMENTATION_NAMESPACE:
			return ImplementationNamespace.CAPABILITY_VERSION_ATTRIBUTE;
		case ServiceNamespace.SERVICE_NAMESPACE:
		default:
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends Capability> T as(final Capability cap, final Class<T> type) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
		        (target, method, args) -> Capability.class == method.getDeclaringClass()
		                ? publicLookup().unreflect(method).bindTo(cap).invokeWithArguments(args)
		                : get(method, cap.getAttributes(), cap.getDirectives(), args));
	}

	@SuppressWarnings("unchecked")
	public static <T extends Requirement> T as(final Requirement req, final Class<T> type) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
		        (target, method, args) -> Requirement.class == method.getDeclaringClass()
		                ? publicLookup().unreflect(method).bindTo(req).invokeWithArguments(args)
		                : get(method, req.getAttributes(), req.getDirectives(), args));
	}

	private static Object get(final Method method, final Map<String, Object> attrs, final Map<String, String> directives,
	        final Object[] args) throws Exception {
		final var name = method.getName().replace('_', '.');

		Object value;
		if (name.startsWith("$")) {
			value = directives.get(name.substring(1));
		} else {
			value = attrs.get(name);
		}
		if (value == null && args != null && args.length == 1) {
			value = args[0];
		}

		return cnv.convert(method.getGenericReturnType(), value);
	}

	public static Set<Resource> getResources(final Collection<? extends Capability> providers) {
		if (providers == null || providers.isEmpty()) {
			return Collections.emptySet();
		}

		return getResources(providers.stream());
	}

	private static Set<Resource> getResources(final Stream<? extends Capability> providers) {
		return providers.map(Capability::getResource).collect(toCollection(() -> new TreeSet<>(RESOURCE_COMPARATOR)));
	}

	public static boolean isEffective(final Requirement r, final Capability c) {
		final var capabilityEffective = c.getDirectives().get(Namespace.CAPABILITY_EFFECTIVE_DIRECTIVE);

		//
		// resolve on the capability will always match any
		// requirement effective
		//

		if (capabilityEffective == null || Namespace.EFFECTIVE_RESOLVE.equals(capabilityEffective)) {
			return true;
		}

		final var requirementEffective = r.getDirectives().get(Namespace.CAPABILITY_EFFECTIVE_DIRECTIVE);

		//
		// If requirement is resolve but capability isn't
		//

		if (requirementEffective == null) {
			return false;
		}

		return capabilityEffective.equals(requirementEffective);
	}

	public static boolean matches(final Requirement requirement, final Resource resource) {
		return capabilityStream(resource, requirement.getNamespace()).anyMatch(c -> matches(requirement, c));
	}

	public static boolean matches(final Requirement requirement, final Capability capability) {
		if (!requirement.getNamespace().equals(capability.getNamespace()) || !isEffective(requirement, capability)) {
			return false;
		}

		final var filter = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		if (filter == null) {
			return true;
		}

		try {
			final var f = new Filter(filter);
			return f.matchMap(capability.getAttributes());
		} catch (final Exception e) {
			return false;
		}
	}

	public static String getEffective(final Map<String, String> directives) {
		final var effective = directives.get(Namespace.CAPABILITY_EFFECTIVE_DIRECTIVE);
		if (effective == null) {
			return Namespace.EFFECTIVE_RESOLVE;
		}
		return effective;
	}

	public static Map<URI, String> getLocations(final Resource resource) {
		return capabilityStream(resource, CONTENT_NAMESPACE, ContentCapability.class).filter(c -> Objects.nonNull(c.url()))
		        .collect(Collector.of(HashMap::new, (m, c) -> m.put(c.url(), c.osgi_content()), (m1, m2) -> {
			        m1.putAll(m2);
			        return m1;
		        }));
	}

	public static List<Capability> findProviders(final Requirement requirement, final Collection<? extends Capability> capabilities) {
		return capabilities.stream().filter(c -> matches(requirement, c)).collect(toList());
	}

	public static boolean isFragment(final Resource resource) {
		final var identity = getIdentityCapability(resource);
		if (identity == null) {
			return false;
		}
		return IdentityNamespace.TYPE_FRAGMENT.equals(identity.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
	}

	public static String stripDirective(final String name) {
		if (Strings.charAt(name, -1) == ':') {
			return Strings.substring(name, 0, -1);
		}
		return name;
	}

	public static String getIdentity(final Capability identityCapability) throws IllegalArgumentException {
		final var id = (String) identityCapability.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
		if (id == null) {
			throw new IllegalArgumentException("Resource identity capability has missing identity attribute");
		}
		return id;
	}

	/**
	 * Compare two resources. This can be used to act as a comparator. The
	 * comparison is first done on name and then version.
	 *
	 * @param a the left resource
	 * @param b the right resource
	 * @return 0 if equal bame and version, 1 if left has a higher name or same name
	 *         and higher version, -1 otherwise
	 */
	public static int compareTo(final Resource a, final Resource b) {
		final var left  = ResourceUtils.getIdentityCapability(a);
		final var right = ResourceUtils.getIdentityCapability(b);

		final var myName    = left.osgi_identity();
		final var theirName = right.osgi_identity();
		if (myName == theirName) {
			return 0;
		}

		if (myName == null) {
			return -1;
		}

		if (theirName == null) {
			return 1;
		}

		final var n = myName.compareTo(theirName);
		if (n != 0) {
			return n;
		}

		final var myVersion    = left.version();
		final var theirVersion = right.version();

		if (myVersion == theirVersion) {
			return 0;
		}

		if (myVersion == null) {
			return -1;
		}

		if (theirVersion == null) {
			return 1;
		}

		return myVersion.compareTo(theirVersion);
	}

	public static List<Resource> sort(final Collection<Resource> a) {
		final List<Resource> list = new ArrayList<>(a);
		Collections.sort(list, ResourceUtils::compareTo);
		return list;
	}

	/**
	 * Sort the resources by symbolic name and version
	 *
	 * @param resources the set of resources to sort
	 * @return a sorted set of resources
	 */
	public static List<Resource> sortByNameVersion(final Collection<Resource> resources) {
		final var sorted = new ArrayList<>(resources);
		Collections.sort(sorted, ResourceUtils::compareTo);
		return sorted;
	}

	public static boolean isInitialRequirement(final Resource resource) {
		final var identityCapability = getIdentityCapability(resource);
		if (identityCapability == null) {
			return false;
		}

		final var osgi_identity = identityCapability.osgi_identity();
		if (osgi_identity == null) {
			return false;
		}

		return Constants.IDENTITY_INITIAL_RESOURCE.equals(osgi_identity);
	}

}
