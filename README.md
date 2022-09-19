# Runtime

Assimbly runtime runs

   * Integrations (Connectors, Flows, Routes)
   * Message brokers 

The integration modules are build on top of [Apache Camel](https://github.com/apache/camel) and the broker modules are build on top of [Apache ActiveMQ](https://github.com/apache/activemq).

## API

Each module in the runtime contains an API. The integration and broker module contain a Java API and the integrationRest and brokerRest contain a REST API.
The API's are used by Assimbly Gateway, but can also be used in your own program.


# Developing

The project is build with maven:

```mvn clean install```

## prerequisites

- JDK11+
- Maven
- [Assimbly Base](https://github.com/assimbly/base)

## build

The base can also be build with Maven:

```mvn clean install```

It's also possible to build only one module at the time.
For this the same Maven command can be executed, but then
from the directory that contains pom.xml of that module.

For example:

```
cd ./integration
mvn clean install
```

# Usage

After building you can call the Java API from your Java application like this:

```java
Integration integration = new CamelIntegration();
integration.start();

integration.setFlowConfiguration(flowId, mediatype, flowConfiguration);

integration.startFlow(flowID);
```

## configuration

An integration flow is configured with key-values. 

The key-values are stored in a [Java Treemap](https://beginnersbook.com/2013/12/treemap-in-java-with-example/)
Multiple flows in a connector are configured with a list of Treemaps. 

The easiest way to generate the Treemap is to convert it from a DIL (Data Integration Language) file. XML, JSON and YAML are supported. 
Another possibility is using the GUI of [Assimbly Gateway](https://github.com/assimbly/gateway). 

## example

The following XML configuration prints 'Hello World!'.

```java

Integration integration = new CamelIntegration("example", "file://C:/conf/helloworld.xml");

integration.start();
integration.startFlow("HelloWorld");

```

### DIL

Assimbly uses the data integration language to create the integrations.

helloworld.xml
```xml
<flow>
    <name>HelloWorld</name>
    <steps>
        <step>
            <type>source</type>
            <uri>timer:foo</uri>
        </step>
        <step>
            <type>sink</type>
            <uri>print:Hello World!</uri>
        </step>
    </steps>
</flow>
```

For a longer [XML example](https://github.com/assimbly/connector/wiki/XML-Configuration-Example) see the wiki.

## management

The API simplifies common management tasks. The following lifecycle management actions are supported:

* start
* stop
* restart
* pause
* resume

## support

In case of questions or issues, you can create a Github issue.

