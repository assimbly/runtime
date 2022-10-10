# DIL

DIL stands for Data Integration Language. It's a language to develop
integrations in an easy and flexible way.


## Key features

- Validate DIL files
- Transpile DIL code into [Apache Camel](https://github.com/apache/camel) code.
- Run DIL flows and Camel routes
- Contains predefined DIL steps


# Developing

The project is build with maven (mvn clean install).

# prerequisite

- JDK11+
- Maven
- [Assimbly Base](https://github.com/assimbly/base)

# build

The base can also be build with Maven:

```mvn clean install```


## Example

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


