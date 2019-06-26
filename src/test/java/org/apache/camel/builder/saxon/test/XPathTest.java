package org.apache.camel.builder.saxon.test;

import javax.xml.xpath.XPathFactory;

import net.sf.saxon.xpath.XPathFactoryImpl;
//import org.apache.camel.language.xpath.XPathBuilder;	See: https://github.com/apache/camel/blob/master/components/camel-saxon/src/test/java/org/apache/camel/builder/saxon/XPathTest.java
import org.apache.camel.builder.xml.XPathBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Ignore;
import org.junit.Test;

public class XPathTest extends CamelTestSupport {
	
	static String xmlStr01 = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><foo><bar id=\"1\">cheese</bar><soapenv:Body><ns5:voorlopigeVorderingVastgelegd xmlns=\"http://ns.xxxx.nl/msg/debiteur/v1\" xmlns:ns2=\"http://ns.xxxx.nl/msg/transactie/v1\" xmlns:ns3=\"http://ns.xxxx.nl/msg/financieelproduct/v1\" xmlns:ns4=\"http://ns.xxxx.nl/msg/bankrekening/v1\" xmlns:ns5=\"http://ns.xxxx.nl/events/ext/transactie/v1\"><klantnummerExternePartij><klantnummer>99342999</klantnummer></klantnummerExternePartij><ns2:transactie><ns2:actor>TRC</ns2:actor><ns2:label>Company YYYYY</ns2:label><ns2:debiteurnummer>1299342999</ns2:debiteurnummer></ns2:transactie></ns5:voorlopigeVorderingVastgelegd></soapenv:Body></foo></soapenv:Envelope>";
	static String xmlStr02 = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><foo><bar id=\"1\">abc_def_ghi</bar><soapenv:Body><ns5:voorlopigeVorderingVastgelegd xmlns=\"http://ns.xxxx.nl/msg/debiteur/v1\" xmlns:ns2=\"http://ns.xxxx.nl/msg/transactie/v1\" xmlns:ns3=\"http://ns.xxxx.nl/msg/financieelproduct/v1\" xmlns:ns4=\"http://ns.xxxx.nl/msg/bankrekening/v1\" xmlns:ns5=\"http://ns.xxxx.nl/events/ext/transactie/v1\"><klantnummerExternePartij><klantnummer>99342999</klantnummer></klantnummerExternePartij><ns2:transactie><ns2:actor>TRC</ns2:actor><ns2:label>Company YYYYY</ns2:label><ns2:debiteurnummer>1299342999</ns2:debiteurnummer></ns2:transactie></ns5:voorlopigeVorderingVastgelegd></soapenv:Body></foo></soapenv:Envelope>";

	static String xmlFullStr01 = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><soapenv:Header><metadata xmlns=\"http://ns.xxxx.nl/msg/header/v2\"><timestamp>2014-01-09T13:33:55.804Z</timestamp><application>TRC</application><service>externalevent</service><label>Company YYYYY 01</label><debiteurnummer>1299342999</debiteurnummer><operatie>voorlopigeVorderingVastgelegd</operatie><messageId>00f790f8-b812-407e-b178-9028bb02f05c</messageId><correlationId>00f790f8-b812-407e-b178-9028bb02f05c</correlationId><userId>TRC</userId><environment>ONT</environment></metadata></soapenv:Header><soapenv:Body><ns5:voorlopigeVorderingVastgelegd xmlns=\"http://ns.xxxx.nl/msg/debiteur/v1\" xmlns:ns2=\"http://ns.xxxx.nl/msg/transactie/v1\" xmlns:ns3=\"http://ns.xxxx.nl/msg/financieelproduct/v1\" xmlns:ns4=\"http://ns.xxxx.nl/msg/bankrekening/v1\" xmlns:ns5=\"http://ns.xxxx.nl/events/ext/transactie/v1\"><klantnummerExternePartij><klantnummer>99342999</klantnummer></klantnummerExternePartij><ns2:transactie><ns2:actor>TRC</ns2:actor><ns2:label>Company YYYYY 02</ns2:label><ns2:debiteurnummer>1299342999</ns2:debiteurnummer></ns2:transactie></ns5:voorlopigeVorderingVastgelegd></soapenv:Body></soapenv:Envelope>";
	
	// 24-06-2019, 15:55 hr: NS-prefix XML test:
	@Test
    public void testXPathUsingSaxonOnXmlPayldWithNSPrefix() throws Exception {
        XPathFactory fac = new XPathFactoryImpl();
        XPathBuilder builder = XPathBuilder.xpath("//foo/bar").factory(fac);

        String name = builder.evaluate(context, xmlStr01, String.class);
        assertEquals("<bar xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" id=\"1\">cheese</bar>", name);        
    }

    @Test
    public void testXPathUsingSaxon() throws Exception {
        XPathFactory fac = new XPathFactoryImpl();
        XPathBuilder builder = XPathBuilder.xpath("foo/bar").factory(fac);

        String name = builder.evaluate(context, "<foo><bar id=\"1\">cheese</bar></foo>", String.class);
        assertEquals("<bar id=\"1\">cheese</bar>", name);

        name = builder.evaluate(context, "<foo><bar id=\"1\">cheese</bar></foo>");
        assertEquals("cheese", name);
    }

    @Test
    public void testXPathFunctionSubstringUsingSaxon() throws Exception {
        String xml = "<foo><bar>Hello World</bar></foo>";

        XPathFactory fac = new XPathFactoryImpl();
        XPathBuilder builder = XPathBuilder.xpath("substring(/foo/bar, 7)").factory(fac);

        String result = builder.resultType(String.class).evaluate(context, xml, String.class);
        assertEquals("World", result);

        result = builder.evaluate(context, xml);
        assertEquals("World", result);
    }

    @Test
    public void testXPathFunctionTokenizeUsingSaxonXPathFactory() throws Exception {
        // START SNIPPET: e1
        // create a Saxon factory
        XPathFactory fac = new net.sf.saxon.xpath.XPathFactoryImpl();

        // create a builder to evaluate the xpath using the saxon factory
        XPathBuilder builder = XPathBuilder.xpath("tokenize(/foo/bar, '_')[2]").factory(fac);

        // evaluate as a String result
        String result = builder.evaluate(context, "<foo><bar>abc_def_ghi</bar></foo>");
        assertEquals("def", result);
        // END SNIPPET: e1
    }

    @Test
    public void testXPathFunctionTokenizeUsingSaxonXPathFactoryOnXmlPayldWithNSPrefixes() throws Exception {
        // START SNIPPET: e1
        // create a Saxon factory
        XPathFactory fac = new net.sf.saxon.xpath.XPathFactoryImpl();

        // create a builder to evaluate the xpath using the saxon factory
        XPathBuilder builder = XPathBuilder.xpath("tokenize(//foo/bar, '_')[2]").factory(fac);			// Is this XQuery rather than XPath 2 ?

        // evaluate as a String result
        String result = builder.evaluate(context, xmlStr02);
        assertEquals("def", result);
        // END SNIPPET: e1
    }

    /*
     * 
     * Expected display output (the label12 unit test outcome is a bit odd, its integration test case yields: metadata label and body label values concatenated):
     * 
     * 13:25:51.578 [main] INFO  org.apache.camel.builder.saxon.test.XPathTest - ********************************************************************************
13:25:51.589 [main] INFO  org.apache.camel.builder.saxon.test.XPathTest - Testing: testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes(org.apache.camel.builder.saxon.test.XPathTest)
13:25:51.594 [main] INFO  org.apache.camel.builder.saxon.test.XPathTest - ********************************************************************************
13:25:51.906 [main] INFO  org.apache.camel.impl.DefaultCamelContext - Apache Camel 2.23.1 (CamelContext: camel-1) is starting
13:25:51.906 [main] INFO  org.apache.camel.management.DefaultManagementStrategy - JMX is disabled
13:25:52.078 [main] INFO  org.apache.camel.impl.converter.DefaultTypeConverter - Type converters loaded (core: 195, classpath: 60)
13:25:52.094 [main] INFO  org.apache.camel.impl.DefaultCamelContext - StreamCaching is not in use. If using streams then its recommended to enable stream caching. See more details at http://camel.apache.org/stream-caching.html
13:25:52.094 [main] INFO  org.apache.camel.impl.DefaultCamelContext - Total 0 routes, of which 0 are started
13:25:52.094 [main] INFO  org.apache.camel.impl.DefaultCamelContext - Apache Camel 2.23.1 (CamelContext: camel-1) started in 0.188 seconds
1. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label01 result: Company YYYYY 02
2. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label02 result: Company YYYYY 02
2b. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label02b result: Company YYYYY 02
3. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label03 result: 
4. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label04 result: Company YYYYY 02
5. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label05 result: 
6. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label06 result: 
7. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label07 result: 
8. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label08 result: 
9. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label09 result: 
10. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label10 result: Company YYYYY 01
11. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label11 result: 
12. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label12 result: Company YYYYY 01
13:25:52.891 [main] INFO  org.apache.camel.builder.saxon.test.XPathTest - ********************************************************************************
13:25:52.891 [main] INFO  org.apache.camel.builder.saxon.test.XPathTest - Testing done: testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes(org.apache.camel.builder.saxon.test.XPathTest)
13:25:52.891 [main] INFO  org.apache.camel.builder.saxon.test.XPathTest - Took: 0.797 seconds (797 millis)
13:25:52.891 [main] INFO  org.apache.camel.builder.saxon.test.XPathTest - ********************************************************************************
13:25:52.891 [main] INFO  org.apache.camel.impl.DefaultCamelContext - Apache Camel 2.23.1 (CamelContext: camel-1) is shutting down
13:25:52.891 [main] INFO  org.apache.camel.impl.DefaultCamelContext - Apache Camel 2.23.1 (CamelContext: camel-1) uptime 0.985 seconds
13:25:52.891 [main] INFO  org.apache.camel.impl.DefaultCamelContext - Apache Camel 2.23.1 (CamelContext: camel-1) is shutdown in 0.000 seconds
     * 
     */
    @Test
    public void testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes() throws Exception {
        // START SNIPPET: e1
        // create a Saxon factory
        XPathFactory fac = new net.sf.saxon.xpath.XPathFactoryImpl();

        // 1. create a builder to evaluate the xpath using the saxon factory
        XPathBuilder builder = XPathBuilder.xpath("//*[local-name()='transactie']/*[local-name()='label']/text()").factory(fac);			// Is this XQuery rather than XPath 2 ?
        // evaluate as a String result
        String result = builder.evaluate(context, xmlFullStr01);
        System.out.println("1. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label01 result: " + result);
        assertEquals("Company YYYYY 02", result);
        // END SNIPPET: e1
        
        // 2. create a builder to evaluate the xpath using the saxon factory
        builder = XPathBuilder.xpath("//ns2:label/text()").factory(fac)
        		.namespace("ns2", "http://ns.xxxx.nl/msg/transactie/v1");			// Is this XQuery rather than XPath 2 ?
        // evaluate as a String result
        result = builder.evaluate(context, xmlFullStr01);
        System.out.println("2. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label02 result: " + result);
        assertEquals("Company YYYYY 02", result);
        
        //--------------- 25062019: this works, as indicated in: http://saxon.sourceforge.net/saxon7.9.1/expressions.html (M. Kay):
        // 2b. create a builder to evaluate the xpath using the saxon factory
        builder = XPathBuilder.xpath("//*:voorlopigeVorderingVastgelegd//*:label/text()").factory(fac)
        		// .namespace("ns2", "http://ns.xxxx.nl/msg/transactie/v1")
        													;			// Is this XQuery rather than XPath 2 ?
																		// M. Kay explains: *:localname as: "to select nodes with a given local name, regardless of namespace"
        // evaluate as a String result
        result = builder.evaluate(context, xmlFullStr01);
        System.out.println("2b. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label02b result: " + result);
        assertEquals("Company YYYYY 02", result);
        
        // 3. create a builder to evaluate the xpath using the saxon factory
        builder = XPathBuilder.xpath("//label/text()").factory(fac);			// Is this XQuery rather than XPath 2 ?
        // evaluate as a String result
        result = builder.evaluate(context, xmlFullStr01);
        System.out.println("3. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label03 result: " + result);
        assertEquals("", result);
        // END SNIPPET: e1
        
        // 4. create a builder to evaluate the xpath using the saxon factory
        builder = XPathBuilder.xpath("//ns5:voorlopigeVorderingVastgelegd/ns2:transactie/ns2:label/text()").factory(fac)
        		.namespace("ns2", "http://ns.xxxx.nl/msg/transactie/v1")
        		.namespace("ns5", "http://ns.xxxx.nl/events/ext/transactie/v1");			// Is this XQuery rather than XPath 2 ?
        // evaluate as a String result
        result = builder.evaluate(context, xmlFullStr01);
        System.out.println("4. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label04 result: " + result);
        assertEquals("Company YYYYY 02", result);
        
        // 5. create a builder to evaluate the xpath using the saxon factory
        builder = XPathBuilder.xpath("//metadata/userId/text()").factory(fac);			// Is this XQuery rather than XPath 2 ?
        // evaluate as a String result
        result = builder.evaluate(context, xmlFullStr01);
        System.out.println("5. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label05 result: " + result);
        assertEquals("", result);
        // END SNIPPET: e1
        
        // 6. create a builder to evaluate the xpath using the saxon factory
        builder = XPathBuilder.xpath("//userId/text()").factory(fac);			// Is this XQuery rather than XPath 2 ?
        // evaluate as a String result
        result = builder.evaluate(context, xmlFullStr01);
        System.out.println("6. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label06 result: " + result);
        assertEquals("", result);
        // END SNIPPET: e1
        
        // 7. create a builder to evaluate the xpath using the saxon factory
        builder = XPathBuilder.xpath("//metadata/userId/text()").factory(fac)
        		.namespace("", "http://ns.xxxx.nl/msg/header/v2");			// Is this XQuery rather than XPath 2 ?
        			/* Default NS prefix ?? */
        
        // evaluate as a String result
        result = builder.evaluate(context, xmlFullStr01);
        System.out.println("7. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label07 result: " + result);
        assertEquals("", result);
        // END SNIPPET: e1
        
        // Vanaf 25062019:
        //
        // 8. create a builder to evaluate the xpath using the saxon factory
        builder = XPathBuilder.xpath("//metadata/userId/text()").factory(fac)
        		//.namespace("", "http://ns.xxxx.nl/msg/header/v2")
        		;			// Is this XQuery rather than XPath 2 ?
        					/* Default NS prefix ?? */
        
        // evaluate as a String result
        result = builder.evaluate(context, xmlFullStr01);
        System.out.println("8. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label08 result: " + result);
        assertEquals("", result);
        // END SNIPPET: e1
             
        // 9. create a builder to evaluate the xpath using the saxon factory        
        builder = XPathBuilder.xpath("//metadata[@xmlns=\"http://ns.xxxx.nl/msg/header/v2\"]/label/text()").factory(fac);			// Is this XQuery rather than XPath 2 ?

        // evaluate as a String result
        result = builder.evaluate(context, xmlFullStr01);
        System.out.println("9. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label09 result: " + result);
        assertEquals("", result);
        // END SNIPPET: e1
        
        // 10. create a builder to evaluate the xpath using the saxon factory
        builder = XPathBuilder.xpath("//*[local-name()='metadata']/*[local-name()='label']/text()").factory(fac);			// Is this XQuery rather than XPath 2 ?
        // evaluate as a String result
        result = builder.evaluate(context, xmlFullStr01);
        System.out.println("10. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label10 result: " + result);
        assertEquals("Company YYYYY 01", result);
        // END SNIPPET: e1
        
        // 11. create a builder to evaluate the xpath using the saxon factory
        
        builder = XPathBuilder.xpath("//metadata[@xmlns=\"http://ns.xxxx.nl/msg/header/v2\"]/label/text()").factory(fac);			// Is this XQuery rather than XPath 2 ?

        // evaluate as a String result
        result = builder.evaluate(context, xmlFullStr01);
        System.out.println("11. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label11 result: " + result);
        assertEquals("", result);
        // END SNIPPET: e1
        
        //--------------- 25062019: this works, as indicated in: http://saxon.sourceforge.net/saxon7.9.1/expressions.html (M. Kay):
        // 12. create a builder to evaluate the xpath using the saxon factory
        builder = XPathBuilder.xpath("//*:label/text()").factory(fac);			// Is this XQuery rather than XPath 2 ?
        																		// M. Kay explains: *:localname as: "to select nodes with a given local name, regardless of namespace"

        // evaluate as a String result
        result = builder.evaluate(context, xmlFullStr01);
        System.out.println("12. testXPathUsingSaxonXPathFactoryOnXmlFullPayldWithNSPrefixes: label12 result: " + result);
        assertEquals("Company YYYYY 01", result);			// This one is odd: integration test (which is 100 percent correct, yields: Company YYYYY 01Company YYYYY 02 
        // END SNIPPET: e1
        
    }

    @Ignore("See http://www.saxonica.com/documentation/index.html#!xpath-api/jaxp-xpath/factory")
    @Test
    public void testXPathFunctionTokenizeUsingObjectModel() throws Exception {
        // START SNIPPET: e2
        // create a builder to evaluate the xpath using saxon based on its object model uri
        XPathBuilder builder = XPathBuilder.xpath("tokenize(/foo/bar, '_')[2]").objectModel("http://saxon.sf.net/jaxp/xpath/om");

        // evaluate as a String result
        String result = builder.evaluate(context, "<foo><bar>abc_def_ghi</bar></foo>");
        assertEquals("def", result);
        // END SNIPPET: e2
    }

    @Test
    public void testXPathFunctionTokenizeUsingSaxon() throws Exception {
        // START SNIPPET: e3
        // create a builder to evaluate the xpath using saxon
        XPathBuilder builder = XPathBuilder.xpath("tokenize(/foo/bar, '_')[2]").saxon();

        // evaluate as a String result
        String result = builder.evaluate(context, "<foo><bar>abc_def_ghi</bar></foo>");
        assertEquals("def", result);
        // END SNIPPET: e3
    }

    @Test
    public void testXPathFunctionTokenizeUsingSystemProperty() throws Exception {
        // START SNIPPET: e4
        // set system property with the XPath factory to use which is Saxon 
        System.setProperty(XPathFactory.DEFAULT_PROPERTY_NAME + ":" + "http://saxon.sf.net/jaxp/xpath/om", "net.sf.saxon.xpath.XPathFactoryImpl");

        // create a builder to evaluate the xpath using saxon
        XPathBuilder builder = XPathBuilder.xpath("tokenize(/foo/bar, '_')[2]");

        // evaluate as a String result
        String result = builder.evaluate(context, "<foo><bar>abc_def_ghi</bar></foo>");
        assertEquals("def", result);
        // END SNIPPET: e4
    }
}
