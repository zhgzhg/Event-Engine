Event Engine
============
<img alt="Event Engine Logo" src="https://raw.githubusercontent.com/zhgzhg/Event-Engine/master/logo.svg?sanitize=true" height="160" width="160" />

An event engine developed for the Virtual Referent Space (VRS).
The project is licensed under Apache License 2.0. You may find the terms in file named "LICENSE" in this directory.

![build status badge](https://travis-ci.com/zhgzhg/Event-Engine.svg?branch=master "Build Status")
[ ![Download](https://api.bintray.com/packages/zhgzhg/Event-Engine/Event-Engine/images/download.svg "Download Event Engine") ](https://bintray.com/zhgzhg/Event-Engine/Event-Engine/0.2.5)


What's Provided
---------------

The project implements several modules with the following purpose:

* Events - structural representation of complex events. Provides initialisation and computation mechanisms for them.
* Annotations - used to define new event structures and validate them during compilation if annotation processing is enabled. The module is required by the 'Events' one.
* Serializers - serialisation and encoding mechanisms for events (like for e.g. JSON ;; BASE32 ;; etc.)
* Dispatchers - uniform, high-level implementation for distribution of events based on broker systems like ActiveMQ or Kafka.


Requirements
------------

* Java 8+
* Maven 3.3.9+ or IntelliJ IDEA 2018.1+


How to Compile
--------------

### For Java 8:
* With IntelliJ IDEA: 
    * Open the project and build it using the GUI menu.
* With Maven:
    * Execute `mvn clean install -P java8`

### For Java 9+ - beta, limited modularization:
* With IntelliJ IDEA:
    * Open the project.
    * In project's settings specify Project JDK to be JDK9 or later
    * For every module copy the file module-info.java inside its java directory.

* With Maven:
    * Execute `mvn clean install -P java9p`
 
How to Use
----------
 
At this point the examples are extremely limited, so please refer to the java documentation and the unit tests.
You can also examine the [test examples](https://github.com/zhgzhg/Event-Engine-JADE/tree/master/src/test/java/test/pseudo "pseudo test client agent")
of [Event Engine for JADE](https://github.com/zhgzhg/Event-Engine-JADE "Event Engine for JADE") project.
 
 
Known Issues
------------

The combined jar files of all project's modules are not recommended for production use. Work with the individually
generated jars instead!
 
Pay attention to IntelliJ IDEA settings if you are using Maven to build the project. Especially when changing JDK
version during imports and runs. The environment variable JAVA_HOME might be mandatory to set if you have several
JDK versions, in order to guarantee that maven javadoc plugin is using the correct JRE (see below).

Event Engine relies on not modularized yet dependencies. Migrating to Java 9+ may cause problems.
Compilation for Java 9+ with Maven will succeed, but the javadoc generation might fail.
