# Connector

This API is meant to configure and manage point-to-point connections (beta). For example by sending data from a database to
a message broker.

   * Connector: A collections of point-to-point connections
   * Route: One point-point connection
   * Endpoint: A source or destination
   
Currently the API is build on top of [Apache Camel](https://github.com/apache/camel). Supported
Camel components are File, Stream, JDBC, SJMS, SFTP, HTTP4, ACTIVEMQ and SONICMQ. 

## Configuration

Configuration of a route is done with a Java Treemap. Easiest way is to generate the Treemap from an XML file. Configuration
can also be done with a GUI, see [Apache Camel](https://github.com/assimbly/gateway). 

## Management

The API simplifies common management tasks. You can however also get the Camel Context to access the full API of Camel.

The following lifecycle management actions are supported:

* start
* stop
* restart
* pause
* resume


## Development

The project is build with maven (mvn install). After building you can call the API from your Java application like this: 

```java

Connector connector = new CamelConnector();

connector.start();
Treemap<String,String> routeconfiguration = connector.convertXMLToRouteConfiguration(routeID, configurationUri) );
setRouteConfiguration(routeconfiguration);
connector.startRoute(routeID);

or 

Connector connector = new CamelConnector(routeID, configurationUri);

connector.start();
connector.startRoute(routeID);

```

## Example

The following XML configuration moves files from a one directory to another.

```xml

<?xml version="1.0" encoding="UTF-8"?>
<connector>
	<id>example</id>
	<routes>
		
		<!-- example file to file --> 		
		<route>
			<id>filetofile</id>
			<from>
				<uri>file://C:/Test1</uri>
			</from>
			<to>
				<uri>file://C:/Test2</uri>
			</to>
		</route>
	</routes>		
</connector>

```

```java

Connector connector = new CamelConnector("example", "file://C:/conf/conf.xml");

connector.start();
connector.startRoute("example.filetofile");

```

## Longer XML configuration example

```xml
<?xml version="1.0" encoding="UTF-8"?>
<route>
	<id>example</id>
	<routes>

		<!-- FILE Producer Examples -->

		<!-- example file to wastebin -->
		<route>
			<id>exampe.filetowastebin</id>
			<from>
				<uri>file://C:/Test1</uri>
			</from>
			<to>
				<uri>wastebin</uri>
			</to>	
		</route>
		
		<!-- example file to file --> 		
		<route>
			<id>example.filetofile</id>
			<from>
				<uri>file://C:/Test1</uri>
			</from>
			<to>
				<uri>file://C:/Test2</uri>
			</to>
		</route>

		<!-- example file to streamm (prints message) --> 		
		<route>
			<id>example.filetostream</id>
			<from>
				<uri>file://C:/Test1</uri>
			</from>
			<to>
				<uri>stream:out</uri>
			</to>
		</route>

		<!-- example file to sonicmq --> 		
		<route>
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
		</route>

		<!-- example file to sftp  -->
		<route>
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
		</route>

		<!-- example file to sql  -->
		<route>
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
		</route>
					
	</routes>
	
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
		
</route>
```
