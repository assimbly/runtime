package org.assimbly.integration.routes.templates;

import org.apache.camel.builder.RouteBuilder;

public class XmlToJson extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("myTemplate")
                 // here we define the required input parameters (can have default values)
                 .templateParameter("name")
                 .templateParameter("greeting")
                 .templateParameter("myPeriod", "3s")
                 // here comes the route in the template
                 // notice how we use {{name}} to refer to the template parameters
                 // we can also use {{propertyName}} to refer to property placeholders
                 .from("timer:{{name}}?period={{myPeriod}}")
                 .setBody(simple("{{greeting}} ${body}"))
                 .log("${body}");


         routeTemplate("xmltojson")
                 .templateParameter("id")

                 //transport parameters
                 .templateParameter("transport.in.type","kamelet")
                 .templateParameter("transport.in.id", "source")
                 .templateParameter("transport.in.params","")
                 .templateParameter("transport.out.type","sync")
                 .templateParameter("transport.out.id")
                 .templateParameter("transport.out.params","")

                 //step parameters
                 .templateParameter("keepStrings","false")
                 .templateParameter("removeNamespaces","false")
                 .templateParameter("removeRoot","false")
                 .templateParameter("hasTypes","false")
                 .templateParameter("typeValueMismatch","ORIGINAL")

                 .from("{{transport.in.type}}:{{transport.in.id}}{{transport.in.params}}")
                     .to("dataformat:custom-xmljson:marshal?keepStrings={{keepStrings}}&removeNamespaces={{removeNamespace}}&removeRoot=false&hasTypes={{removeRoot}}&typeValueMismatch={{typeValueMismatch}}")
                     .to("{{transport.out.type}}:{{transport.out.id}}{{transport.out.params}}")
                     .routeId("{{id}}");



         /*
         routeTemplate("xmltojson-action")
                 .templateParameter("transport.in.type","kamelet")
                 .templateParameter("transport.in.id", "source")
                 .templateParameter("transport.in.params","")
                 .templateParameter("transport.out.type","sync")
                 .templateParameter("transport.out.id")
                 .templateParameter("transport.out.params","")
                 .from("{{transport.in.type}}:{{transport.in.id}}{{transport.in.params}}")
        		 .to("")
                 .to("{{transport.out.type}}:{{transport.out.id}}{{transport.out.params}}");

         routeTemplate("xmltojson-sink")
                 .templateParameter("transport.in.type","kamelet")
                 .templateParameter("transport.in.id", "source")
                 .templateParameter("transport.in.params","")
                 .from("{{transport.in.type}}:{{transport.in.id}}{{transport.in.params}}")
                 .to("")
                 .to("{{transport.out.type}}:{{transport.out.id}}{{transport.out.params}}");

         routeTemplate("jsontoxml-sink")
                 .templateParameter("transport.in.type","kamelet")
                 .templateParameter("transport.in.id", "source")
                 .templateParameter("transport.in.params","")
                 .from("{{transport.in.type}}:{{transport.in.id}}{{transport.in.params}}")
                 .to("")
                 .to("{{transport.out.type}}:{{transport.out.id}}{{transport.out.params}}");
        */


    }

}
