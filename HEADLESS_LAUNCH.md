# Headless Launch Configuration

**OSGi.fx** supports a "Headless" launch mode, which allows you to start the application with a pre-defined connection configuration. This skips the initial connection wizard and automatically connects to the configured agent.

> [!NOTE]
> "Headless" in this context means "without the connection wizard UI", not necessarily without any UI at all. The main application window will still open after connection.

## How to Use

To use this feature, you need to provide a JSON configuration file via the `osgifx.config` system property when launching the application.

```bash
java -Dosgifx.config=/path/to/config.json -jar osgifx.jar
```

## Configuration Format

The configuration file must be a valid JSON file. You can configure either a **Socket** connection or an **MQTT** connection.

### Socket Connection Example

```json
{
  "type": "SOCKET",
  "socket": {
    "host": "localhost",
    "port": 4567,
    "timeout": 10000,
    "trustStorePath": "/path/to/truststore",
    "trustStorePassword": "password"
  }
}
```

| Field | Type | Description |
| :--- | :--- | :--- |
| `type` | String | Must be `SOCKET` |
| `host` | String | The hostname or IP address of the OSGi agent |
| `port` | Number | The port number of the OSGi agent |
| `timeout` | Number | Connection timeout in milliseconds |
| `trustStorePath` | String | (Optional) Path to the SSL truststore |
| `trustStorePassword` | String | (Optional) Password for the SSL truststore |

### MQTT Connection Example

```json
{
  "type": "MQTT",
  "mqtt": {
    "server": "broker.hivemq.com",
    "port": 1883,
    "timeout": 10000,
    "clientId": "osgifx-client",
    "username": "myuser",
    "password": "mypassword",
    "pubTopic": "osgifx/pub",
    "subTopic": "osgifx/sub",
    "lwtTopic": "osgifx/lwt"
  }
}
```

| Field | Type | Description |
| :--- | :--- | :--- |
| `type` | String | Must be `MQTT` |
| `server` | String | The MQTT broker address |
| `port` | Number | The MQTT broker port |
| `timeout` | Number | Connection timeout in milliseconds |
| `clientId` | String | MQTT Client ID |
| `username` | String | (Optional) MQTT username |
| `password` | String | (Optional) MQTT password |
| `pubTopic` | String | Topic to publish requests to |
| `subTopic` | String | Topic to subscribe for responses |
| `lwtTopic` | String | Last Will and Testament topic |
