# Runtime

Assimbly runtime runs

   * Integrations (Connectors, Flows) Routes)
   * Message broker 
   
Each module in the runtime contains an API. The integration and broker module contain a Java API and the integrationRest and brokerRest contain a REST API.

The integration modules are build on top of [Apache Camel](https://github.com/apache/camel) and the broker modules are build on top of [Apache ActiveMQ](https://github.com/apache/activemq).



## Configuration

An integration flow is configured with key-values. 

The key-values are stored in a [Java Treemap](https://beginnersbook.com/2013/12/treemap-in-java-with-example/)
Multiple flows in a connector can be configure with a list of Treemaps. 

The easiest way to generate the Treemap is to convert it from a configuration file (XML, JSON and YAML are supported). Another possibility is using the
GUI of [Assimbly Gateway](https://github.com/assimbly/gateway). 

## Management

The API simplifies common management tasks. The following lifecycle management actions are supported:

* start
* stop
* restart
* pause
* resume

# Developing

The project is build with maven (mvn clean install).

# prerequisite

- JDK11+
- Maven
- [Assimbly Base](https://github.com/assimbly/base)

# build

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

After building you can call the API from your Java application like this: 

```java
Integration integration = new CamelIntegration();
integration.start();

integration.setFlowConfiguration(flowId, mediatype, flowConfiguration);

integration.startFlow(flowID);
```

## Example

The following XML configuration moves files from a one directory to another.

```java

Integration integration = new CamelIntegration("example", "file://C:/conf/conf.xml");

integration.start();
integration.startFlow("filetofile");

```

conf.xml
```xml

<?xml version="1.0" encoding="UTF-8"?>
<integrations>
    <integration>
        <id>live</id>
        <flows>
            <flow>
                <id>1</id>
                <name>TestFlow</name>
                <type>connector</type>
                <version/>
                <autostart/>
                <assimblyHeaders>true</assimblyHeaders>
                <parallelProcessing/>
                <maximumRedeliveries>0</maximumRedeliveries>
                <redeliveryDelay>3000</redeliveryDelay>
                <logLevel>OFF</logLevel>
                <notes/>
                <components>
                    <component>file</component>
                </components>
                <steps>
                    <step>
                        <id>3</id>
                        <type>error</type>
                        <uri>file://C:\messages\error</uri>
                    </step>
                    <step>
                        <id>1</id>
                        <type>from</type>
                        <uri>file://C:\messages\in</uri>
                    </step>
                    <step>
                        <id>2</id>
                        <type>to</type>
                        <uri>file://C:\messages\out</uri>
                    </step>
                </steps>
            </flow>
        </flows>
        <routes/>
        <routeConfigurations/>
        <connections/>
        <headers/>
    </integration>
</integrations>

```

For a longer [XML example](https://github.com/assimbly/connector/wiki/XML-Configuration-Example) see the wiki. 


