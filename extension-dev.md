---
layout: default
title: Extension Development
permalink: /extension-dev
---

# Extension Development

External plugins or extensions can easily be developed for **OSGi.fx**. Since `OSGi.fx` has itself been developed using **OSGi** and **Eclipse e4**, you can easily leverage their modular capabilities to build your own extensions.

## Getting Started

Please have a look at how the bundles with `com.osgifx.console.ui.*` project name pattern are developed. 

> [!TIP]
> As a starting point, refer to the sample [Tic-Tac-Toe Extension](https://github.com/amitjoy/osgifx/tree/main/com.osgifx.console.extension.ui.tictactoe).

## Installation

Once the extension is developed, you can test it by installing it from the `Actions -> Install Extension` menu option.

## Deployment Packages

To develop an extension, you need to provide an **OSGi Deployment Package** archive. 
- Have a look at [OSGi Deployment Admin Specification](http://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.deploymentadmin.html) on how to prepare such deployment packages.

## Developer Workspace

For ease of development, you can use the **OSGi.fx workspace** to further develop your own extensions. The workspace comprises a new bnd plugin which will enable you to automatically generate a deployment package from a `bndrun` file.
