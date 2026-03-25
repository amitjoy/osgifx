---
layout: default
title: Extension Development
permalink: /extension-dev
---

# Extension Development

External plugins or extensions can easily be developed for **OSGi.fx**. Since **OSGi.fx** has itself been developed using **OSGi** and **Eclipse e4**, you can easily leverage their modular capabilities to build your own extensions.

## Getting Started

See how the bundles with the `com.osgifx.console.ui.*` project name pattern are developed as a reference.

> [!TIP]
> As a starting point, refer to the sample [Tic-Tac-Toe Extension](https://github.com/amitjoy/osgifx/tree/main/com.osgifx.console.extension.ui.tictactoe).

## Installation

Once the extension is developed, install it via **`Actions -> Install Extension`** in the OSGi.fx menu.

---

## Deployment Packages

To develop an extension, you need to provide an **OSGi Deployment Package** archive. 
- Have a look at [OSGi Deployment Admin Specification](http://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.deploymentadmin.html) on how to prepare such deployment packages.

## Developer Workspace

For ease of development, use the **[OSGi.fx workspace](https://github.com/amitjoy/osgifx)** to build your own extensions. The workspace includes a bnd plugin that automatically generates a deployment package from a `bndrun` file.

---

## Agent Extension SPI

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

The [supervisor (OSGi.fx)](/agent) can then invoke your extension via `agent.executeExtension("my-custom-extension", context)`.

---

## Large Payload Handling (SPI)

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

### Related Pages

*   [Remote Agent Documentation](/agent) — full SPI reference and architecture details.
*   [Getting Started](/)
