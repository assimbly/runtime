# Connector

This API is meant to configure and manage (point-to-point) flows (beta). For example by sending data from a database to
a message broker.

   * Connector: A collection of flows
   * Flow: connects endpoints
   * Endpoint: A source or destination
   
Currently the API is build on top of [Apache Camel](https://github.com/apache/camel). Supported
Camel components are File, Stream, JDBC, SJMS, SFTP, HTTP4, ACTIVEMQ and SONICMQ. 

## Configuration

A flow is configured with key-values. The key-values are stored in a [Java Treemap](https://beginnersbook.com/2013/12/treemap-in-java-with-example/)
Multiple flows in a connector can be configure with a list of Treemaps. 

The easiest way to generate the Treemap is to convert it from an XML file. Another possibility is using the
GUI of Assimbly gateway. See [Apache Camel](https://github.com/assimbly/gateway). 

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

Connector connector = new CamelConnector();

connector.start();
Treemap<String,String> flowConfiguration = connector.convertXMLToFlowConfiguration(flowID, configurationUri);
setFlowConfiguration(flowConfiguration);
connector.startFlow(flowID);

or 

Connector connector = new CamelConnector(flowID, configurationUri);

connector.start();
connector.startFlow(flowID);

```

## Example

The following XML configuration moves files from a one directory to another.

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

```java

Connector connector = new CamelConnector("example", "file://C:/conf/conf.xml");

connector.start();
connector.startFlow("example.filetofile");

```

## Longer XML configuration example

```xml
<?xml version="1.0" encoding="UTF-8"?>
<connector>
	<id>example</id>
	<flows>

		<!-- FILE Producer Examples -->

		<!-- example file to wastebin -->
		<flow>
			<id>exampe.filetowastebin</id>
			<from>
				<uri>file://C:/Test1</uri>
			</from>
			<to>
				<uri>wastebin</uri>
			</to>	
		</flow>
		
		<!-- example file to file --> 		
		<flow>
			<id>example.filetofile</id>
			<from>
				<uri>file://C:/Test1</uri>
			</from>
			<to>
				<uri>file://C:/Test2</uri>
			</to>
		</flow>

		<!-- example file to streamm (prints message) --> 		
		<flow>
			<id>example.filetostream</id>
			<from>
				<uri>file://C:/Test1</uri>
			</from>
			<to>
				<uri>stream:out</uri>
			</to>
		</flow>

		<!-- example file to sonicmq --> 		
		<flow>
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
		</flow>

		<!-- example file to sftp  -->
		<flow>
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
		</flow>

		<!-- example file to sql  -->
		<flow>
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
		</flow>
					
	</flows>
	
	<!-- connections with for example MQ or Databases -->
	<services>
		<service>
			<id>local.mgmt</id>
			<username>Administrator</username>
			<password>Administrator</password>
			<url>tcp://localhost:2506</url>
		</service>
		
		<service>
			<id>test.db</id>
			<username>username</username>
			<password>example</password>
			<url>jdbc:mysql://localhost/dbname</url>
			<driver>com.mysql.jdbc.Driver</driver>
		</service>		
	</services>
	
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
		
</connector>
```
