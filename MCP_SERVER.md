# OSGi.fx Console MCP Server

An [MCP (Model Context Protocol)](https://modelcontextprotocol.io/) server that connects LLMs (like Claude) to a running OSGi framework. This allows you to diagnose, monitor, and debug remote OSGi runtimes using natural language.

## Features

* **🔍 Deep Diagnostics:** Inspect bundles, services, components (DS), and configurations.
* **🛡️ Safety First:** Read-only by default. Dangerous Gogo commands are blocked.
* **📊 Real-time Monitoring:** Check heap usage, threads, and memory pools.
* **⚡ Log Analysis:** Fetch and filter logs by time range or count.

## Getting Started

### 1. Enable the MCP Server
The MCP server is built into the **OSGi.fx** application.

1.  Launch **OSGi.fx**.
2.  Connect to your remote OSGi framework.
3.  Go to the **Actions** menu and select **Start MCP Server**.
4.  The server will start on port `8080` (default) or your configured port.

### 2. Client Configuration (SSE)

Configure your MCP client (e.g., Claude Desktop) to connect via Server-Sent Events (SSE).

**Example Configuration:**

```json
{
  "mcpServers": {
    "osgifx": {
      "serverUrl": "http://localhost:8080/mcp",
      "type": "sse",
      "disabled": false
    }
  }
}
```

## Available Tools

| Tool Name | Description |
| --- | --- |
| **`list_bundles`** | Lists all installed OSGi bundles with state, version, and ID. |
| **`list_services`** | Lists all registered services and their properties. |
| **`list_components`** | Lists Declarative Services (DS) components and their satisfaction state. |
| **`list_configurations`** | Lists OSGi Configuration Admin configurations (PIDs). |
| **`list_http_components`** | Lists registered Servlets, Filters, and Resources (HttpService). |
| **`list_threads`** | Lists JVM threads with states (use for deadlock detection). |
| **`get_system_properties`** | Retrieves Java System Properties (e.g., `java.version`) and Framework properties. |
| **`get_heap_usage`** | Returns current JVM heap memory statistics. |
| **`check_memory_usage`** | Detailed breakdown of Heap, Non-Heap, and Memory Pools. |
| **`fetch_log_snapshot`** | Retrieves system logs. Supports filtering by time range or count. |
| **`analyze_classloader_leaks`** | (Heavy Operation) Analyzes the heap for potential bundle classloader leaks. |
| **`run_health_checks`** | Executes Felix Health Checks. |
| **`run_gogo_command`** | Executes raw Gogo shell commands (restricted access). |
| **`ping_agent`** | Verifies connectivity to the remote OSGi agent. |
| **`read_dmt_node`** | Reads values and metadata from a specific Device Management Tree (DMT) node URI. |
| **`run_garbage_collection`** | Triggers a System.gc() on the remote JVM. |
| **`capture_heap_dump`** | Captures a HPROF heap dump from the remote JVM. |
| **`start_bundle`** | Starts the bundle with the specified ID. |
| **`stop_bundle`** | Stops the bundle with the specified ID. |
| **`get_bundle_headers`** | Retrieves the manifest headers for a specific bundle ID. |
| **`enable_component`** | Enables a Declarative Services (DS) component by name. |
| **`disable_component`** | Disables a Declarative Services (DS) component by name. |
| **`get_component_details`** | Retrieves detailed information for a specific component. |
| **`update_configuration`** | Updates or creates an OSGi configuration (PID & properties). |
| **`send_event`** | Sends an OSGi event to a topic. |
| **`update_logger_context`** | Updates Logger Context log levels for a bundle. |
| **`install_bundle`** | Installs a bundle from a URL. Returns Bundle ID. |
| **`uninstall_bundle`** | Uninstalls the bundle with the specified ID. |
| **`get_bundle_revisions`** | Retrieves detailed revision info (wiring/capabilities) for a bundle. |
| **`delete_configuration`** | Deletes an OSGi configuration by PID. |
| **`execute_agent_extension`** | Executes a named Agent Extension with a context map. |

## System Prompt / Guiding Principles

When configuring your LLM (e.g., Claude), provides these principles to ensure safe and effective operation:

```markdown
### Guiding Principles

1.  **Diagnostic First:** Always inspect the state (`list_bundles`, `get_component_details`) *before* attempting to change it.
2.  **Action Confirmation:** You are authorized to perform state-changing operations (`start`, `stop`, `update`) when explicitly requested by the user. 
    * *Caution:* If a user asks to "fix it," explain what you plan to do (e.g., "I will restart the bundle") before calling the tool.
3.  **Terminology Precision:**
    * **"Properties"**: Java System Properties.
    * **"Configurations"**: OSGi Config Admin (PIDs).
```

## Safety Constraints

*   **Restricted Commands:** State-changing Gogo commands (e.g., `stop`, `uninstall`, `update`) are **blocked** in `run_gogo_command` to encourage using the dedicated tools (`stop_bundle`).
*   **Heap Dumps:** The `capture_heap_dump` tool returns large binary blobs; ensure your client can handle them.

## Troubleshooting

**"Agent is not connected"**
Ensure the OSGi Agent bundle is installed and running on the target framework and that the supervisor is configured to connect to the correct host/port.

**"Tool execution timed out"**
Heavy operations like `analyze_classloader_leaks` or `capture_heap_dump` may take time. Ask the model to wait or try again.
