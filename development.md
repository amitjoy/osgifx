---
layout: default
title: Development
permalink: /development
---

# Development Guide

## Project Import for Development

To setup the development environment for **OSGi.fx**, follow these steps:

1.  **Install JDK 25**: Download and install **Java 25 JDK** (ensure it includes JavaFX modules or install them separately). This is required only for starting **OSGi.fx** as it uses Java 25 as its base requirement.
    > [!NOTE]
    > Currently, distributions like [Zulu](https://www.azul.com/downloads/?version=java-25&package=jdk-fx) often include JavaFX modules.
2.  **Set JAVA_HOME**: Point your `JAVA_HOME` to the newly installed JDK 25.
3.  **Install Eclipse**: Get `Eclipse IDE for RCP/RAP Developers`. Note that Eclipse can start with other lower Java versions (e.g., Java 17 or 21), but JDK 25 must be used within Eclipse for **OSGi.fx** projects.
4.  **Install Plugins**: Install `Bndtools` from the Eclipse Marketplace.
5.  **Configure Compiler**: Go to `Java -> Compiler` in Preferences and set Compliance Level to **25**.
6.  **Configure JREs**: 
    - Go to `Java -> Installed JREs` and add your JDK 25.
    - Go to `Java -> Installed JREs -> Execution Environments` and match `JavaSE-25` to your installed JDK.
7.  **Import Projects**: `File -> Import -> General -> Existing Projects into Workspace` and select `Search for nested projects`.

## Building from Source

To build the application from the command line:

```bash
./gradlew clean build
```

The source code is available on [GitHub](https://github.com/amitjoy/osgifx).

---

## Contributing

Contributions of all kinds are welcomed and appreciated!

- **Issue Tracker**: [Report Bugs / Request Features](https://github.com/amitjoy/osgifx/issues)
- **Source Code**: [github.com/amitjoy/osgifx](https://github.com/amitjoy/osgifx)
- **Maintainer**: [Amit Kumar Mondal](https://github.com/amitjoy) (admin@amitinside.com)

[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/amitjoy/osgifx/issues)

---

### Related Pages

*   [Remote Agent Documentation](/agent)
*   [Getting Started](/)
