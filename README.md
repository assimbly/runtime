# Connector
This API is meant to configure and manage point-to-point connections (alpha status).

Currently the API is build on top of Apache Camel. 

## Configuration

Configuration is done with a Java Treemap. Easiest way is to generate the Treemap from an XML file. Currently supported
Camel components are File, Stream, JDBC, SJMS, SFTP, HTTP4, ACTIVEMQ and SONICMQ. 

## Management

The following lifecycle management actions are supported:

* start
* stop
* restart
* pause
* resume

The API simplifies common management tasks. You can however also get the Camel Context to access the full API of Camel.

## Development

The project is build with maven (mvn install). After building you can call this from your java application like this: 

```java

Connector connector = new CamelConnector();

connector.start();
connector.addConfiguration(connectorID, configurationUri) );
connector.startRoute(routeID);

```

For example the following XML configuration moves files from a one directory to another.

```xml

<?xml version="1.0" encoding="UTF-8"?>
<configuration>
	<id>example</id>
	<type>file_gateway</type>
	<connectors>
		
		<!-- example file to file --> 		
		<connector>
			<id>filetofile</id>
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

## Longer XML configuration example

```xml

<?xml version="1.0" encoding="UTF-8"?>
<configuration>
	<id>example</id>
	<type>file_gateway</type>
	<connectors>

		<!-- FILE Producer Examples -->

		<!-- example file to wastebin -->
		<connector>
			<id>exampe.filetowastebin</id>
			<from>
				<uri>file://C:/Test1</uri>
			</from>
			<to>
				<uri>wastebin</uri>
			</to>	
		</connector>
		
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

		<!-- example file to streamm (prints message) --> 		
		<connector>
			<id>example.filetostream</id>
			<from>
				<uri>file://C:/Test1</uri>
			</from>
			<to>
				<uri>stream:out</uri>
			</to>
		</connector>

		<!-- example file to sonicmq --> 		
		<connector>
			<id>example.filetosonicmq</id>
			<from>
				<uri>file://C:/Test1</uri>
			</from>
			<to>
				<uri>sonicmq:Sample.Q1</uri>
				<options>
					<jmsMessageType>text</jmsMessageType>
				</options>
				<connection_id>local.mgmt</connection_id>
			</to>
			<error>
				<uri>file://C:/Test2</uri>
			</error>
		</connector>

		<!-- example file to sftp  -->
		<connector>
			<id>example.filetosftp</id>
			<from>
				<uri>file://C:/Test1</uri>
			</from>			
			<to>
				<uri>sftp://username@server/directory</uri>
				<options>
					<password>S0n1c2015!</password>
				</options>
			</to>	
		</connector>

		<!-- example file to sql  -->
		<connector>
			<id>example.filetosql</id>
			<from>
				<uri>file://C:/Test1</uri>
			</from>			
			<to>
				<uri>sql:insert into history (MESSAGE,`DATE`,MESSAGE_ID,TYPE) values (:#message,:#date,:#message_id,:#type)</uri>
				<options>
					<dataSource>test.db</dataSource>					
				</options>
				<connection_id>test.db</connection_id>
				<header_id>mapper</header_id>
			</to>	
		</connector>
					
	</connectors>
	
	<!-- connections with for example MQ or Databases -->
	<connections>
		<connection>
			<id>local.mgmt</id>
			<username>Administrator</username>
			<password>Administrator</password>
			<url>tcp://localhost:2506</url>
		</connection>
		
		<connection>
			<id>test.db</id>
			<username>username</username>
			<password>example</password>
			<url>jdbc:mysql://localhost/dbname</url>
			<driver>com.mysql.jdbc.Driver</driver>
		</connection>		
	</connections>
	
	<!-- headers -->
	<headers>
		<header>
			<id>mapper</id>
			<x type="constant">y</x>
			<message type="xpath">/root/message/text()</message>
			<date type="xpath">/root/date/text()</date>
			<message_id type="xpath">/root/message_id/text()</message_id>
			<type type="xpath">/root/type/text()</type>
		</header>
	</headers>
		
</configuration>
```
