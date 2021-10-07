<!--p align="center">
  <img width="563" alt="logo" src="https://user-images.githubusercontent.com/13380182/101778008-90754300-3af3-11eb-95da-91c54608f277.png" />
</p-->

## OSGi.fx Console

This repository provides a desktop application to remotely manage OSGi runtimes

-----------------------------------------------------------------------------------------------------------

[![amitjoy - osgifx-console](https://img.shields.io/static/v1?label=amitjoy&message=osgi-messaging&color=blue&logo=github)](https://github.com/amitjoy/osgifx-console)
[![stars - osgifx-console](https://img.shields.io/github/stars/amitjoy/osgifx-console?style=social)](https://github.com/amitjoy/osgifx-console)
[![forks - osgifx-console](https://img.shields.io/github/forks/amitjoy/osgi-messaging?style=social)](https://github.com/amitjoy/osgifx-console)
[![License - Apache](https://img.shields.io/badge/License-Apache-blue)](#license)
[![Build - Passing](https://img.shields.io/badge/Build-Passing-brightgreen)](https://github.com/amitjoy/osgifx-console/runs/1485969918)
[![GitHub release](https://img.shields.io/github/release/amitjoy/osgifx-console?include_prereleases&sort=semver)](https://github.com/amitjoy/osgifx-console/releases/)

------------------------------------------------------------------------------------------------------------

### Tools and technologies for the desktop application

1. Java 8
2. Equinox
3. Eclipse 4
5. JavaFX
6. e(fx)clipse
7. bndtools

------------------------------------------------------------------------------------------------------------

### Minimum Requirements for remote runtime

1. Java 8
2. OSGi R6

------------------------------------------------------------------------------------------------------------

### Installation

To use it in the OSGi environment, you need to install `in.bytehue.osgifx.console.agent` and set `osgi.fx.agent.port` system property in the runtime

--------------------------------------------------------------------------------------------------------------

#### Project import for development

**Import as Eclipse Projects**

1. Install Bndtools
2. Import all the projects (`File -> Import -> General -> Existing Projects into Workspace`)

--------------------------------------------------------------------------------------------------------------

#### Building from Source

Run `./gradlew clean build` in the project root directory

--------------------------------------------------------------------------------------------------------------

### Developer

Amit Kumar Mondal (admin@amitinside.com)

--------------------------------------------------------------------------------------------------------------

### Contribution [![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/amitjoy/osgi-messaging/issues)

Want to contribute? Great! Check out [Contribution Guide](https://github.com/amitjoy/osgi-messaging/blob/master/CONTRIBUTING.md)

--------------------------------------------------------------------------------------------------------------

### License

This project is licensed under Apache License Version 2.0 [![License](http://img.shields.io/badge/license-Apache-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
