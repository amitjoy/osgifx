# OSGi.fx User Guide

This guide provides detailed instructions on setting up the Runtime Agent and launching the OSGi.fx Client.

## 1. Runtime Agent Setup

To manage your OSGi framework remotely, you must install and configure the OSGi.fx Agent.

### Minimum Requirements
*   **Java**: 1.8+
*   **OSGi**: R6+

### Installation
Install the `com.osgifx.console.agent.jar` (available from Maven Central) into your OSGi runtime.

### Configuration

The agent supports **Socket** and **MQTT** communication protocols.

#### A. Socket Communication

**Basic Configuration:**
Set the `osgi.fx.agent.socket.port` system property in your runtime.
*   `1099`: Allows connections only from localhost.
*   `0.0.0.0:1099`: Allows remote connections.

> [!NOTE]
> There is no hardcoded default port. You must explicitly configure `osgi.fx.agent.socket.port` or use the `osgifx:startSocket` Gogo command (which defaults to `1099`).

**Password Authentication:**
To require password authentication for socket connections:
1.  Set the `osgi.fx.agent.socket.password` system property on the agent:
    ```bash
    -Dosgi.fx.agent.socket.password=your-secure-password
    ```
2.  In the OSGi.fx Client connection settings:
    *   Check **"Requires Authentication"**
    *   Enter the matching password
    *   Optionally check **"Save Password"** to store credentials securely (encrypted with AES-256)
3.  If password is not saved, you'll be prompted each time you connect.

**Secure Configuration (SSL/TLS):**
To secure the socket connection:
1.  Set `osgi.fx.agent.socket.secure=true`.
2.  Set `osgi.fx.agent.socket.secure.sslcontext.filter` to a filter string (e.g., `(name=my_sslcontext)`). This is used to look up the `SSLContext` service.
3.  Ensure a TrustStore is configured in the OSGi.fx Client to match the server's KeyStore.

---

#### B. MQTT Communication

To use MQTT, install the OSGi Messaging MQTT 5 provider and configure the client:

**Installation:**
*   **Maven Coordinates**: `in.bytehue:in.bytehue.messaging.mqtt5.provider:LATEST` (available from Maven Central)
*   **Configuration PID**: `in.bytehue.messaging.client`

**Alternative:** Set `osgi.fx.agent.mqtt.provider=custom` and register your own `Mqtt5Publisher` and `Mqtt5Subscriber` OSGi services.

> [!NOTE]
> You must configure `maximumPacketSize` and `sendMaximumPacketSize` to `268435456` (256 MB) in the MQTT client configuration to handle large data payloads.

**Agent Configuration:**
Set the following system properties:
*   `osgi.fx.agent.mqtt.pubtopic`: Topic where the agent sends responses.
*   `osgi.fx.agent.mqtt.subtopic`: Topic where the agent receives requests.

**Username/Password Authentication:**
For MQTT brokers requiring authentication:
1.  In the OSGi.fx Client connection settings:
    *   Check **"Requires Authentication"**
    *   Enter your MQTT broker username and password
    *   Optionally check **"Save Password"** to store credentials securely (encrypted with AES-256)
2.  If password is not saved, you'll be prompted each time you connect.

**OAuth/Token Authentication:**
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
*   If `clientId` is omitted, the MQTT client ID will be used.
*   The authorization server must respond with:
    ```json
    {
        "access_token": "...",
        "expires_in": 3600
    }
    ```

## 2. Launching the Client

### Using the Launch Script (`RunOSGiFx`)

We provide a robust Java script to launch the client with all necessary modularity flags.

**Prerequisites:**
*   Java 25+ (required for the OSGi.fx UI client)

**Setup:**
1.  Download `RunOSGiFx` locally.
2.  Make it executable: `chmod u+x RunOSGiFx`.

**Usage:**
```bash
# Run a local JAR
./RunOSGiFx --jar path/to/osgifx.jar

# Download and run (requires internet)
./RunOSGiFx --gav com.osgifx:osgifx:LATEST
```

**Features:**
*   **Auto-Download**: Fetches artifacts from Maven Central/Sonatype if `--gav` is used.
*   **System Properties**: Pass `-Dprop=val` arguments, and they will be correctly applied to the JVM.

### Headless Launch

For automated connections without the wizard, refer to the [Headless Launch Guide](HEADLESS_LAUNCH.md).

---

## 3. Agent Configuration Reference

The OSGi.fx agent can be customized using system properties. These properties allow you to harden the agent against resource exhaustion attacks and configure how large payloads (like heap dumps) are handled.

### Security Hardening

To prevent attacks such as Zip Bombs or Collection Bombs during RPC communication, the following properties can be configured:

| Property | Default | Description |
| :--- | :--- | :--- |
| `osgi.fx.agent.rpc.max.decompressed.size` | 250 MB | Maximum allowed decompressed size for GZIP streams. Prevents Zip Bomb attacks. |
| `osgi.fx.agent.rpc.max.collection.size` | 1,000,000 | Maximum number of elements in a decoded collection. Prevents Collection Bomb attacks. |
| `osgi.fx.agent.rpc.max.map.size` | 500,000 | Maximum number of entries in a decoded map. Prevents Collection Bomb attacks. |
| `osgi.fx.agent.rpc.max.byte.array.size` | 100 MB | Maximum allowed length for a decoded byte array. Prevents memory exhaustion. |
| `osgi.fx.agent.gogo.allowlist` | `*` | Comma-separated allowlist for Gogo commands (e.g., `osgi:*, equinox:start`). Use `*` to allow all. |
| `osgi.fx.agent.cli.allowlist` | `*` | Comma-separated allowlist for OS shell commands. Use `*` to allow all. |

### Feature Control

| Property | Default | Description |
| :--- | :--- | :--- |
| `osgi.fx.enable.logging` | `false` | Enable real-time log streaming to the supervisor. |
| `osgi.fx.enable.eventing` | `false` | Enable real-time OSGi event streaming to the supervisor. |
| `osgi.fx.agent.auto.start.log.capture` | `false` | Start circular log buffer immediately on bundle activation. |
| `osgi.fx.agent.cli.enabled` | `true` | Globally enable/disable OS shell command execution. |
| `osgi.fx.agent.gogo.enabled` | `true` | Globally enable/disable Gogo shell command execution. |

---

## 4. Gogo Commands

The OSGi.fx agent registers several Gogo commands under the `osgifx` scope. These commands allow you to dynamically start/stop the agent and check its status from the console.

| Command | Description | Example |
| :--- | :--- | :--- |
| `osgifx:startSocket` | Starts the socket agent | `osgifx:startSocket [port=1099 host=0.0.0.0 secure=true]` |
| `osgifx:stopSocket` | Stops the socket agent | `osgifx:stopSocket` |
| `osgifx:startMqtt` | Starts the MQTT agent | `osgifx:startMqtt [provider=osgi-messaging pubTopic=/out subTopic=/in]` |
| `osgifx:stopMqtt` | Stops the MQTT agent | `osgifx:stopMqtt` |
| `osgifx:status` | Prints the agent status | `osgifx:status` |

### Starting Socket Agent
You can start the socket agent with optional arguments:
```bash
g! osgifx:startSocket [port=1099 host=localhost secure=true sslContextFilter=(name=my_sslcontext)]
```

**Parameters:**
*   `port` - TCP port number (default: `1099`)
*   `host` - Interface to bind to (default: `localhost`)
*   `secure` - Enable TLS/SSL (default: `false`)
*   `sslContextFilter` - OSGi filter for custom `SSLContext` service (optional)

### Starting MQTT Agent
You can start the MQTT agent with optional arguments:
```bash
g! osgifx:startMqtt [provider=osgi-messaging pubTopic=osgifx/out subTopic=osgifx/in]
```

**Parameters:**
*   `provider` - MQTT provider (`osgi-messaging` or `custom`, default: `osgi-messaging`)
*   `pubTopic` - Topic for agent responses (required)
*   `subTopic` - Topic for agent requests (required)

---

## 5. Agent Extension SPI

The agent provides an **AgentExtension** SPI that allows you to execute custom code in the remote runtime and return JSON-compliant results.

### The SPI Interface

Implement the `AgentExtension` interface and register it as an OSGi service:

```java
public interface AgentExtension<C, R> {
    /**
     * Executes the extension logic.
     *
     * @param context the input context (must be DTO-compliant)
     * @return the result (must be DTO-compliant)
     */
    R execute(C context);
}
```

### Service Registration

Register your extension with a unique name:

```java
@Component(
    service = AgentExtension.class,
    property = "agent.extension.name=my-custom-extension"
)
public class MyExtension implements AgentExtension<Map<String, Object>, Map<String, Object>> {
    @Override
    public Map<String, Object> execute(Map<String, Object> context) {
        // Your custom logic here
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        return result;
    }
}
```

The supervisor can then invoke your extension via `agent.executeExtension("my-custom-extension", context)`.

---

## 6. Large Payload Handling (SPI)

For remote runtimes where transferring large files (e.g., several hundred MBs of heap dumps) over RPC is inefficient or restricted, OSGi.fx provides the `LargePayloadHandler` Service Provider Interface (SPI).

This SPI allows you to implement custom logic to handle large payloads, such as uploading them to an external storage service (e.g., Amazon S3, Azure Blob Storage, or an internal Artifactory).

### The SPI Interface

To use this feature, implement the `LargePayloadHandler` interface and register it as an OSGi service in your remote runtime.

```java
public interface LargePayloadHandler {
    /**
     * Handles the large payload (e.g., uploads to S3).
     *
     * @param metadata the payload metadata
     * @param content  the payload content as a stream
     * @return the result of the handling operation
     */
    PayloadHandlerResult handle(PayloadMetadata metadata, InputStream content);

    /**
     * Returns the maximum payload size supported by this handler in bytes.
     * Large payloads exceeding this limit will fallback to other methods.
     */
    long getMaximumSize();

    /**
     * Returns a human-readable name for this handler (e.g., "S3 Upload").
     */
    String getName();
}
```

### Pre-flight and User Decision

When a large payload operation (like "Heap Dump") is triggered:

1.  **Pre-flight Check**: The agent estimates the payload size and checks for registered `LargePayloadHandler` services.
2.  **User Choice**: A dialog is presented to the user with the following options:
    *   **Transfer via RPC**: Default method; directly transfers the file through the active connection.
    *   **Use [Handler Name]**: Available if an SPI implementation is registered.
    *   **Store Locally**: Saves the file on the remote machine's disk at a specified location.

### Implementation Example (Conceptual S3 Upload)

```java
@Component(service = LargePayloadHandler.class)
public class S3PayloadHandler implements LargePayloadHandler {

    @Override
    public PayloadHandlerResult handle(PayloadMetadata metadata, InputStream content) {
        try {
            // Logic to upload 'content' to an S3 bucket...
            String s3Url = "https://s3.amazonaws.com/my-bucket/" + metadata.getFilename();
            return PayloadHandlerResult.success(s3Url);
        } catch (Exception e) {
            return PayloadHandlerResult.failure("Upload failed: " + e.getMessage());
        }
    }

    @Override
    public long getMaximumSize() {
        return 5 * 1024 * 1024 * 1024L; // 5 GB
    }

    @Override
    public String getName() {
        return "Amazon S3";
    }
}
```

---

## 7. Chaos Monkey - Resilience Testing

The **Chaos Monkey** is a powerful fault-injection tool designed to test the resilience, self-healing, and dynamic rebinding capabilities of your OSGi applications. It randomly disrupts bundles and SCR components based on your configuration.

### 4-Layer Safety Architecture

To prevent accidental "suicide" of the remote environment, Chaos Monkey implements a multi-layered safety guard:

1.  **System Bundle Guard**: The System Bundle (ID 0) is strictly immune to all chaos operations.
2.  **Agent Guard**: The OSGi.fx Agent bundle is automatically detected and protected.
3.  **Infrastructure Guard**: Bundles from the framework itself (e.g., Felix or Equinox core) are excluded.
4.  **Scope Guard**: You define the exact pool of bundles and components to be targeted.
    *   **Filtering**: Both inclusion and exclusion filters support **comma-separated regular expressions** (e.g., `com.my.*, com.other.*`).

### Example Scenarios

*   **Test Bundle Refresh**: Use Chaos Monkey to stop bundles that provide common services. This verifies if dependent bundles correctly handle service disappearance and refresh.
*   **Component Rebinding**: Randomly disable single DS components to ensure that consumers correctly rebind to alternatives or transition into a safe "unsatisfied" state without crashing.
*   **Safety Timers**: Always use the **Auto-Stop Timer** to ensure that a chaos session doesn't run indefinitely in shared development environments.

---

## 8. OSGi.fx Client Features

OSGi.fx comes loaded with a plethora of features designed to make remote OSGi management seamless. While most of the options in the various tabs reflect standard capabilities expected by developers familiar with OSGi, several advanced features significantly enhance productivity and diagnostics. 

### Advanced Diagnostics
*   **Thread Dump & Heap Dump**: Capture and analyze threads and heap memory directly from the remote runtime. Thread dumps assist in detecting deadlocks or CPU spikes natively, while heap dumps help pinpoint memory leaks. Heap dumps utilize the Large Payload Handling SPI for efficient transferring and local storage.
*   **Snapshot Functionality**: Take a complete snapshot of the remote runtime state (bundles, services, components, properties, etc.). This is incredibly useful for capturing the state at a specific point in time, comparing multiple states to trace issues, attaching to bug reports, or reviewing the environment offline for root-cause analysis without requiring a persistent connection to the agent.

### Advanced Component Management
*   **Conditions (with Injection)**: OSGi Declarative Services (DS) components often define conditions for activation. You can seamlessly inject these conditions directly from the UI to satisfy and simulate requirements, triggering component activations on demand. This is an awesome functionality for testing component lifecycles without writing any additional scaffolding code or manual configurations.
*   **Batch Install**: Install multiple bundles simultaneously by selecting a directory or selecting multiple JAR files in the Bundles tab. This drastically reduces the time needed to deploy updates, install third-party libraries, or set up a new remote environment.

### Event Administration
*   **Sending and Receiving Events**: You can both listen to and emit OSGi events through the EventAdmin interface. Use the intuitive **Event Filter Dialog** to easily construct LDAP filters for subscribing to specific topics. The dialog features an autocomplete dropdown that displays options recognized by OSGi in event filters, drastically simplifying the manual creation of complex filtering rules.

### Model Context Protocol (MCP) Server
OSGi.fx deeply integrates an **[MCP Server](MCP_SERVER.md)** that securely connects Large Language Models (LLMs) to your running OSGi framework. This unlocks natural language interactions for deep diagnostics, real-time monitoring, and context-aware log analysis directly from tools like Claude Desktop. You can find detailed capabilities and configuration instructions in the [MCP Server Guide](MCP_SERVER.md).

### Global Search and Table Filtering
*   **Menu Search Option**: Quickly find functions, specific tabs, and preferences by using the global search field located directly in the application menu.
*   **Table Column Search**: Every table in OSGi.fx allows for advanced inline searching. **Right-click on any column header** to see the option to search for matching entries within that specific column. It makes locating specific bundles, components, or properties trivial even within enormous datasets.

### Bottom Status Bar
The bottom status bar of the OSGi.fx UI provides vital connection health and synchronization utilities:
*   **RPC Progress Dialog**: A spinner/icon indicates ongoing Remote Procedure Calls (RPC). Clicking it opens the RPC Progress Dialog, which is highly beneficial for debugging and monitoring slow or long-running network requests, giving visibility into what operations are actively communicating with the runtime.
*   **Sync Button**: Allows you to force-synchronize the client's localized state with the remote runtime. Using the **"Sync All"** menu option immediately invalidates all client-side caches and pulls the freshest data from the remote runtime, ensuring you always observe the most accurate state if the runtime was modified externally.
