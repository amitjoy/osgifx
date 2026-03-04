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
*   `2000`: Allows connections only from localhost.
*   `0.0.0.0:2000`: Allows remote connections.

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

To use MQTT, install `in.bytehue.messaging.mqtt5.provider.jar` (from OSGi Messaging project) and configure the `in.bytehue.messaging.client` PID.

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
*   Java 25+

**Setup:**
1.  Download `RunOSGiFx` locally.
2.  Make it executable: `chmod u+x RunOSGiFx`.

**Usage:**
```bash
# Run a local JAR
./RunOSGiFx --jar path/to/osgifx.jar

# Download and run (requires internet)
./RunOSGiFx --gav com.osgifx:osgifx-console:LATEST
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
| `osgi.fx.agent.rpc.max.map.size` | 1,000,000 | Maximum number of entries in a decoded map. Prevents Collection Bomb attacks. |
| `osgi.fx.agent.rpc.max.byte.array.size` | 100 MB | Maximum allowed length for a decoded byte array. Prevents memory exhaustion. |

---

## 4. Gogo Commands

The OSGi.fx agent registers several Gogo commands under the `osgifx` scope. These commands allow you to dynamically start/stop the agent and check its status from the console.

| Command | Description | Example |
| :--- | :--- | :--- |
| `osgifx:startSocket` | Starts the socket agent | `osgifx:startSocket [port=1234 host=0.0.0.0]` |
| `osgifx:stopSocket` | Stops the socket agent | `osgifx:stopSocket` |
| `osgifx:startMqtt` | Starts the MQTT agent | `osgifx:startMqtt [pubTopic=/out subTopic=/in]` |
| `osgifx:stopMqtt` | Stops the MQTT agent | `osgifx:stopMqtt` |
| `osgifx:status` | Prints the agent status | `osgifx:status` |

### Starting Socket Agent
You can start the socket agent with optional arguments for port, host, security, and SSL context filter:
```bash
g! osgifx:startSocket [port=4567 host=localhost secure=true]
```

### Starting MQTT Agent
You can start the MQTT agent with optional arguments for provider, pubTopic, and subTopic:
```bash
g! osgifx:startMqtt [provider=osgi-messaging pubTopic=osgifx/out subTopic=osgifx/in]
```

---

## 5. Large Payload Handling (SPI)

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
