---
layout: default
---

OSGi.fx is an easy-to-use application to remotely manage OSGi frameworks. Similar to Felix web console, this is an endeavour for desktop application users with all the necessary functionalities required to remotely manage OSGi runtimes.

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
| Install or update bundle                                                                                                                                                          |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Drag and drop support of bundles (on Install Bundle Dialog) while installing or updating                                                                                          |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| List available configurations from `ConfigurationAdmin`                                                                                                                             |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| List property descriptors (`Metatype`)                                                                                                                                            |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Start/stop/uninstall bundle or fragment                                                                                                                                           |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Enable/disable DS component                                                                                                                                                       |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Update/delete existing configuration                                                                                                                                               |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Create new configuration using metatype descriptor                                                                                                                                 |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Overview of the remote OSGi framework (memory consumption, uptime, framework information, number of bundles, number of threads, number of services and number of DS components)   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Generate dependency graph for bundles   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Generate dependency graph for DS components   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Find all cycles between available DS components   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Export generated dependency graph to DOT (GraphViz) format (Right click on generated graph) |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Install external feature (plugin)   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Check and install if updates are available for installed feature(s)   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| List and uninstall already installed feature(s)   |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |
| Open Diagnostics (Show application log file)  |  ![done](https://user-images.githubusercontent.com/13380182/138339309-19f097f7-0f8d-4df9-8c58-c98f0a9acc60.png)   |


#### Application Screenshots

<img src="https://user-images.githubusercontent.com/13380182/152663681-615aaf56-9945-41d2-9861-e68880da9f3f.png" />
<img src="https://user-images.githubusercontent.com/13380182/153308167-eba08b08-ad74-4e8c-8032-f7864bd93288.png" />
<img src="https://user-images.githubusercontent.com/13380182/152663696-cce697a0-610f-4b7c-8306-843e926cdbd5.png" />
<img src="https://user-images.githubusercontent.com/13380182/152663960-5539ada8-d9ab-4ce8-807d-8857a81360c3.png" />
<img src="https://user-images.githubusercontent.com/13380182/152663676-f29732d5-a18f-4c1d-a88c-06aa938a101b.png" />
<img src="https://user-images.githubusercontent.com/13380182/152663677-17ff2ba7-95cf-4113-91b2-12f40f97bb92.png" />
<img src="https://user-images.githubusercontent.com/13380182/152663678-f18513b8-736f-4091-b942-fc4fa1f61bc6.png" />
<img src="https://user-images.githubusercontent.com/13380182/152663680-6a4051f8-fded-41d4-bc2c-06d2d07883f7.png" />
<img src="https://user-images.githubusercontent.com/13380182/152663683-16dc47cc-f0ce-4508-a7cb-6676bebce8bb.png" />
<img src="https://user-images.githubusercontent.com/13380182/152663684-0a3c80ad-e539-43a6-afae-065a653213ab.png" />
<img src="https://user-images.githubusercontent.com/13380182/153308825-7398e4d6-ab26-4860-ac52-61f17039d0b2.png" />
<img src="https://user-images.githubusercontent.com/13380182/153308417-21417cb3-8761-4ccc-a8c6-7777a10c9b6b.png" />
<img src="https://user-images.githubusercontent.com/13380182/152663687-af054d21-8451-4226-82dd-974491b53a4e.png" />
<img src="https://user-images.githubusercontent.com/13380182/152663689-1c74d0b7-73a4-4bfd-854d-05e84a756cc6.png" />
<img src="https://user-images.githubusercontent.com/13380182/152663690-2dd6b1bf-9b29-42f0-a12f-146a083e3a1a.png" />
<img src="https://user-images.githubusercontent.com/13380182/152663691-99c457c3-8524-4d05-8e38-ab3658604f64.png" />
<img src="https://user-images.githubusercontent.com/13380182/152663693-ada6d47e-6392-43dd-babf-c1e819cd6840.png" />
<img src="https://user-images.githubusercontent.com/13380182/152663694-90219591-7a01-44a7-b57c-5e1cbe7f235e.png" />
<img src="https://user-images.githubusercontent.com/13380182/152663695-5f9a53e5-a18b-46f4-8c0d-6b1ecee677a0.png" />
<img src="https://user-images.githubusercontent.com/13380182/152663697-55c966a9-94a2-4eaf-a270-05610bbf4371.png" />
<img src="https://user-images.githubusercontent.com/13380182/152663698-fd148901-e492-4436-986e-d958e13996ad.png" />
<img src="https://user-images.githubusercontent.com/13380182/153310288-14707aac-a03e-487a-b0a8-2f0304e1425b.png" />
<img src="https://user-images.githubusercontent.com/13380182/152663700-ca3cc38c-74fd-4ebb-b736-66af21757123.png" />
<img src="https://user-images.githubusercontent.com/13380182/152663701-80a05b53-a8ad-42c2-a1e1-a20073ea28b5.png" />
<img src="https://user-images.githubusercontent.com/13380182/152663702-3a709ee4-9aee-4b2b-b861-8dc169df5d83.png" />
<img src="https://user-images.githubusercontent.com/13380182/152663703-983aaba6-44fe-42b2-b1d0-1c10a60c8a41.png" />
<img src="https://user-images.githubusercontent.com/13380182/153309982-c2445505-2667-483c-9f1c-cb97679295cf.png" />

--------------------------------------------------------------------------------------------------------------

#### Tools and Technologies

|                      	|                                             	|
|----------------------	|---------------------------------------------	|
| Java                 	| 1.8                                         	|
| Rich Client Platform 	| JavaFX 8                                    	|
| Runtime Frameworks   	| OSGi (Equinox), Eclipse 4 (e4), e(fx)clipse 	|
| UI Libraries         	| ControlsFX, TilesFX, FormsFX                  |
| Tools                	| Bndtools 6                                  	|

#### Important Notes

* The distribution packages include JRE for respective platforms
* After application is installed, you will need to change the permission of the application to **777** or change the ownership, otherwise, the OSGi framework will not be able to create the required directories inside it

--------------------------------------------------------------------------------------------------------------

#### Minimum Requirements for Runtime Agent

1. Java 8
2. OSGi R6

To use the agent in the OSGi environment, you need to install `in.bytehue.osgifx.console.agent.jar` and set `osgi.fx.agent.port` system property in the runtime. Note that, you can either set the property to any port e.g. `2000` or `0.0.0.0:2000`. The latter one will allow remote connections whereas the former one will only allow connections from `localhost`.

--------------------------------------------------------------------------------------------------------------

#### Project Import for Development

1. Install `Bndtools` from Eclipse Marketplace
2. Import all the projects (`File -> Import -> General -> Existing Projects into Workspace` and select `Search for nested projects`)

--------------------------------------------------------------------------------------------------------------

#### Building from Source

Run `./gradlew clean build` in the project root directory

--------------------------------------------------------------------------------------------------------------

#### External Feature Development

External plugins or features can easily be developed for `OSGi.fx`. Please have a look at how the bundles with `com.osgifx.console.ui.*` project name pattern are developed. As a starting point, please have a look at the sample [Tic-Tac-Toe feature](https://github.com/amitjoy/osgifx/tree/main/com.osgifx.console.feature.tictactoe). Since `OSGi.fx` has itself been developed using **OSGi** and **Eclipse e4**, you can easily use their modular functionalities to build your own features.

Once the feature is built, you can test it by installing it from the `Help -> Install External Feature` menu option.

Note that, to develop an external feature, you need to provide a ZIP archive comprising one or more feature JSON files. Have a look at [OSGi Features Specification](http://docs.osgi.org/specification/osgi.cmpn/8.0.0/service.feature.html) on how to prepare the JSON files. For every feature, you need to provide the bundles (JARs) in the `bundles` directory inside the archive.

For ease of development, you can use the OSGi.fx workspace to further develop your own features as the workspace comprises a new bnd plugin which will enable you to automatically prepare the ZIP archive from a bndrun file. As an example, please refer to the sample [Tic-Tac-Toe feature](https://github.com/amitjoy/osgifx/tree/main/com.osgifx.console.ext.feature.tictactoe).

--------------------------------------------------------------------------------------------------------------

#### Developer

[Amit Kumar Mondal](https://github.com/amitjoy) (admin@amitinside.com)

--------------------------------------------------------------------------------------------------------------

#### Contribution

[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/amitjoy/osgifx-console/issues)

--------------------------------------------------------------------------------------------------------------

#### License

[![License](http://img.shields.io/badge/license-Apache-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)