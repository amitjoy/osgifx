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
