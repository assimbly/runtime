# Connector

This API is meant to configure and manage Assimbly flows. For example by sending data from a database to
a message broker.

   * Connector: A collection of flows
   * Flow: connects endpoints
   * Endpoint: A source or destination
   
Currently the API is build on top of [Apache Camel](https://github.com/apache/camel).

## Configuration

A flow is configured with key-values. The key-values are stored in a [Java Treemap](https://beginnersbook.com/2013/12/treemap-in-java-with-example/)
Multiple flows in a connector can be configure with a list of Treemaps. 

The easiest way to generate the Treemap is to convert it from an file (XML, JSON and YAML are supported). Another possibility is using the
GUI of [Assimbly Gateway](https://github.com/assimbly/gateway). 

## Management

The API simplifies common management tasks. The following lifecycle management actions are supported:

* start
* stop
* restart
* pause
* resume


## Development

The project is build with maven (mvn install). After building you can call the API from your Java application like this: 

```java
Connector connector = new CamelConnector(flowID, configurationUri);

connector.start();
connector.startFlow(flowID);
```

or

```java
Connector connector = new CamelConnector();
connector.start();

connector.setFlowConfiguration(flowId, mediatype, flowConfiguration);
connector.startFlow(flowID);
```

## Example

The following XML configuration moves files from a one directory to another.

```java

Connector connector = new CamelConnector("example", "file://C:/conf/conf.xml");

connector.start();
connector.startFlow("filetofile");

```

conf.xml
```xml

<?xml version="1.0" encoding="UTF-8"?>
<connectors>
   <connector>
      <id>1</id>
      <name>default</name>
      <type>ADAPTER</type>
      <environmentName>Dev1</environmentName>
      <stage>DEVELOPMENT</stage>
      <defaultFromEndpointType>FILE</defaultFromEndpointType>
      <defaultToEndpointType>FILE</defaultToEndpointType>
      <defaultErrorEndpointType>FILE</defaultErrorEndpointType>
      <offloading/>
      <flows>
         <flow>
            <id>2</id>
            <name>FILE2FILE</name>
            <autostart>false</autostart>
            <offloading>false</offloading>
            <maximumRedeliveries>0</maximumRedeliveries>
            <redeliveryDelay>3000</redeliveryDelay>
            <logLevel>OFF</logLevel>
            <from>
               <id>2</id>
               <uri>file://C:\test1</uri>
            </from>
            <to>
               <id>2</id>
               <uri>file://C:\test2</uri>
               <options>
                  <directoryMustExist>true</directoryMustExist>
               </options>
            </to>
            <error>
               <id>2</id>
               <uri>file://C:\test3</uri>
            </error>
         </flow>
      <services/>
      <headers/>
      <environmentVariables/>
   </connector>
</connectors>

```

For a longer [XML example](https://github.com/assimbly/connector/wiki/XML-Configuration-Example) see the wiki. 