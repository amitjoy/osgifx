<p align="center">
  <img width="600" alt="logo" src="https://user-images.githubusercontent.com/13380182/137404892-2e90dbfb-c20d-4267-b1b9-672597d7f2a2.png" />
  <br/>
  <p align="center">This repository provides a desktop application to remotely manage OSGi runtimes</p>
</p>
<p align="center">
  <a href="https://github.com/amitjoy/osgifx-console"><img width="170" alt="logo" src="https://img.shields.io/static/v1?label=amitjoy&message=osgi-fx-console&color=blue&logo=github" /></a>
  <a href="https://github.com/amitjoy/osgifx-console"><img width="80" alt="logo" src="https://img.shields.io/github/stars/amitjoy/osgifx-console?style=social" /></a>
  <a href="https://github.com/amitjoy/osgifx-console"><img width="80" alt="logo" src="https://img.shields.io/github/forks/amitjoy/osgi-messaging?style=social" /></a>
  <a href="#license"><img width="110" alt="logo" src="https://img.shields.io/badge/License-Apache-blue" /></a>
  <a href="https://github.com/amitjoy/osgifx-console/runs/1485969918"><img width="95" alt="logo" src="https://img.shields.io/badge/Build-Passing-brightgreen" /></a>
  <a href="https://github.com/amitjoy/osgifx-console/releases/"><img width="134" alt="logo" src="https://img.shields.io/github/release/amitjoy/osgifx-console?include_prereleases&sort=semver" /></a>
  </p>

![1](https://user-images.githubusercontent.com/13380182/137404709-f567056b-59e9-4298-943c-515ac624c961.png)
![2](https://user-images.githubusercontent.com/13380182/137404712-c41ec2a2-3561-4aa0-8061-41f02e8c5819.png)
![3](https://user-images.githubusercontent.com/13380182/137404714-46e64fe5-4a73-41e4-878d-6557e364fafa.png)
![4](https://user-images.githubusercontent.com/13380182/137404717-8d97c245-e03f-42f0-9ebb-e4d9dd131ba0.png)
![5](https://user-images.githubusercontent.com/13380182/137404719-05db8a30-0e97-4b37-bce8-1b03339c491c.png)
![6](https://user-images.githubusercontent.com/13380182/137404724-949e9f07-c0d6-47c7-bf35-7ebbc1b64d2b.png)
![7](https://user-images.githubusercontent.com/13380182/137404725-61553ff7-32c3-45ec-99bb-1c55bf4e03ab.png)
![8](https://user-images.githubusercontent.com/13380182/137404726-eb341a40-ca9d-4bb5-981c-c182f37ec9e0.png)

------------------------------------------------------------------------------------------------------------

### Tools and Technologies

|                      	|                                             	|
|----------------------	|---------------------------------------------	|
| Java                 	| 1.8                                         	|
| Rich Client Platform 	| JavaFX 8                                    	|
| Runtime Frameworks   	| OSGi (Equinox), Eclipse 4 (e4), e(fx)clipse 	|
| UI Libraries         	| ControlsFX, TilesFX, FormsFX                  |
| Tools                	| Bndtools 6                                  	|

------------------------------------------------------------------------------------------------------------

### Features

|                                                                                                                                                                         	|   	|
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------	|:-:	|
| List all installed bundles and fragments                                                                                                                                        	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| List all exported and imported packages                                                                                                                                       	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| List all registered services                                                                                                                                                    	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| List all registered DS components                                                                                                                                               	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| List all available system and framework properties                                                                                                                              	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| List all daemon and non-daemon threads                                                                                                                                          	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| Receive events on demand (option to start and stop receiving events)                                                                                                            	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| Execute Gogo command                                                                                                                                                            	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| Auto-completion of all available remote Gogo commands during command execution                                                                                                    |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| Install or update bundle                                                                                                                                                        	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| Drag and drop support of bundles (on Install Bundle Dialog) while installing or updating                                                                                        	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| List available configurations from `ConfigurationAdmin`                                                                                                                             |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| List property descriptors (`Metatype`)                                                                                                                                            |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| Start/stop/uninstall bundle or fragment                                                                                                                                         	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| Enable/disable DS component                                                                                                                                                     	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|
| Update/delete existing configuration                                                                                                                                            	 |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	 |
| Create new configuration using metatype descriptor                                                                                                                              	 |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	 |
| Overview of the remote OSGi framework (memory consumption, uptime, framework information, number of bundles, number of threads, number of services and number of DS components) 	|  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png) 	|

--------------------------------------------------------------------------------------------------------------

### Minimum Requirements for Runtime Agent

1. Java 8
2. OSGi R6

------------------------------------------------------------------------------------------------------------

### Installation of Agent

To use the agent in the OSGi environment, you need to install `in.bytehue.osgifx.console.agent` and set `osgi.fx.agent.port` system property in the runtime

--------------------------------------------------------------------------------------------------------------

### Project Import for Development

1. Install Bndtools from Eclipse Marketplace
2. Import all the projects (`File -> Import -> General -> Existing Projects into Workspace` and select `Search for nested projects`)

--------------------------------------------------------------------------------------------------------------

### Building from Source

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
