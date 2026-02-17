<p align="center">
  <img width="300" alt="logo" src="screenshots/logo.png" />
  <br/>
  <p align="center">An easy-to-use desktop application to manage OSGi frameworks remotely</p>
</p>
<p align="center">
  <a href="https://osgifx.com"><img width="120" alt="logo" src="https://img.shields.io/static/v1?label=amitjoy&message=OSGi.fx&color=blue&logo=github" /></a>
  <a href="https://github.com/amitjoy/osgifx"><img width="80" alt="logo" src="https://img.shields.io/github/stars/amitjoy/osgifx?style=social" /></a>
  <a href="https://github.com/amitjoy/osgifx"><img width="80" alt="logo" src="https://img.shields.io/github/forks/amitjoy/osgifx?style=social" /></a>
  <a href="#license"><img width="110" alt="logo" src="https://img.shields.io/badge/License-Apache-blue" /></a>
  <img width="130" alt="logo" src="https://github.com/amitjoy/osgifx/actions/workflows/build.yml/badge.svg" />
  <a href="https://github.com/amitjoy/osgifx-console/releases/"><img width="100" alt="logo" src="https://img.shields.io/github/release/amitjoy/osgifx-console?include_prereleases&sort=semver" /></a>
  </p>

### ❓ Why OSGi.fx?

**OSGi.fx** is the ultimate remote management tool for your OSGi frameworks. Unlike legacy consoles, it offers a **modern, responsive JavaFX interface** that gives you:
*   **🚀 Deep Insights:** Visualize bundles, services, users, and components instantly.
*   **⚡ Real-time Monitoring:** Track threads, heap usage, and logs live.
*   **�️ Visual Dependencies:** Explore complex relationships with interactive graphs.
*   **✨ Ease of Use:** Drag-and-drop installs, smart auto-complete, and valid configuration editing.

------------------------------------------------------------------------------------------------------------

### �📸 Gallery
<div align="center">
  <img src="screenshots/1.png" width="45%" />
  <img src="screenshots/2.png" width="45%" />
  <img src="screenshots/3.png" width="45%" />
  <img src="screenshots/4.png" width="45%" />
  <img src="screenshots/5.png" width="45%" />
  <img src="screenshots/6.png" width="45%" />
</div>

------------------------------------------------------------------------------------------------------------

### 🛠️ Tech Stack

| Component | Technology |
| :--- | :--- |
| **☕ Java (Application)** | 25 |
| **☕ Java (Agent)** | 1.8 |
| **🖥️ Rich Client Platform** | JavaFX 25 |
| **⚙️ Runtime (Application)** | OSGi R8 (Equinox), Eclipse 4 (e4), e(fx)clipse |
| **⚙️ Runtime (Agent)** | OSGi R6 |
| **🎨 UI Libraries** | ControlsFX, TilesFX, FormsFX |
| **🛠️ Software Tools** | Bndtools 7.2.1 |

------------------------------------------------------------------------------------------------------------

### 🚀 Getting Started

The latest released version is **2.4.5**.

#### 📥 Download & Install
Visit the [Project Website](http://osgifx.com) to download and install the latest version for your platform.

For detailed instructions, please refer to the **User Guide** on the website or check our local [User Guide](GUIDE.md).

------------------------------------------------------------------------------------------------------------

### ✨ Features

| Feature | Version | Implemented | Released |
| :--- | :---: | :---: | :---: |
| **📦 Bundle Inventory** <br> _View all installed bundles and fragments_ | 2.4.4 | ✅ | 🚀 |
| **📦 Package Insights** <br> _Explore exported and imported packages_ | 2.4.4 | ✅ | 🚀 |
| **🛠️ Service Registry** <br> _Inspect all registered OSGi services_ | 2.4.4 | ✅ | 🚀 |
| **🧩 Component Viewer** <br> _Visualize declarative services (DS) components_ | 2.4.4 | ✅ | 🚀 |
| **⚙️ System Properties** <br> _Access robust system and framework properties_ | 2.4.4 | ✅ | 🚀 |
| **🧵 Thread Monitor** <br> _Track daemon and non-daemon threads_ | 2.4.4 | ✅ | 🚀 |
| **📢 Event Emitter** <br> _Dispatch synchronous or asynchronous events_ | 2.4.4 | ✅ | 🚀 |
| **👂 Event Listener** <br> _Subscribe to OSGi events in real-time_ | 2.4.4 | ✅ | 🚀 |
| **📝 Log Stream** <br> _Live streaming of OSGi logs_ | 2.4.4 | ✅ | 🚀 |
| **logger R7 Logger Config** <br> _Manage OSGi R7 logger levels and configurations_ | 2.4.4 | ✅ | 🚀 |
| **🐚 Gogo Shell** <br> _Execute Gogo commands remotely_ | 2.4.4 | ✅ | 🚀 |
| **💻 CLI Executor** <br> _Run system CLI commands directly_ | 2.4.4 | ✅ | 🚀 |
| **✨ Smart Auto-Complete** <br> _Intelligent suggestion for remote Gogo commands_ | 2.4.4 | ✅ | 🚀 |
| **📥 Bundle Manager** <br> _Install, update, starting and stopping bundles_ | 2.4.4 | ✅ | 🚀 |
| **🖱️ Drag & Drop Install** <br> _Effortless bundle installation via drag-and-drop_ | 2.4.4 | ✅ | 🚀 |
| **🔧 Config Admin** <br> _Manage configurations via `ConfigurationAdmin`_ | 2.4.4 | ✅ | 🚀 |
| **📋 Metatype Inspector** <br> _Browse OCDs and property descriptors_ | 2.4.4 | ✅ | 🚀 |
| **⏯️ Bundle Lifecycle** <br> _Start, stop, and uninstall bundles/fragments_ | 2.4.4 | ✅ | 🚀 |
| **⚡ Component Control** <br> _Enable or disable DS components on the fly_ | 2.4.4 | ✅ | 🚀 |
| **✏️ Config Editor** <br> _Create, update, and delete configurations_ | 2.4.4 | ✅ | 🚀 |
| **🏗️ Config Factory** <br> _Instantiate new configurations from factory PIDs_ | 2.4.4 | ✅ | 🚀 |
| **📊 Runtime Dashboard** <br> _Overview of memory, uptime, bundles, threads, and services_ | 2.4.4 | ✅ | 🚀 |
| **🕸️ Bundle Graph** <br> _Visualize bundle dependencies interactively_ | 2.4.4 | ✅ | 🚀 |
| **🔗 Component Graph** <br> _Visualize DS component references and dependencies_ | 2.4.4 | ✅ | 🚀 |
| **🔄 Cycle Detector** <br> _Identify circular dependencies in DS components_ | 2.4.4 | ✅ | 🚀 |
| **📤 Graph Export** <br> _Export dependency graphs to DOT (GraphViz) format_ | 2.4.4 | ✅ | 🚀 |
| **📂 Log Viewer** <br> _Access and analyze application log files_ | 2.4.4 | ✅ | 🚀 |
| **🕵️ Leak Detector** <br> _Identify suspicious classloader leaks_ | 2.4.4 | ✅ | 🚀 |
| **🌐 HTTP Runtime** <br> _Inspect Servlets, Filters, and Resources_ | 2.4.4 | ✅ | 🚀 |
| **📈 Heap Monitor** <br> _Real-time heap usage and GC tracking_ | 2.4.4 | ✅ | 🚀 |
| **🔌 Extension Manager** <br> _Install and manage external plugins_ | 2.4.4 | ✅ | 🚀 |
| **🗑️ Extension Uninstaller** <br> _Remove installed extensions easily_ | 2.4.4 | ✅ | 🚀 |
| **📜 OBR Generator** <br> _Generate OBR XML repositories_ | 2.4.4 | ✅ | 🚀 |
| **🌲 DMT Explorer** <br> _Traverse and update the Device Management Tree_ | 2.4.4 | ✅ | 🚀 |
| **👥 User Admin** <br> _Manage roles, users, and groups_ | 2.4.4 | ✅ | 🚀 |
| **❤️ Health Checks** <br> _Execute and monitor Felix Health Checks_ | 2.4.4 | ✅ | 🚀 |
| **📸 Snapshot** <br> _Capture and analyze runtime state snapshots_ | 2.4.4 | ✅ | 🚀 |
| **ℹ️ DTO Inspector** <br> _Explore standard OSGi Runtime DTOs_ | 2.4.4 | ✅ | 🚀 |
| **🔍 Advanced Search** <br> _Powerful search across the OSGi framework_ | 2.4.4 | ✅ | 🚀 |
| **🤖 MCP Support** <br> _Model Context Protocol integration for AI agents. [Read More](MCP_SERVER.md)_ | 2.4.5 | ✅ | 🚀 |
| **👻 Headless Launch** <br> _Start application with pre-configured connection_ | 2.4.5 | ✅ | 🚀 |

--------------------------------------------------------------------------------------------------------------

### 💡 Troubleshooting & Tips

*   **👻 Headless Mode:** Starting from 2.4.5, need to connect without the connection wizard? Use the `-Dosgifx.config=/path/to/config.json` system property to launch OSGi.fx with a pre-defined connection. See the [Headless Launch Documentation](HEADLESS_LAUNCH.md).
*   **🤖 AI Assistance:** OSGi.fx 2.4.5 supports the **Model Context Protocol (MCP)**, allowing AI agents to connect to and debug your OSGi runtime directly! See the [MCP Server Documentation](MCP_SERVER.md).

--------------------------------------------------------------------------------------------------------------

### 👨‍💻 Maintainer

Amit Kumar Mondal (admin@amitinside.com)

--------------------------------------------------------------------------------------------------------------

### 🤝 Contributing [![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/amitjoy/osgifx-console/issues)

Want to contribute? Great! Check out [Contribution Guide](https://github.com/amitjoy/osgifx-console/blob/main/CONTRIBUTING.md)

<a href="eclipse+installer:https://raw.githubusercontent.com/amitjoy/osgifx/refs/heads/main/oomph/config_osgifx.setup">
            <img src="https://img.shields.io/static/v1?logo=eclipseide&label=eclipse%20for%20osgifx&message=branch%20:%20main&style=for-the-badge&logoColor=white&labelColor=963508&color=gray"
                alt="osgifx workspace" /></a>

--------------------------------------------------------------------------------------------------------------

### 📄 License

This project is licensed under Apache License Version 2.0 [![License](http://img.shields.io/badge/license-Apache-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

--------------------------------------------------------------------------------------------------------------


