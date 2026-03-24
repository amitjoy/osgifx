---
layout: default
title: Agent
permalink: /agent
---

# OSGi.fx Remote Agent

The **OSGi.fx Remote Agent** is a lightweight, high-performance management agent designed to run inside a target OSGi framework. It acts as the bridge between the remote runtime and the **OSGi.fx Desktop Console**, enabling deep introspection, real-time diagnostics, and lifecycle management of OSGi systems.


## 📋 Requirements

The agent is designed for broad compatibility across legacy and modern environments:
- **Java**: Version **1.8** or higher (tested up to Java 25).
- **OSGi**: Core R6 or higher (Equinox, Felix, Concierge, etc.).

---

## 🚀 Key Advantages

| Feature | Description |
| :--- | :--- |
| **High Performance** | Utilizes binary serialization (`BinaryCodec`) and LZ4 compression to minimize network traffic and heap occupancy. |
| **Reactive Snapshotting** | Implements a debounced, deadline-aware snapshotting mechanism that only updates data when the framework state changes. |
| **Transport Agnostic** | Supports communication over standard **Sockets** (TCP/TLS) or **MQTT 5** (OSGi Messaging or Custom). |
| **Graceful Degradation** | Operates on minimal runtimes by dynamically detecting available services (ConfigAdmin, SCR, etc.) without requiring them. |
| **Security First** | Built-in support for password authentication and TLS-secured communication. |

---

## 🏗️ Module Architecture

The agent is split into three OSGi bundles for clean separation of concerns:

- **`com.osgifx.console.agent.api`**: The architectural contract defining:
  - **`Agent` / `AgentSnapshot`**: Primary management interfaces for control and inspection
  - **`dto`**: Comprehensive DTO suite optimized for minimal serialization footprints
  - **`rpc`**: Binary codec (`BinaryCodec`), LZ4 compression (`Lz4Codec`), transport abstractions, and `SnapshotDecoder`
  - **`spi`**: Extension points (`AgentExtension`, `LargePayloadHandler`) for custom logic

- **`com.osgifx.console.agent`**: The core implementation containing:
  - **`admin`**: Domain-specific managers (Bundles, Components, Configurations, Services, Conditions, etc.)
  - **`redirector`**: I/O hijacking engine for multi-mode terminal proxies (Console, Gogo, Socket, Telnet)
  - **`provider`**: Agent server, binary log buffer, classloader leak detector, startup time calculator
  - **`starter`**: Zero-dependency `Activator` and Gogo command bridge
  - **`helper`**: Utility classes for OSGi compendium service detection and agent operations

- **`com.osgifx.console.agent.di`**: Standalone micro-DI container:
  - **JSR-330 compliant**: Full support for `@Inject`, `@Singleton`, and `Provider<T>`
  - **Lambda-accelerated**: Injection optimized via `LambdaMetafactory` for high-speed instantiation
  - **Zero dependencies**: Operates without OSGi SCR, making it portable across minimal runtimes
  - **Dynamic binding**: Runtime binding of interfaces to implementations and custom providers

---

## 🏗️ Architecture

The agent is built on a modular, DI-based architecture to ensure extensibility and clean lifecycle management.

#### 🧩 Lightweight Internal DI
To remain portable and operate even when OSGi Declarative Services (SCR) is missing, the agent uses a **bespoke, lightweight Dependency Injection container**. 
- **Standalone**: Zero dependencies on heavy DI frameworks (Guice, Spring, OSGi DS).
- **Fast**: Uses `LambdaMetafactory` for high-speed constructor and field injection.
- **Lazy Providers**: Supports `jakarta.inject.Provider` for services that may be wired later in the framework lifecycle.
- **Lifecycle Integrated**: Closely tied to the Bundle Activator for deterministic startup/shutdown of all internal "Admin" services.

#### 🛡️ Class Space Resilience
The agent is designed to be injected into existing frameworks where classloader conflicts are common:
- **Proxying Strategy**: The `GogoRedirector` and other redirection components use `java.lang.reflect.Proxy` to interact with Gogo APIs. This prevents `ClassCastException` if the agent and Gogo are in different class spaces, a common issue in complex OSGi deployments.

The agent defines its contract across two primary interfaces in the `com.osgifx.console.agent.api` bundle:

#### 1. `AgentSnapshot` (The "Inspection" Layer)
Focuses exclusively on **high-performance state retrieval**.
- **Design**: All methods return `byte[]` (pre-serialized, LZ4-compressed binary blobs).
- **Goal**: Minimize heap occupancy and serialization overhead. This is the "Monitoring" interface used by OSGi.fx to visualize the runtime.

#### 2. `Agent` (The "Control" Layer)
Focuses on **lifecycle management and runtime manipulation**.
- **Design**: Extends `AgentSnapshot`. Includes methods with side effects: `install()`, `start()`, `stop()`, `updateConfiguration()`, `sendEvent()`, etc.
- **Goal**: Act as the authority for remote management.

#### 💡 Why the separation?
This "Control vs. Inspection" split allows the agent to serve hundreds of monitoring requests (Snapshots) with near-zero CPU cost by using atoms and binary caches, while keeping the management logic (Agent) clean and focused on framework side effects.

- **The Monitoring Flow**: OSGi.fx calls `AgentSnapshot.bundles()`. The agent returns a pre-cached binary blob. No locks, no DTO creation, no serialization.
- **The Control Flow**: OSGi.fx calls `Agent.stop(id)`. The agent executes the side effect, which triggers a framework event, which in turn invalidates the `AgentSnapshot` cache for a future request.

#### 🛡️ Sub-System Self-Protection
To prevent accidental "suicide" of the management bridge or catastrophic framework failure, the agent implements strict self-protection:
- **Immutable Critical Bundles**: The `AgentServer` explicitly blocks `stop()` and `uninstall()` operations targeting the **System Bundle (ID 0)** or the **Agent Bundle itself**.
- **Execution Allowlisting**: All remote Gogo and CLI commands are verified against a property-based allowlist before invocation.
- **Path Traversal Guard**: Methods like `getBundleDataFile` and local creation commands strictly validate paths to prevent escaping the bundle's private data storage or the designated output directories.

---

### ⚡ Deep Dive: Binary Caching & Performance

The agent's state-retrieval mechanism is built for **extreme efficiency** in high-churn OSGi environments. Unlike traditional agents that serialize objects on-demand, this agent uses a **Reactive Binary Snapshot** strategy.

#### 1. Why Reactive Snapshots?
*   **CPU Rationale**: Serialization (especially of large DTO trees) is one of the most expensive operations. By snapshotting once per stable state, we move the cost from the *request path* to the *background path*.
*   **Memory Rationale**: Holding a full tree of DTO objects in memory is expensive for the GC. Instead, the agent holds a single compressed `byte[]`. This significantly reduces heap occupancy.
*   **Network Rationale**: Data is encoded using a compact binary format and further compressed with **LZ4**. This minimizes bandwidth without the high CPU overhead of GZIP.

#### 2. Hybrid Binary Serialization (`BinaryCodec`)
The agent uses a custom-built, schema-less binary codec that outperforms standard Java serialization and JSON by orders of magnitude:
*   **JIT-Inlined Raw Memory Access (With Graceful Fallback)**: Uses `sun.misc.Unsafe` wrapped seamlessly via `LambdaMetafactory`. This dynamically spins up `@FunctionalInterface` implementations at runtime invoking precise `MethodHandle`s, guaranteeing the HotSpot JIT compiler completely inlines the memory access. 
    *   **Future-Proof (`Java 25+`)**: Direct references to `sun.misc.Unsafe` break strict Gradle builds and module boundaries (`Jigsaw`) in modern JVMs. The agent strictly loads `Unsafe` dynamically via `Class.forName()`. If restricted or removed in future JVMs (like Java 25+), the agent implements a zero-exception **Standard Fallback** strategy. It gracefully degrades to standard `MethodHandle` unreflected getters/setters (`lookup.unreflectGetter()`). Crucially, these fallback handles are *also* passed through `LambdaMetafactory` to guarantee the exact same JIT inlining speed, executing natively without ever touching `sun.misc`.
*   **Internal Micro-DI Engine (`com.osgifx.console.agent.di`)**: The agent does not rely on heavy frameworks like Spring or Guice, nor does it use native OSGi SCR for its internal plumbing. It utilizes a bespoke, zero-dependency Dependency Injection engine that supports:
    *   **JSR-330 Compliance**: Full support for `@Inject`, `@Singleton`, and `Provider<T>`.
    *   **Dynamic Binding**: Runtime binding of interfaces to implementations and custom providers.
    *   **Lambda-Accelerated Injection**: Constructor and field injection are optimized to bypass standard reflection overhead where possible.
    *   **Ultra-Portable**: Operates in environments where full OSGi component runtimes are unavailable or restricted.
*   **Schema-less Payload**: Field names are never sent. Data is serialized in a deterministic order (sorted by field name), drastically reducing payload size.
*   **Zero-Boxing**: Primitive fields (int, long, double, etc.) are packed directly into the binary stream without object allocation.
*   **Non-Blocking I/O**: Utilizes internal `FastByteArray` streams that eliminate synchronization overhead and defensive array copies.

#### 3. Adaptive & Reactive Snapshotting Strategy (`AbstractSnapshotAdmin`)
To prevent the agent from thrashing the CPU during high-frequency OSGi events (e.g., a burst of 1000 service registrations), all "Admins" (Bundles, Services, Components) utilize a sophisticated debouncing mechanism:
*   **Debounce Window**: 200ms of "silence" required before a snapshot is generated.
*   **Hard Deadline**: 5000ms maximum wait. If events keep flooding in, the snapshot is forced at the deadline to ensure the Console isn't starved of data.
*   **Change-Count Synchronization**: Snapshots are only recalculated if the internal `pendingChangeCount` exceeds the `lastChangeCount`, ensuring zero CPU cycles are wasted on redundant serializations.
*   **Lazy Serialization**: Snapshots are encoded/compressed on a background thread and cached as immutable byte arrays, minimizing the duration of locks on core OSGi structures.


Most data points (Bundles, Services, Configs) are served via a thread-safe caching mechanism:

1.  **Change Detection**: The agent monitors framework change counts (e.g. `SERVICE_CHANGECOUNT`).
2.  **Debouncing (200ms)**: During "event storms" (e.g., a burst of 50 service registrations), the agent waits for a 200ms silence period before triggering a re-snapshot.
3.  **The Deadline (5s)**: If the system is in constant flux and never reaches the "silence" threshold, a snapshot is forced every 5 seconds to ensure the remote UI stays reasonably fresh.
4.  **Atomic Binary Cache**: The resulting `byte[]` is stored in an `AtomicReference`.
    *   **Direct-to-Wire**: If a supervisor requests the same data multiple times, the agent sends the *same byte array* directly to the socket/MQTT link. **Zero serialization** is performed for subsequent requests until the next framework change.

#### 📸 Snapshot Tracking Strategy

| Admin | Tracking Strategy | Trigger Event |
| :--- | :--- | :--- |
| **Bundles** | `BundleTracker` + `FrameworkListener` | Bundle lifecycle, Startlevel changes |
| **Services** | `ServiceTracker` | Service registration/modification/removal |
| **Components** | `ServiceComponentRuntime` Tracker | SCR Component state changes (`changecount`) |
| **Conditions** | `ServiceTracker` | Condition service registration/modification/removal |
| **Configurations**| `ConfigurationListener` | ConfigAdmin PID/Factory updates |
| **HTTP/JAX-RS/CDI**| `changecount` Monitoring | Remote DTO state changes via R7/R8 specs |
| **User Admin** | `UserAdminListener` | Roles, Groups, and Credential changes |
| **Loggers** | `BundleListener` | Bundle attachment (logger context availability) |
| **Health Checks** | `ServiceTracker` | HealthCheck service registration changes |
| **Threads/Props** | **Live (On-demand)** | Real-time JVM state (Too volatile to cache) |

#### 🛠️ Concrete Example: `XBundleAdmin`
Here is how a real Snapshot provider implements change detection and adaptive scheduling:

```java
@Singleton
public final class XBundleAdmin extends AbstractSnapshotAdmin<XBundleDTO> {
    // 1. Initialise trackers for the state we care about
    public void init() {
        bundleTracker = new BundleTracker<>(context, INSTALLED | ACTIVE | ...) {
            @Override
            public Object addingBundle(Bundle b, BundleEvent e) {
                // 2. Increment change count and schedule background re-snapshot
                scheduleUpdate(pendingChangeCount.incrementAndGet());
                return new Object();
            }
            // ... similar for removed/modified
        };
        bundleTracker.open();
    }

    // 3. The "map" method is called in the background when the 200ms debounce expires
    @Override
    protected List<XBundleDTO> map() throws Exception {
        Bundle[] bundles = context.getBundles();
        return Arrays.stream(bundles).map(this::toDTO).collect(Collectors.toList());
    }
}
```

This pattern ensures that the intensive `map()` (DTO creation + OSGi API calls) happens **away from the remote request path**.

---

### 📊 Full Snapshot Catalog

All the following data points are available as binary snapshots via the `AgentSnapshot` interface:

| Snapshot Method | DTO Type | Description |
| :--- | :--- | :--- |
| `bundles()` | `XBundleDTO` | Comprehensive bundle metadata and wiring |
| `components()` | `XComponentDTO` | Declarative Services (SCR) state |
| `conditions()` | `XConditionDTO` | OSGi R8 Condition services and component dependencies |
| `services()` | `XServiceDTO` | OSGi service registry snapshot |
| `configurations()` | `XConfigurationDTO` | ConfigAdmin properties and Metatype info |
| `properties()` | `XPropertyDTO` | System and Framework properties |
| `threads()` | `XThreadDTO` | JVM thread states and stack traces |
| `roles()` | `XRoleDTO` | UserAdmin roles and members |
| `healthChecks()` | `XHealthCheckDTO` | Registered Felix Health Checks |
| `httpComponents()` | `XHttpComponentDTO` | Servlets, Filters, Resources (R7/R8) |
| `jaxRsComponents()` | `XJaxRsComponentDTO` | JAX-RS Whiteboard components |
| `cdiContainers()` | `XCdiContainerDTO` | CDI Container and Component status |
| `bundleLoggerContexts()` | `XBundleLoggerContextDTO`| R7 Logger Admin contexts |
| `leaks()` | `XBundleDTO` | Potential classloader leaks |
| `runtime()` | `RuntimeDTO` | Framework and System information |
| `heapUsage()` | `XHeapUsageDTO` | Real-time memory/heap statistics |
| `runtimeCapabilities()` | `XRuntimeCapabilityDTO` | Detected framework capabilities |

#### 🔍 Live vs. Cached Snapshots

The agent intelligently distinguishes between data that benefits from reactive caching and data that should always be retrieved live:

| Admin | Strategy | Rationale |
| :--- | :--- | :--- |
| **Bundles** | **Cached** | Expensive to build (wiring, packages, services). Infrequent changes. |
| **Services** | **Cached** | Moderate cost. Service registry changes trigger re-snapshot. |
| **Components** | **Cached** | Moderate cost. SCR component state tracked via `changecount`. |
| **Conditions** | **Cached** | Moderate cost. Condition services tracked via `ServiceTracker`. |
| **Configurations** | **Cached** | Moderate cost. ConfigAdmin + Metatype processing. |
| **User Roles** | **Cached** | Cheap but infrequent changes. Caching prevents redundant processing. |
| **Health Checks** | **Cached** | Service tracker monitors HealthCheck registrations. |
| **HTTP Components** | **Cached** | R7/R8 Whiteboard runtime tracked via `changecount`. |
| **JAX-RS Components** | **Cached** | JAX-RS runtime tracked via `changecount`. |
| **CDI Containers** | **Cached** | CDI runtime tracked via `changecount`. |
| **Logger Contexts** | **Cached** | Bundle-based logger contexts change with bundle lifecycle. |
| **Classloader Leaks** | **Cached** | Phantom reference tracking updates on GC events. |
| **Threads** | **Live** | Constantly changing. Caching would provide stale data. |
| **Properties** | **Live** | Cheap to retrieve. Rarely change. |
| **Heap Usage** | **Live** | Real-time memory statistics. Must always be current. |
| **Runtime Info** | **Live** | Framework DTO is cheap to retrieve. |

#### 📦 Compression Format

All snapshot methods return data compressed using `Lz4Codec.compressWithLength()`, which produces a self-describing payload:

**Binary Format:**
```
[1-byte flag][4-byte big-endian length][compressed/raw data]
```

- **Flag (1 byte):** `0x00` = uncompressed, `0x01` = LZ4 compressed
- **Length (4 bytes):** Original uncompressed size in big-endian format (supports up to 2GB payloads)
- **Data:** LZ4-compressed bytes or raw bytes if compression didn't reduce size

**Compression Threshold:** Payloads smaller than **512 bytes** are not compressed to avoid overhead.

#### 🚀 Performance Benchmarks

The reactive binary snapshot architecture delivers **massive performance improvements** over traditional on-demand serialization:

| Metric | Before (On-Demand) | After (Reactive Snapshots) | Improvement |
| :--- | :--- | :--- | :--- |
| **CPU per request** | ~50ms | ~0.1ms | **500x faster** |
| **Heap pressure** | High (transient DTOs) | Minimal (single `byte[]`) | **10-20x reduction** |
| **Latency (cached)** | 50-100ms | <1ms | **50-100x faster** |
| **Throughput** | ~20 req/s | ~1000 req/s | **50x higher** |
| **Network efficiency** | LZ4 compressed | LZ4 compressed | Same |

**Real-World Impact:**
- A supervisor polling 10 snapshots/second consumes **<1% CPU** on the agent (vs. 50%+ with on-demand serialization)
- During OSGi event storms (100+ events/sec), debouncing prevents CPU thrashing while ensuring UI updates within 5 seconds
- Constrained devices (Raspberry Pi, industrial gateways) can serve monitoring requests with near-zero performance impact

#### 🔧 Using `SnapshotDecoder` (Supervisor Side)

The `SnapshotDecoder` utility simplifies decoding compressed snapshots on the supervisor side:

```java
// Initialize once
BinaryCodec codec = new BinaryCodec(bundleContext);
SnapshotDecoder decoder = new SnapshotDecoder(codec);

// Decode list snapshots
byte[] bundlesSnapshot = agent.bundles();
List<XBundleDTO> bundles = decoder.decodeList(bundlesSnapshot, XBundleDTO.class);

// Decode set snapshots
byte[] leaksSnapshot = agent.leaks();
Set<XBundleDTO> leaks = decoder.decodeSet(leaksSnapshot, XBundleDTO.class);

// Decode single object snapshots
byte[] runtimeSnapshot = agent.runtime();
RuntimeDTO runtime = decoder.decode(runtimeSnapshot, RuntimeDTO.class);

// Decode single bundle snapshot (efficient for detail views)
byte[] bundleSnapshot = agent.bundle(42L);
XBundleDTO bundle = decoder.decode(bundleSnapshot, XBundleDTO.class);
```

**Key Features:**
- **Type-safe decoding:** Generic methods for `List<T>`, `Set<T>`, and single objects
- **Automatic decompression:** Handles `Lz4Codec.decompressWithLength()` format transparently
- **Null-safe:** Returns empty collections for null/empty snapshots
- **Exception handling:** Wraps decompression failures in `RuntimeException` with clear error messages
- **Single-object snapshots:** Use `agent.bundle(id)` for efficient retrieval of individual bundle details without fetching the entire list

---

### 🕹️ Full Control API Catalog

While `AgentSnapshot` is strictly for reading state, the `Agent` interface provides a comprehensive set of operations with side-effects for manipulating the remote runtime.

| Category | Methods | Description |
| :--- | :--- | :--- |
| **Bundle Management** | `installWithData`, `installWithMultipleData`, `installFromURL`, `start`, `stop`, `uninstall` | Lifecycle management. `installWithMultipleData` batch-installs bundles and performs a single refresh to avoid internal framework thrashing. |
| **Bundle Introspection** | `getBundleRevisons`, `getBundleDataFile` | Inspecting wiring revisions and reading persistent data files from the bundle's private `getDataFile()` area. |
| **Resource Browsing** | `findBundleEntries`, `listBundleResources`, `getBundleEntryBytes`, `getBundleResourceBytes` | Deep inspection of bundle contents. **Physical JAR search**: `findBundleEntries` searches strictly inside the physical JAR (bypassing the classloader). **Classpath search**: `listBundleResources` searches the entire class space (including imports and fragments). The `*Bytes` variants retrieve raw content from either the physical JAR (`getBundleEntryBytes`) or the classloader (`getBundleResourceBytes`). |
| **Component Management** | `enableComponentByName`, `enableComponentById`, `disableComponentByName`, `disableComponentById` | Fine-grained control over Declarative Services (SCR) components. |
| **Configuration Admin** | `createOrUpdateConfigurations`, `deleteConfiguration`, `createFactoryConfiguration` | Create, update, and delete standard and factory configurations natively. |
| **User Admin** | `createRole`, `updateRole`, `removeRole` | Modifying roles, groups, and credentials within the OSGi User Admin service. |
| **Logger Admin** | `updateBundleLoggerContext` | Dynamically changing OSGi R7 log levels per bundle without restarts. |
| **Event Admin** | `sendEvent` (Sync), `postEvent` (Async) | Publishing custom OSGi events directly into the remote framework's event bus. |
| **Health Checks** | `executeHealthChecks` | On-demand execution of specific Felix Health Checks via tags or names. |
| **DMT Admin** | `readDmtNode`, `updateDmtNode` | Reads and updates nodes in the Device Management Tree. |
| **Process & Shell** | `execGogoCommand`, `execCliCommand` | Remote execution of Gogo commands or underlying OS shell (CLI) commands (tightly protected by allowlists). |
| **JMX / Memory** | `heapdump`, `getMemoryInfo`, `gc` | Triggering garbage collection, inspecting OS vs JVM memory, or requesting full JVM heap dumps over the wire. |
| **Diagnostics** | `threadDump`, `estimateThreadDumpSize`, `createThreadDumpLocally` | Generate jstack-style thread dumps (GZIP-compressed), estimate size, or save locally for later retrieval. |
| **Agent Lifecycle** | `disconnect`, `ping`, `refresh` | Checking connectivity, safely detaching the agent, or triggering a framework wiring refresh. |

## 🔌 SPIs & Extension Points

The agent is designed to be highly extensible via standard OSGi service-based SPIs. This allows users to add custom management logic or alternative large-payload handling strategies.

### 1. `AgentExtension` SPI
Dynamic extensions allow you to execute custom code in the remote runtime and return JSON-compliant results (DTOs).

- **Interface**: `com.osgifx.console.agent.spi.extension.AgentExtension<C, R>`
- **Registration**: Register as an OSGi service with the property `agent.extension.name`. This can be done via the `@Component` property or more concisely using the `@AgentExtensionName` annotation.
- **Discovery**: Automatically discovered by the agent's DI container via `ServiceTracker`.

**Example Extension (Option 1: Using `@AgentExtensionName`):**
```java
@Component
@AgentExtensionName("cpu.monitor")
public class CpuMonitorExtension implements AgentExtension<CpuContext, CpuResult> {
    @Override
    public CpuResult execute(CpuContext context) {
        // Custom logic here
        return new CpuResult(os.getLoad());
    }
}
```

**Example Extension (Option 2: Using `@Component` Property):**
```java
@Component(property = "agent.extension.name=cpu.monitor")
public class CpuMonitorExtension implements AgentExtension<CpuContext, CpuResult> {
    // ... same as above
}
```

### 2. `LargePayloadHandler` SPI
For massive files (e.g., a 2GB Heap Dump) that exceed RPC transport limits or network timeouts, the agent provides an out-of-band transfer SPI.

- **Interface**: `com.osgifx.console.agent.spi.payload.LargePayloadHandler`
- **Mechanism**: When a large payload is generated, the agent searches for a registered `LargePayloadHandler`. If found, it delegates the "handling" (e.g., uploading to an S3 bucket, SFTP server, or Corporate Artifactory).
- **Result**: The agent returns the URL or reference provided by the handler to the Supervisor, which can then download it separately.

**Example Use Case:**
On a factory-floor IoT device with limited RAM, a Heap Dump cannot be streamed over the socket. A registered `S3PayloadHandler` can upload the file to AWS and return a pre-signed URL to the engineer's OSGi.fx console.

### 3. I/O Redirection & Telnet Bridge
The agent can dynamically redirect the terminal I/O of the remote framework.

- **`CONSOLE` Mode**: Global hijacking of `System.in`, `System.out`, and `System.err`. All log messages and standard output are streamed back to the Supervisor in real-time.
- **`COMMAND_SESSION` Mode**: Creates a fresh, virtual Gogo `CommandSession`. This allows executing commands in a private context without interfering with other administrators.
- **`Telnet` Mode**: Acts as a transparent bridge. If the agent detects a Telnet-based shell (e.g. Felix Shell TUI) running on a specific port, it can connect to it and pipe the I/O through the RPC channel.

---

## 🪵 Logging & Eventing Architecture

The agent features a high-performance logging and eventing system designed for industrial-grade data rates and memory-constrained runtimes.

### 💾 Persistent Binary Logging
Beyond real-time streaming, the agent can capture logs even when no Supervisor is connected:
- **`osgifx_logs.bin`**: If `osgi.fx.agent.auto.start.log.capture=true` is set, the agent immediately begins recording logs to a local file in the bundle's data area.
- **Persistence**: This allows OSGi.fx to retrieve "post-mortem" logs after a framework crash or restart.
- **History Recovery**: Upon connection, the Supervisor can query the `BinaryLogBuffer` for historical data stored in this file.

### Binary Log Buffer (`BinaryLogBuffer`)
Traditional log collection (e.g. `List<LogEntry>`) creates massive heap pressure. The agent uses a **packed binary circular buffer**:
- **Zero-Allocation Writes**: Primitive types (longs, ints) are packed directly into a pre-allocated byte array using bitwise shifts. This completely eliminates object allocation during logging, preventing GC-induced jitter in real-time systems.
- **Persistent Flight Recorder**: If `osgi.fx.agent.auto.start.log.capture=true` is set, the buffer persists to `osgifx_logs.bin`. Upon agent restart, it automatically restores the buffer from disk, allowing for true post-mortem analysis even if the framework crashed and rebooted.
- **Two-Pass Sequential Retrieval**: When querying logs by a time range, the agent performs a two-pass scan on the index ring:
    1. **Pass 1 (Backward Scan)**: Identifies the start/end logical indices and calculates the exact byte size required for the result.
    2. **Pass 2 (Forward Copy)**: Allocates the result array once and performs a high-speed `System.arraycopy` (handling wrap-around). 
    This minimizes garbage collection overhead and ensures deterministic performance.

**In-Depth Packing Format:**
The buffer doesn't store Strings directly to avoid heap fragmentation. Instead:
1.  **String Pooling**: Common tags/loggers are indexed.
2.  **Binary Packing**: 
    - `Long` (Timestamp) -> 8 bytes
    - `Byte` (Level) -> 1 byte
    - `Int` (Message Ref) -> 4 bytes
3.  **Atomic Updates**: The write pointer is managed via `VarHandle` or `Unsafe` to allow concurrent logging without global locks.

**Example: Snapshot Retrieval**
```java
// Retrieve logs from the last 5 minutes
long fiveMinsAgo = System.currentTimeMillis() - 300_000;
```

#### 5. Advanced Path Resolution & Variable Substitution
The agent provides a powerful variable substitution engine for all filesystem-related RPC calls (Heap Dumps, Snapshots, Data-File retrieval). Paths can contain placeholders that are resolved against System Properties, Framework Properties, or Environment Variables.

**Supported Placeholders:**
- `{prop.key}`: Resolves to `bundleContext.getProperty("prop.key")` or `System.getProperty("prop.key")`.
- `{env:VAR_NAME}`: Resolves to `System.getenv("VAR_NAME")`.
- `{timestamp}`: Resolves to the current time in `yyyy-MM-dd-HH-mm-ss` format.

*Example Path:* `/tmp/osgifx/{framework.name}/heap-{timestamp}.hprof.gz`

#### 6. Internal "Micro-DI" Engine (`com.osgifx.console.agent.di`)
To maintain a small footprint (< 200KB) and zero external dependencies, the agent uses a custom, high-performance Dependency Injection engine.

- **Jakarta Inject Support**: Native support for `@Inject`, `@Singleton`, and `Provider<T>`.
- **Zero-Annotation Wiring**: Interfaces can be bound to implementations manually via `DI.bindInterface()`.
- **Fast Startup**: No classpath scanning or heavy reflection. All bindings are resolved lazily upon first request.
- **Provider Pattern**: Supports lazy-loaded services that are only instantiated when an RPC call actually requires them.
byte[] logSnapshot = logBuffer.snapshot(fiveMinsAgo, Long.MAX_VALUE);
// Result is a packed binary blob, ready for LZ4 compression and transmission

### Remote Eventing
When enabled (`osgi.fx.enable.eventing=true`), the agent subscribes to all OSGi `EventAdmin` topics and streams them in real-time to the Supervisor. Events are debounced and batched to prevent flooding over slow MQTT or Socket links.

---

## 📡 MQTT 5 & OSGi Messaging Integration

While Socket communication is great for direct diagnostics, OSGi.fx is heavily optimized for IoT and Edge gateway monitoring through MQTT.

The agent uses the **[OSGi Messaging](https://github.com/amitjoy/osgi-messaging)** library (`in.bytehue.messaging.mqtt5.provider`) as its default provider for MQTT 5 RPC.

### Why `osgi-messaging`?
1. **OSGi Native**: It seamlessly integrates MQTT 5 capabilities with OSGi framework services using the OSGi Messaging specification.
2. **High Throughput**: Capable of streaming large binary snapshots and LZ4-compressed RPC payloads with a very low footprint, which is crucial for embedded OSGi runtimes.
3. **Pluggable Architecture**: Configurable via `osgi.fx.agent.mqtt.provider=osgi-messaging`. If your infrastructure requires a different MQTT ecosystem, you can set the provider to `custom` and register your own `Mqtt5Publisher` and `Mqtt5Subscriber` OSGi services.

This integration empowers OSGi.fx to securely manage and introspect edge devices acting as MQTT clients, without needing to establish complex TCP reverse-tunnels into firewalled networks.

---

## 🔄 I/O Redirection

The agent provides a flexible **Redirection SPI** (`com.osgifx.console.agent.redirector.Redirector`) to capture and stream system I/O.

- **Gogo Redirector** (`COMMAND_SESSION` / `-1`): Connects a remote Gogo `CommandSession` to the Supervisor's terminal. Uses proxied access to Gogo APIs to avoid classloader constraints.
- **Console Redirector** (`CONSOLE` / `-2`): Captures `System.out` and `System.err` and streams them as remote events.
- **Socket Redirector** (`PORT`): Pipes I/O directly over the active agent socket or a dedicated secondary port.
- **Null Redirector** (`NONE` / `0`): Silently discards I/O or detaches active redirectors.

This sub-system leverages the `redirect(int port)` and `stdin(String)` remote APIs to hook into and push characters to the chosen backend stream.

---

### 🛡️ Command Security & Allowlisting
To prevent accidental or malicious execution of sensitive commands, the agent implements a dual-layer allowlist system:

1.  **Gogo Allowlist** (`osgi.fx.agent.gogo.allowlist`): 
    - Comma-separated list of scopes or functions (e.g., `osgi:*, equinox:start`).
    - Defaults to `*` (All permitted).
2.  **CLI Allowlist** (`osgi.fx.agent.cli.allowlist`):
    - Restricts what shell commands can be executed via `execCliCommand`.
    - Defaults to `*`.

**How it works**: Before any execution, the agent checks the command against these patterns. If a match is not found, the operation is blocked with a security exception, protecting the underlying host OS.

---

The agent registers several commands under the `osgifx` scope for dynamic control via the Gogo shell.

| Command | Usage | Description |
| :--- | :--- | :--- |
| `osgifx:startSocket` | `startSocket [host=.. port=.. secure=..]`| Starts the Socket RPC server. |
| `osgifx:stopSocket` | `stopSocket` | Stops the Socket RPC server and clears properties. |
| `osgifx:startMqtt` | `startMqtt [provider=.. pubTopic=.. subTopic=..]`| Starts the MQTT RPC endpoint. |
| `osgifx:stopMqtt` | `stopMqtt` | Stops the MQTT RPC endpoint. |
| `osgifx:status` | `status` | Displays running status and current configuration. |

---

## 📦 Optional Dependencies & Dynamic Wiring (Graceful Degradation)

To ensure the agent remains ultra-lightweight and heavily portable, it does **not** rely on strict `Import-Package` manifest headers for heavily utilized OSGi compendium specifications (like ConfigAdmin, SCR, EventAdmin).

If the agent strictly imported `org.osgi.service.cm`, it would fail to resolve on minimal OSGi gateways or embedded IoT devices that do not have a ConfigAdmin implementation deployed.

Instead, the agent uses the **`PackageWirings`** subsystem. It dynamically interrogates the framework's `BundleWiring` state to check if optional specification packages are physically wired to the agent bundle.

### How `PackageWirings` benefits the runtime:
1. **Zero-Friction Deployment**: Drop the agent into *any* OSGi framework (Equinox, Felix, Concierge) regardless of what other bundles are installed.
2. **Graceful Degradation**: If `isScrWired()` returns false, the agent cleanly skips initializing the `XComponentAdmin` service. RPC calls for DS Components gracefully return empty collections instead of throwing `NoClassDefFoundError`.
3. **High Performance Cache**: To avoid the massive overhead of walking the OSGi wiring graph for every network request, wiring results are cached concurrently. The cache is totally stable because once a package is wired to a bundle revision, it cannot change without a framework-level bundle update/refresh. 

**Technical Design:**
The agent adapts its own `Bundle` to `BundleWiring` and inspects `getRequiredWires(osgi.wiring.package)`. This allows it to detect if an optional dependency (e.g., `org.osgi.service.cm`) has been resolved by the framework. This approach is superior to `Class.forName()` as it respects the OSGi visibility rules and avoids unnecessary class loading if the package is not actually available.

### Supported Optional Capabilities:
The following specifications are dynamically detected via `PackageWirings`. If installed on the system, the agent automatically exposes their management capabilities to OSGi.fx:

*   **Declarative Services (SCR)** (`org.osgi.service.component.runtime`)
*   **Configuration Admin** (`org.osgi.service.cm`)
*   **Metatype Service** (`org.osgi.service.metatype`)
*   **User Admin** (`org.osgi.service.useradmin`)
*   **Event Admin** (`org.osgi.service.event`)
*   **Log Service & R7 Logger** (`org.osgi.service.log` / `org.osgi.service.log.admin`)
*   **HTTP & JAX-RS Whiteboard** (`org.osgi.service.http.runtime` / `org.osgi.service.jaxrs.runtime`)
*   **CDI Integration** (`org.osgi.service.cdi.runtime`)
*   **Felix Health Checks** (`org.apache.felix.hc.api`)
*   **Gogo Shell** (`org.apache.felix.gogo.runtime`)
*   **DMT Admin** (`org.osgi.service.dmt`)

*(Note: OSGi.fx queries `Agent.getRuntimeCapabilities()` to dynamically toggle UI elements based on the exact wirings active on the remote agent).*

---

## ⚙️ Configuration & Diagnostics

The agent behavior is controlled via OSGi Framework Properties (or System Properties). Overriding via System Properties is supported to allow dynamic recalibration via Gogo shell.

### 🌐 Connectivity & Transport

| Property | Default | Description |
| :--- | :--- | :--- |
| `osgi.fx.agent.socket.port` | `1234` | TCP port for the management server. Format: `[interface:]port`. |
| `osgi.fx.agent.socket.password` | `(none)` | Shared secret for Socket authentication. |
| `osgi.fx.agent.socket.secure` | `false` | Enable TLS/SSL for the socket server. |
| `osgi.fx.agent.socket.secure.sslcontext.filter` | `(none)` | OSGi filter for a custom `SSLContext`. |
| `osgi.fx.agent.mqtt.provider` | `osgi-messaging` | MQTT implementation (`osgi-messaging` or `custom`). When using `custom`, the agent expects `Mqtt5Publisher` and `Mqtt5Subscriber` OSGi services. |
| `osgi.fx.agent.mqtt.pubtopic` | `(none)` | MQTT topic for outbound messages (Required for MQTT). |
| `osgi.fx.agent.mqtt.subtopic` | `(none)` | MQTT topic for inbound messages (Required for MQTT). |

#### 🔐 Authentication Configuration

**Socket Password Authentication:**
To require password authentication for socket connections:
1.  Set the `osgi.fx.agent.socket.password` system property on the agent (e.g., `-Dosgi.fx.agent.socket.password=your-secure-password`).
2.  In the OSGi.fx Client connection settings, check **"Requires Authentication"** and enter the matching password. You can check **"Save Password"** to store credentials securely (encrypted with AES-256).

**Socket Secure Configuration (SSL/TLS):**
1.  Set `osgi.fx.agent.socket.secure=true`.
2.  Set `osgi.fx.agent.socket.secure.sslcontext.filter` to a filter string (e.g., `(name=my_sslcontext)`) to look up the `SSLContext` service.
3.  Ensure a TrustStore is configured in the OSGi.fx Client to match the server's KeyStore.

**MQTT Username/Password Authentication:**
For MQTT brokers requiring authentication:
1.  In the OSGi.fx Client connection settings, check **"Requires Authentication"** and enter your MQTT broker username and password.

**MQTT OAuth/Token Authentication:**
If your MQTT broker requires OAuth tokens:
1.  Configure the Token Configuration in the OSGi.fx Client.
2.  Leave the **Password** field empty in the connection settings.

**Token Configuration Format (JSON):**
```json
{
    "authServerURL": "https://auth.example.com/token",
    "clientId": "my-client-id",
    "clientSecret": "my-secret",
    "audience": "my-audience",
    "scope": "mqtt-scope"
}
```

### 🛡️ Resource Protection (Injection Attacks)
| Property | Default | Description |
| :--- | :--- | :--- |
| `osgi.fx.agent.rpc.max.decompressed.size` | `250MB` | Max GZIP decompression size (Zip Bomb Protection). |
| `osgi.fx.agent.rpc.max.collection.size` | `1,000,000` | Max elements in decoded collections. |
| `osgi.fx.agent.rpc.max.map.size` | `500,000` | Max entries in decoded maps. |
| `osgi.fx.agent.rpc.max.byte.array.size`| `100MB` | Max length of decoded byte arrays. |
| `osgi.fx.agent.gogo.allowlist` | `*` | Allowed Gogo commands (e.g., `osgi:*, equinox:start`). |
| `osgi.fx.agent.cli.allowlist` | `*` | Allowed OS shell commands. |

### 📝 Behavioral Configuration
| Property | Default | Description |
| :--- | :--- | :--- |
| `osgi.fx.enable.logging` | `false` | Enable real-time log streaming. |
| `osgi.fx.enable.eventing` | `false` | Enable real-time OSGi event streaming. |
| `osgi.fx.agent.auto.start.log.capture` | `false` | Start circular log buffer on bundle activation. |
| `osgi.fx.agent.cli.enabled` | `true` | Globally enable/disable underlying shell execution. |
| `osgi.fx.agent.gogo.enabled` | `true` | Globally enable/disable Gogo shell execution. |

---

## 🛡️ Security & Resilience

The agent implements several layers of protection to ensure secure and stable operation:
- **Decompression Guard**: Rejects LZ4 streams that expand beyond `osgi.fx.agent.rpc.max.decompressed.size` to prevent CPU exhaustion.
- **Collection Bounds**: Decoding is interrupted if a malicious payload requests the allocation of massive Maps or Lists (Collection Bombs).
- **Sub-System Self-Protection**: Explicitly blocks `stop()` and `uninstall()` operations targeting the **System Bundle (ID 0)** or the **Agent Bundle itself** to prevent management bridge "suicide".
- **Path Traversal Guard**: Strictly validates file paths for data-file retrieval and local creation commands.
- **Authentication**: Socket transport can be locked behind a shared secret/password.
- **TLS/SSL**: All Socket communication can be encrypted via a framework-provided `SSLContext`.

---

---

## 🔍 Advanced Diagnostics

### 🕵️ Classloader Leaks (Phantom Reference Graph)
The `leaks()` snapshot identifies bundles whose classes are improperly pinned in memory after the bundle has been uninstalled, updated, or refreshed. 

Instead of relying on standard heap analysis which is incredibly slow, the agent utilizes a highly targeted **Phantom Reference Graph**:
1. **Interception**: A custom `BundleTracker` intercepts all bundle start events and extracts the internal `BundleWiring` classloader instance.
2. **Phantom Queuing**: It wraps the target classloader in a `java.lang.ref.PhantomReference` securely tied to a background `ReferenceQueue`.
3. **Usage Counting**: It maintains a thread-safe strict counter of active classloader instances per `bundleId`.
4. **Daemon Polling**: A zero-cost daemon thread (`classloader-leak-detector`) constantly polls the reference queue. When the JVM's Garbage Collector successfully finalizes an old, unreferenced classloader, the counter is safely decremented.
5. **Anomaly Detection**: If the UI calls `leaks()` and the agent detects that an `ACTIVE` bundle possesses `> 1` tracked classloaders (or an inactive bundle possesses any), it mathematically proves that a rogue thread, `ThreadLocal`, or external variable is illegally retaining the old bundle revision in memory, causing a severe memory leak.

### ⏱️ Startup Duration Tracking (`BundleStartTimeCalculator`)
The agent implements a `SynchronousBundleListener` that intercepts `STARTING` and `STARTED` events across the entire framework to provide precise boot analytics.
- **Microsecond Precision**: It calculates the exact clock-time delta for every bundle's activation cycle.
- **Bottleneck Identification**: This allows the Console to visualize a "Waterfall" view of framework startup, highlighting slow-starting or blocking bundles.
- **Persistence**: Startup timings are available even for bundles that started before the agent was fully initialized, thanks to deep integration with the framework's historical bundle states.

### 📉 Network-Aware Estimation & Local Storage
For environments with strict network limits (e.g., narrow-band IoT), the agent supports a "Save-then-Pull" workflow. 

1. **Estimation**: The supervisor first calls estimation algorithms to decide if the payload is safe for the current network:
    - `estimateHeapdumpSize()`: Uses current memory usage and a conservative 25% GZIP compression estimate.
    - `estimateSnapshotSize()`: Reactively calculates size based on the count of active bundles, components, and services.
    - `estimateThreadDumpSize()`: Estimates size based on active thread counts and expected stack frame depth.
2. **Local Creation**: If the estimate is too large for real-time RPC, the UI can request local creation:
    - `createHeapdumpLocally(path)`
    - `createSnapshotLocally(path)`
    - `createThreadDumpLocally(path)` 
   The resulting file can then be retrieved later or via a `LargePayloadHandler`.

### 📡 MQTT RPC & OSGi Messaging
The agent supports a fully decoupled MQTT 5 transport, which is essential for managing devices behind restrictive firewalls or NATs.

- **OSGi Messaging Integration**: By default, it leverages the [osgi-messaging](https://github.com/amitjoy/osgi-messaging) library (specifically `in.bytehue.messaging.mqtt5.provider`). This provides a standardized, industrial-strength way to handle asynchronous RPC over MQTT.
- **Payload Compression**: Just like the socket transport, MQTT payloads are LZ4 compressed and binary-encoded to stay within typical MQTT broker message limits (e.g., 256KB).
- **Topic-Based Routing**: Communication is Multiplexed over a pair of `pubtopic` and `subtopic`, allowing a single broker to manage thousands of agents.

---

- `com.osgifx.console.agent`: The core implementation bundle.
    - **`admin`**: Domain-specific managers for OSGi services (Components, Config, Events, Metatype, etc.).
    - **`di`**: A high-performance, Jakarta-compliant Micro-DI engine.
    - **`redirector`**: The I/O hijacking engine supporting multi-mode terminal proxies.
    - **`rpc`**: The heart of the agent. Contains the `BinaryCodec`, LZ4 compression logic, and transport-agnostic framing.
    - **`starter`**: Contains the zero-dependency `Activator` and Gogo command bridge.
- `com.osgifx.console.agent.api`: The architectural contract.
    - **`Agent` / `AgentSnapshot`**: The primary management interfaces.
    - **`dto`**: A comprehensive suite of "Anemic" DTOs designed for minimal serialization footprints.
    - **`spi`**: Extension points for custom diagnostic logic and large payload offloading.

## 🕹️ Runtime Calibration (Gogo Shell)

The agent can be dynamically reconfigured at runtime without restarting the bundle. It registers several commands under the `osgifx` scope:

| Command | Usage | Description |
| :--- | :--- | :--- |
| `osgifx:startSocket` | `osgifx:startSocket [port=X host=Y]` | Starts the socket server with optional overrides. |
| `osgifx:stopSocket` | `osgifx:stopSocket` | Gracefully shuts down the socket server. |
| `osgifx:startMqtt` | `osgifx:startMqtt [provider=A pubTopic=B subTopic=C]` | Starts the MQTT bridge. |
| `osgifx:stopMqtt` | `osgifx:stopMqtt` | shuts down the MQTT bridge. |
| `osgifx:status` | `osgifx:status` | Displays running endpoints and active configs. |

**Pro Tip:** You can change the RPC behavior on-the-fly:

```bash
g! osgifx:startSocket [port=2222 host=0.0.0.0 secure=true]
```

## 🏗️ JVM Compatibility & Resilience
The agent is designed to be "Future-Proof" and compatible with Java 8 through Java 25+.

- **Unsafe Fallback**: To achieve maximum performance on HotSpot JVMs, it utilizes `sun.misc.Unsafe` for zero-copy memory operations.
- **Graceful Degradation**: If `sun.misc.Unsafe` is inaccessible (e.g., on newer Runtimes or non-HotSpot JVMs), it automatically falls back to `LambdaMetafactory` or standard Reflection without any functional loss.
- **Zero-Dependency Core**: It does not depend on any third-party libraries (except the optional MQTT provider), making it immune to "Dependency Hell".

---
© 2021-2026 Amit Kumar Mondal. Licensed under the Apache License, Version 2.0.
