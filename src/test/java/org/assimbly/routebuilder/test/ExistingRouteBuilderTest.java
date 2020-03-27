package org.assimbly.routebuilder.test;

public class ExistingRouteBuilderTest {
	
}
/*
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.assimbly.connector.routes.DefaultRoute;


public class ExistingRouteBuilderTest extends CamelTestSupport {

    public void setUp() throws Exception {
        // delete directories so we have a clean start
        deleteDirectory("target/inbox");
        deleteDirectory("target/outbox");
        super.setUp();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        // use the existing FileMoveRoute as the route builder
        // this class is in the src/main/java/camelinaction directory
        return new DefaultRoute();
    }

}
*/