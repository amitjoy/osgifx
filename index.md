---
layout: default
---

OSGi.fx is an easy-to-use application to remotely manage OSGi frameworks. Similar to Felix web console, this is an endeavour for desktop application users to provide all the necessary functionalities to remotely manage OSGi runtimes.

#### Application Screenshots


<img src="https://user-images.githubusercontent.com/13380182/152663681-615aaf56-9945-41d2-9861-e68880da9f3f.png" />
<img src="https://user-images.githubusercontent.com/13380182/153308167-eba08b08-ad74-4e8c-8032-f7864bd93288.png" />
<img src="https://user-images.githubusercontent.com/13380182/152663696-cce697a0-610f-4b7c-8306-843e926cdbd5.png" />

--------------------------------------------------------------------------------------------------------------

#### Implemented Features

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
| Install external feature (plugin)   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| List and uninstall already installed feature(s)   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Generate OBR XML   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |


#### Tools and Technologies

|                      	|                                             	|
|----------------------	|---------------------------------------------	|
| Java (Application)    | 17                                         	|
| Java (Agent)          | 1.8                                         	|
| Rich Client Platform 	| JavaFX 17                                    	|
| Runtime Frameworks   	| OSGi (Equinox), Eclipse 4 (e4), e(fx)clipse 	|
| UI Libraries         	| ControlsFX, TilesFX, FormsFX                  |
| Tools                	| Bndtools 6.2                                  |

--------------------------------------------------------------------------------------------------------------

#### Important Notes for Download and Update

* The application is distributed through [jdeploy](https://www.jdeploy.com) enabling developers to distribute native applications effortlessly
* Note that, the required VM will directly be downloaded while installing the application using `jdeploy`
* Also note that, if the auto-update feature is enabled, every new version will be automatically downloaded while starting the application
* Due to the update of the application, the application might not work expectedly as the old bundle cache still exists. That's why, make sure to delete the existing OSGi storage area located in `~/.osgifx-ws`

--------------------------------------------------------------------------------------------------------------

#### Minimum Requirements for Runtime Agent

1. Java 1.8
2. OSGi R6

To use the agent in the OSGi environment, you need to install `com.osgifx.console.agent.jar` and set `osgi.fx.agent.port` system property in the runtime. Note that, you can either set the property to any port e.g. `2000` or `0.0.0.0:2000`. The latter one will allow remote connections whereas the former one will only allow connections from `localhost`.

--------------------------------------------------------------------------------------------------------------

#### Batch Install

You can also install multiple bundles and create multiple configurations in one go. For that, you need to create `fxartifacts` directory in your home folder and keep all bundles and configuration JSON files in it. Then you can choose `Batch Install` from the `Actions` menu
and it will list only the JARs and JSON files from the directory. You can then choose from the list which JARs to install and which configurations to create.

Note that, the configuration JSON files need to comply with [OSGi Configuration Specification](http://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.configurator.html)

--------------------------------------------------------------------------------------------------------------

#### External Feature Development

External plugins or features can easily be developed for `OSGi.fx`. Please have a look at how the bundles with `com.osgifx.console.ui.*` project name pattern are developed. As a starting point, please have a look at the sample [Tic-Tac-Toe feature](https://github.com/amitjoy/osgifx/tree/main/com.osgifx.console.ext.feature.tictactoe). Since `OSGi.fx` has itself been developed using **OSGi** and **Eclipse e4**, you can easily leverage their modular functionalities to build your own features (extensions).

Once the feature is built, you can test it by installing it from the `Help -> Install External Feature` menu option.

Note that, to develop an external feature, you need to provide a ZIP archive comprising one or more feature JSON files. Have a look at [OSGi Features Specification](http://docs.osgi.org/specification/osgi.cmpn/8.0.0/service.feature.html) on how to prepare the JSON files. For every feature, you need to provide the bundles (JARs) in the `bundles` directory inside the archive.

For ease of development, you can use the OSGi.fx workspace to further develop your own features as the workspace comprises a new bnd plugin which will enable you to automatically prepare the ZIP archive from a bndrun file. As an example, please refer to the sample [Tic-Tac-Toe feature](https://github.com/amitjoy/osgifx/tree/main/com.osgifx.console.ext.feature.tictactoe).

--------------------------------------------------------------------------------------------------------------

#### Project Import for Development

1. Install `Bndtools` from Eclipse Marketplace
2. Import all the projects (`File -> Import -> General -> Existing Projects into Workspace` and select `Search for nested projects`)

--------------------------------------------------------------------------------------------------------------

#### Building from Source

Run `./gradlew clean build` in the project root directory

--------------------------------------------------------------------------------------------------------------

#### Developer

[Amit Kumar Mondal](https://github.com/amitjoy) (admin@amitinside.com)

--------------------------------------------------------------------------------------------------------------

#### Contribution

[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/amitjoy/osgifx-console/issues)

--------------------------------------------------------------------------------------------------------------

#### License

[![License](http://img.shields.io/badge/license-Apache-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)