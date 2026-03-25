---
layout: default
title: Eclipse Plugin
permalink: /eclipse-plugin
---

# Eclipse Plugin

Bring the power of **OSGi.fx** directly into your development environment with the **[OSGi.fx Eclipse Plugin](https://github.com/amitjoy/osgifx-eclipse-plugin)** — no separate desktop app installation required.

<div style="display: flex; flex-wrap: wrap; gap: 1rem; justify-content: center; margin: 2rem 0;">
  <a href="https://marketplace.eclipse.org/content/osgifx" class="btn btn-primary" target="_blank" style="font-size: 1.2rem; padding: 0.75rem 1.5rem;">
    View on Eclipse Marketplace
  </a>
  <a href="https://github.com/amitjoy/osgifx-eclipse-plugin" class="btn btn-secondary" target="_blank" style="font-size: 1.2rem; padding: 0.75rem 1.5rem;">
    View on GitHub
  </a>
</div>

## Plugin Highlights

-   **⚡ Seamless Launch:** Start the OSGi.fx console directly from Eclipse.
-   **🛠️ Connection Profiles:** Manage your **Socket** and **MQTT** connections within the IDE.
-   **⚙️ Auto-Managed Runtime:** Automatic setup of the required **JavaFX 25** environment — no manual JVM configuration needed.
-   **🎨 Visual Status:** Real-time tracking of connection status with intuitive icons.
-   **🛡️ Standalone Independence:** The plugin runs entirely separate from the desktop application — no standalone app install required.

> [!NOTE]
> **Minimum Requirements:** Eclipse IDE for RCP/RAP Developers (2022-06 or later) with Java 17+.

## Installation

The easiest way to install is via the **Eclipse Marketplace** — simply click **Install** from the marketplace listing and restart Eclipse.

If you prefer manual installation (e.g., for a specific version or air-gapped environments), use `Help > Install New Software...` and add one of the following update sites:

-   **🚀 Latest Release:** `https://osgifx.com/eclipse`
-   **🧪 Staging / Beta:** `https://osgifx.com/eclipse-staging`

<div style="display: flex; flex-wrap: wrap; gap: 1rem; justify-content: center; margin: 2rem 0;">
  <a href="https://marketplace.eclipse.org/content/osgifx" class="btn btn-primary" target="_blank" style="font-size: 1.2rem; padding: 0.75rem 1.5rem;">
    View on Eclipse Marketplace
  </a>
  <a href="https://github.com/amitjoy/osgifx-eclipse-plugin" class="btn btn-secondary" target="_blank" style="font-size: 1.2rem; padding: 0.75rem 1.5rem;">
    View on GitHub
  </a>
</div>

## Plugin Features in Action

### Connect Dialog

Seamlessly manage and connect to your remote OSGi frameworks using the dedicated connection dialog integrated within your Eclipse workspace.

<div style="text-align: center; margin: 2rem 0;">
  <img src="{{ "/screenshots/osgifx_plugin_dialog.png" | relative_url }}" alt="OSGi.fx Connect Dialog" style="border: 1px solid #e2e8f0; border-radius: 0.5rem; max-width: 100%; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);" />
</div>

### Preferences Configuration

Tailor the plugin's behavior to your requirements, such as configuring the JVM and OSGi.fx launcher settings, conveniently through Eclipse Preferences.

<div style="text-align: center; margin: 2rem 0;">
  <img src="{{ "/screenshots/osgifx_plugin_preferences.png" | relative_url }}" alt="OSGi.fx Preferences Dialog" style="border: 1px solid #e2e8f0; border-radius: 0.5rem; max-width: 100%; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);" />
</div>

---

### Related Pages

*   [Headless Launch](/headless-launch) — the underlying mechanism powering plugin connections.
*   [Remote Agent Documentation](/agent)
*   [Getting Started](/)
