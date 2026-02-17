---
layout: default
title: Development
permalink: /development
---

# Development Guide

## Project Import for Development

To setup the development environment for **OSGi.fx**, follow these steps:

1.  **Install JDK 25**: Download and install **Java 25 JDK** (ensure it includes JavaFX modules or install them separately).
    > [!NOTE]
    > Currently, distributions like [Zulu](https://www.azul.com/downloads/?version=java-25&package=jdk-fx) often include JavaFX modules.
2.  **Set JAVA_HOME**: specific the newly installed JDK 25 to your `JAVA_HOME`.
3.  **Install Eclipse**: Get `Eclipse IDE for RCP/RAP Developers` and make sure it starts with your JDK 25.
4.  **Install Plugins**: Install `Bndtools` and `e(fx)clipse` from the Eclipse Marketplace.
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

---

## Contributing

We welcome contributions! 

- **Issue Tracker**: [Report Bugs / Request Features](https://github.com/amitjoy/osgifx/issues)
- **Maintainer**: [Amit Kumar Mondal](https://github.com/amitjoy) (admin@amitinside.com)

[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/amitjoy/osgifx/issues)
