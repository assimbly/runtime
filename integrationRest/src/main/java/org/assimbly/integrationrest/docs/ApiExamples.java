package org.assimbly.integrationrest.docs;

/**
 * Example request bodies for the OpenAPI documentation (annotation values must be compile-time constants).
 */
final class ApiExamples {

    private ApiExamples() {}

    static final String FLOW_JSON = """
            {"dil":{"integrations":{"integration":{"flows":{"flow":{
              "id":"69fc94aed4e0d00010000040","name":"Scheduler","type":"esb",
              "steps":{"step":[
                {"id":"b1c55ffa-2cf0-4135-bfcd-980c57d832d4","type":"source","uri":"quartz:69fc94aed4e0d00010000040_timer",
                 "options":{"timeZone":"Europe/Amsterdam","cron":"0 * * * * ?"},
                 "links":{"link":{"id":"1ae7ab8d-3b16-487c-bf9d-49ba9f56b8c7","transport":"sync","bound":"out"}}},
                {"id":"1ae7ab8d-3b16-487c-bf9d-49ba9f56b8c7","type":"sink","uri":"setbody",
                 "options":{"language":"simple","expression":"hello"},
                 "links":{"link":{"id":"1ae7ab8d-3b16-487c-bf9d-49ba9f56b8c7","transport":"sync","bound":"in"}}}
              ]},
              "options":{"environment":"test","tenant":"default","version":1}
            }}}}}}""";

    static final String ROUTE_XML = """
            <route id="route1">
                <from uri="timer://route1_timer?fixedRate=true&amp;period=10000&amp;repeatCount=1"/>
                <to uri="mock:route2"/>
            </route>""";

    static final String CERTIFICATE_PEM = """
            -----BEGIN CERTIFICATE-----
            MIIC...
            -----END CERTIFICATE-----""";

    static final String COLLECTOR_JSON = """
            {"id":"69fc94aed4e0d00010000040_log","flowId":"69fc94aed4e0d00010000040","flowVersion":1,"type":"log",
             "events":["org.assimbly"],"stores":[{"type":"elastic","uri":"http://elasticsearch:9200/logs/_doc"}],
             "filters":[{"filter":"69fc94aed4e0d00010000040"}]}""";
}
