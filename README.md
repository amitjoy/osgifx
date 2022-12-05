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

<img src="screenshots/1.png" />
<img src="screenshots/2.png" />
<img src="screenshots/3.png" />
<img src="screenshots/4.png" />
<img src="screenshots/5.png" />
<img src="screenshots/6.png" />

------------------------------------------------------------------------------------------------------------

### Tools and Technologies

|                      	|                                             	    |
|----------------------	|---------------------------------------------	    |
| Java (Application)    | 17                                         	    |
| Java (Agent)          | 1.8                                         	    |
| Rich Client Platform 	| JavaFX 17                                    	    |
| Runtime Frameworks   	| OSGi R8 (Equinox), Eclipse 4 (e4), e(fx)clipse 	|
| UI Libraries         	| ControlsFX, TilesFX, FormsFX                       |
| Tools                	| Bndtools 6.3.1                                     |

------------------------------------------------------------------------------------------------------------

### Latest Version

The latest released version: 2.2.9 (Check [Project Website](https://osgifx.com) to download)

------------------------------------------------------------------------------------------------------------

### Features

|                                                                                                                                                                           |     |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------  |:-:  |
| List all installed bundles and fragments                                                                                                                                          |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| List all exported and imported packages                                                                                                                                         |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| List all registered services                                                                                                                                                      |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| List all registered DS components                                                                                                                                                 |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| List all available system and framework properties                                                                                                                                |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| List all daemon and non-daemon threads                                                                                                                                            |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Send synchronous or asynchronous events on demand                                                                                                                                 |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Receive events on demand (option to start and stop receiving events)                                                                                                              |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Receive logs on demand (option to start and stop receiving logs)                                                                                                              |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Manage R7 Logger Configurations                                                                                   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Execute Gogo command                                                                                                                                                              |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Execute CLI command                                                                                                                                                              |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Auto-completion of all available remote Gogo commands during command execution                                                                                                    |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Install or update bundles                                                                                                                                                          |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Drag and drop support of bundles (on Install Bundle Dialog) while installing or updating                                                                                          |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| List available configurations from `ConfigurationAdmin`                                                                                                                             |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| List `Metatype` property descriptors                                                                                                                                            |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Start/stop/uninstall bundle or fragment                                                                                                                                           |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Enable/disable DS component                                                                                                                                                       |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Update/delete existing configuration                                                                                                                                               |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Create new configuration using metatype descriptor                                                                                                                                 |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Overview of the remote OSGi framework (memory consumption, uptime, framework information, number of bundles, number of threads, number of services and number of DS components)   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Generate dependency graph for bundles   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Generate dependency graph for DS components   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Find all cycles between available DS components   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Export generated dependency graph to DOT (GraphViz) format (Right click on generated graph) |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Open Diagnostics (Show application log file)  |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Show suspicious classloader leaks 	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| Show HTTP runtime components (Servlets, Listeners, Filters, Resources and Error Pages) 	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| Shows heap usage over time and the count of garbage collections 	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| Install extension (plugin)   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| List and uninstall already installed extension(s)   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Generate OBR XML   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Device Management Tree (DMT) Traversal and Update   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Manage User Admin roles   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Execute Felix Healthchecks   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Capture/Read Snapshot   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Display OSGi Runtime DTOs   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Advanced Search   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |

--------------------------------------------------------------------------------------------------------------

### Developer

Amit Kumar Mondal (admin@amitinside.com)

--------------------------------------------------------------------------------------------------------------

### Contribution [![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/amitjoy/osgifx-console/issues)

Want to contribute? Great! Check out [Contribution Guide](https://github.com/amitjoy/osgifx-console/blob/main/CONTRIBUTING.md)

--------------------------------------------------------------------------------------------------------------

### License

This project is licensed under Apache License Version 2.0 [![License](http://img.shields.io/badge/license-Apache-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

--------------------------------------------------------------------------------------------------------------

### User Guide

For instructions on how to download and install latest version, please refer to the [Project Website](https://osgifx.com).
