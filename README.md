# Modules

These API's are meant to configure and manage Assimbly module. For example
a message broker.

   * Connector: connect endpoints
   * Broker: message broker
   
Currently the API's are build on top of [Apache Camel](https://github.com/apache/camel) and [Apache ActiveMQ](https://github.com/apache/activemq).

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
            <endpoint>
               <id>2</id>
               <type>from</type>
               <uri>file://C:\test1</uri>
            </endpoint>
            <endpoint>
               <id>2</id>
               <type>to</type>
               <uri>file://C:\test2</uri>
               <options>
                  <directoryMustExist>true</directoryMustExist>
               </options>
            </endpoint>
            <endpoint>
               <id>2</id>
               <type>error</type>
               <uri>file://C:\test3</uri>
            </endpoint>
         </flow>
      <services/>
      <headers/>
      <environmentVariables/>
   </connector>
</connectors>

```

For a longer [XML example](https://github.com/assimbly/connector/wiki/XML-Configuration-Example) see the wiki. 