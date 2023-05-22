# How to Contribute

First of all, thanks for considering to contribute. We appreciate the time and effort you want to
spend helping to improve things around here. And help we can use :-)

Here is a (non-exclusive, non-prioritized) list of things you might be able to help us with:

* bug reports
* bug fixes
* improvements regarding code quality, e.g. improving readability, performance, modularity etc.
* documentation
* features (both ideas and code are welcome)
* tests

### File Headers

A proper header must be in place for any file contributed to the project. For a new contribution, please add the below header:

```
/*
 * Copyright (c) <year> <legal entity> and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 ```

 Please ensure `<year>` is replaced with the current year or range (e.g. 2017 or 2015, 2017).
 Please ensure `<legal entity>` is replaced with the relevant information. If you are editing an existing contribution, feel free
 to create or add your legal entity to the contributors section as such:

 ```
/*
 * Copyright (c) <year> <legal entity> and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     <legal entity>
 */
 ```

### How to Contribute
The easiest way to contribute code/patches/whatever is by creating a GitHub pull request (PR). When you do make sure that you *Sign-off* your commits.

You do this by adding the `-s` flag when you make the commit(s), e.g.

    $> git commit -s -m "Let's do it."

## Making your Changes

* Fork the repository on GitHub
* Create a new branch for your changes
* Configure your IDE
* Make your changes
* Make sure copyright headers are included in (all) files including updated year(s)
* Make sure proper headers are in place for each file (see above Legal Requirements)
* Commit your changes to that branch
* Use descriptive and meaningful commit messages
* If you have a lot of commits squash them into a single commit
* Make sure you use the `-s` flag when committing as explained above
* Push your changes to your branch in your forked repository

## Importing projects in Eclipse IDE

* Install `Java 17` JDK Distribution (including JavaFX modules) in your machine (Currently I am using [Zulu Distribution](https://www.azul.com/downloads/?version=java-17-lts&package=jdk-fx#zulu) that includes JavaFX modules already)
* Set the newly installed `Java 17` to your `JAVA_HOME`
* Install `Eclipse IDE for RCP/RAP Developers` and make sure your Eclipse installation uses the currently installed `Java 17` to start with
* Install `Bndtools`, `e(fx)clipse`, and `SonarLint` from Eclipse Marketplace
* Go to `Java ⇢ Compiler` in `Eclipse Preferences` and set `Compiler Compliance Level` to `17`
* Go to `Java ⇢ Installed JREs` and select the newly installed JDK with JavaFX modules
* Go to `Java ⇢ Installed JREs ⇢ Execution Environments` and select `JavaSE-17` and choose the recently installed `Java 17 JRE` that includes `JavaFX` modules
* Go to `Java ⇢ Code Style ⇢ Formatter ⇢ Import` and select the `formatter.xml` stored in the project's root directory
* Import all the projects (`File ⇢ Import ⇢ General ⇢ Existing Projects into Workspace` and select `Search for nested projects`)

## Verify your changes

* To start the application inside IDE, go to `com.osgifx.console.product` and double click on `osgifx.bndrun`. This will open the `bndrun editor` and then click on `Debug OSGi` from the top right corner.

## Submitting the Changes

Submit a pull request via the normal GitHub UI

## After Submitting

* Do not use your branch for any other development, otherwise further changes that you make will be visible in the PR.
