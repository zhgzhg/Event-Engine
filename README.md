Event Engine
============

![build status badge](https://travis-ci.org/zhgzhg/Event-Engine.svg?branch=master "Build Status") [ ![Download](https://api.bintray.com/packages/zhgzhg/Event-Engine/Event-Engine/images/download.svg "Download Latest Version") ](https://bintray.com/zhgzhg/Event-Engine/Event-Engine/_latestVersion) 

An event engine developed for the Virtual Referent Space (VRS).
The project is licensed under Apache License 2.0. You may find the terms in file named "LICENSE" in this directory.


Requirements
------------

* Java 8+
* Maven 3.3.9+ or IntelliJ IDEA 2017.3.1+


How to Compile
--------------

### For Java 8:
* With IntelliJ IDEA: 
    * Open the project and build it using the gui options.
* With Maven:
    * Execute `mvn clean install -P java8`

### For Java 9+ - alpha, extremely limited modularization, not recommended:
* With IntelliJ IDEA:
    * Open the project.
    * In project's settings specify Project JDK to be JDK9 
    * For every module copy the file module-info.java inside its java directory.

* With Maven:
    * Execute `mvn clean install -P java9p`
    
Known Issues
------------

The combined jar files of all project's modules are not recommended for production use. Work with the individually
generated jars instead!
 
Pay attention to IntelliJ IDEA settings if execution of Maven through it is desired. Especially when changing JDK
version during imports and runs. Sometimes JAVA_HOME might be mandatory to set.

Event Engine relies on not modularized yet dependencies. Migrating to Java 9+ may cause problems.
Compilation for Java 9 with maven will succeed except for the javadoc generation.