<p align="center">
  <img width="600" alt="logo" src="https://user-images.githubusercontent.com/13380182/137404892-2e90dbfb-c20d-4267-b1b9-672597d7f2a2.png" />
  <br/>
  <p align="center">This repository provides a desktop application to remotely manage OSGi runtimes</p>
</p>

-----------------------------------------------------------------------------------------------------------

[![amitjoy - osgifx-console](https://img.shields.io/static/v1?label=amitjoy&message=osgi-fx&color=blue&logo=github)](https://github.com/amitjoy/osgifx-console)
[![stars - osgifx-console](https://img.shields.io/github/stars/amitjoy/osgifx-console?style=social)](https://github.com/amitjoy/osgifx-console)
[![forks - osgifx-console](https://img.shields.io/github/forks/amitjoy/osgi-messaging?style=social)](https://github.com/amitjoy/osgifx-console)
[![License - Apache](https://img.shields.io/badge/License-Apache-blue)](#license)
[![Build - Passing](https://img.shields.io/badge/Build-Passing-brightgreen)](https://github.com/amitjoy/osgifx-console/runs/1485969918)
[![GitHub release](https://img.shields.io/github/release/amitjoy/osgifx-console?include_prereleases&sort=semver)](https://github.com/amitjoy/osgifx-console/releases/)

------------------------------------------------------------------------------------------------------------

![1](https://user-images.githubusercontent.com/13380182/137404709-f567056b-59e9-4298-943c-515ac624c961.png)
![2](https://user-images.githubusercontent.com/13380182/137404712-c41ec2a2-3561-4aa0-8061-41f02e8c5819.png)
![3](https://user-images.githubusercontent.com/13380182/137404714-46e64fe5-4a73-41e4-878d-6557e364fafa.png)
![4](https://user-images.githubusercontent.com/13380182/137404717-8d97c245-e03f-42f0-9ebb-e4d9dd131ba0.png)
![5](https://user-images.githubusercontent.com/13380182/137404719-05db8a30-0e97-4b37-bce8-1b03339c491c.png)
![6](https://user-images.githubusercontent.com/13380182/137404724-949e9f07-c0d6-47c7-bf35-7ebbc1b64d2b.png)
![7](https://user-images.githubusercontent.com/13380182/137404725-61553ff7-32c3-45ec-99bb-1c55bf4e03ab.png)
![8](https://user-images.githubusercontent.com/13380182/137404726-eb341a40-ca9d-4bb5-981c-c182f37ec9e0.png)

--------------------------------------------------------------------------------------------------------------

### Tools and technologies for the desktop application

1. Java 8
2. Equinox
3. Eclipse 4
5. JavaFX
6. e(fx)clipse
7. Bndtools

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

### Contribution [![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/amitjoy/osgifx-console/issues)

Want to contribute? Great! Check out [Contribution Guide](https://github.com/amitjoy/osgifx-console/blob/main/CONTRIBUTING.md)

--------------------------------------------------------------------------------------------------------------

### License

This project is licensed under Apache License Version 2.0 [![License](http://img.shields.io/badge/license-Apache-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
