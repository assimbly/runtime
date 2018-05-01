# Connector

This API is meant to configure and manage (point-to-points) flows. For example by sending data from a database to
a message broker.

   * Connector: A collection of flows
   * Flow: connects endpoints
   * Endpoint: A source or destination
   
Currently the API is build on top of [Apache Camel](https://github.com/apache/camel). Supported
Camel components are File, Stream, JDBC, SJMS, SFTP, HTTP4, ACTIVEMQ and SONICMQ.

## Configuration

A flow is configured with key-values. The key-values are stored in a [Java Treemap](https://beginnersbook.com/2013/12/treemap-in-java-with-example/)
Multiple flows in a connector can be configure with a list of Treemaps. 

The easiest way to generate the Treemap is to convert it from an file (XML,JSON or YAML are supported). Another possibility is using the
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
<connector>
	<id>example</id>
	<flows>
		
		<!-- example file to file --> 		
		<flow>
			<id>filetofile</id>
			<from>
				<uri>file://C:/Test1</uri>
			</from>
			<to>
				<uri>file://C:/Test2</uri>
			</to>
		</flow>
	</flows>		
</connector>

```

For a longer [XML example](https://github.com/assimbly/connector/wiki/XML-Configuration-Example) see the wiki. 