# Connector
This API is meant to configure and manage point-to-point connections. Currently this build on top of Apache Camel. 

## Development

You can build this project with maven (mvn install). 

```java

Connector connector = new CamelConnector();

connector.start();
connector.addConfiguration(connectorID, configurationUri) );
connector.startRoute(routeID);

```

For example we have following xml configuration to replace file from a one directory to another.

```xml

<?xml version="1.0" encoding="UTF-8"?>
<configuration>
	<id>example</id>
	<type>file_gateway</type>
	<connectors>
		
		<!-- example file to file --> 		
		<connector>
			<id>example.filetofile</id>
			<from>
				<uri>file://C:/Test1</uri>
			</from>
			<to>
				<uri>file://C:/Test2</uri>
			</to>
		</connector>
		
</configuration>

```

```java

Connector connector = new CamelConnector();

connector.start();
connector.addConfiguration("example", "file://C:/conf/conf.xml") );
connector.startRoute("example.filetofile");

```