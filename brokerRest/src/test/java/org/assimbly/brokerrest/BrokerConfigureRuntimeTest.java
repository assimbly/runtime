package org.assimbly.brokerrest;

import org.assimbly.brokerrest.testcontainers.AssimblyGatewayBrokerContainer;
import org.assimbly.brokerrest.utils.HttpUtil;
import org.assimbly.brokerrest.utils.TestApplicationContext;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.net.http.HttpResponse;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class BrokerConfigureRuntimeTest {

    private static AssimblyGatewayBrokerContainer container;

    @BeforeAll
    static void setUp() {
        container = new AssimblyGatewayBrokerContainer();
        container.init();
    }

    @AfterAll
    static void tearDown() {
        container.stop();
    }

    @Test
    void shouldGetBrokerConfiguration() {
        try {
            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/configure", baseUrl, "1");

            // params
            HashMap<String, String> params = new HashMap();
            params.put("brokerType", "classic");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_XML_VALUE);

            // endpoint call - check if backend is started
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, params, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            assertThat(response.body()).isEqualTo(TestApplicationContext.readFileAsStringFromResources("container/broker/activemq.xml"));

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldSetBrokerConfiguration() {
        try {
            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/configure", baseUrl, "1");

            // params
            HashMap<String, String> params = new HashMap();
            params.put("brokerType", "classic");
            params.put("brokerConfigurationType", ""); // the following param is not used internally

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_XML_VALUE);

            // body
            String brokerConfig = TestApplicationContext.readFileAsStringFromResources("container/broker/activemq.xml");

            // endpoint call - check if backend is started
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", brokerConfig, params, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            assertThat(response.body()).isEqualTo("configuration set");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
