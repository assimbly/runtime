package org.assimbly.dil.kamelets;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteConfigurationBuilder;
import org.apache.camel.builder.TemplatedRouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.PluginHelper;
import org.apache.camel.support.ResourceHelper;
import org.assimbly.dil.blocks.processors.UblSchemaValidationProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The UBL action passes valid UBL 2.1 documents on (normalized) and fails on anything else. */
class UblKameletTest {

    private static final String INVOICE = """
            <Invoice xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2"
                     xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"
                     xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2">
              <cbc:ID>INV-1</cbc:ID>
              <cbc:IssueDate>2026-10-18</cbc:IssueDate>
              <cac:AccountingSupplierParty><cac:Party><cac:PartyName><cbc:Name>Supplier</cbc:Name></cac:PartyName></cac:Party></cac:AccountingSupplierParty>
              <cac:AccountingCustomerParty><cac:Party><cac:PartyName><cbc:Name>Customer</cbc:Name></cac:PartyName></cac:Party></cac:AccountingCustomerParty>
              <cac:LegalMonetaryTotal><cbc:PayableAmount currencyID="EUR">100.00</cbc:PayableAmount></cac:LegalMonetaryTotal>
              <cac:InvoiceLine>
                <cbc:ID>1</cbc:ID>
                <cbc:LineExtensionAmount currencyID="EUR">100.00</cbc:LineExtensionAmount>
                <cac:Item><cbc:Name>Widget</cbc:Name></cac:Item>
              </cac:InvoiceLine>
            </Invoice>""";

    private CamelContext context;

    @BeforeEach
    void startContext() throws Exception {
        context = new DefaultCamelContext();
        context.getRegistry().bind("UblSchemaValidationProcessor", new UblSchemaValidationProcessor());
        context.addRoutes(new RouteConfigurationBuilder() {
            @Override
            public void configuration() {
                routeConfiguration("0");
            }
        });
        context.start();
    }

    @AfterEach
    void stopContext() {
        context.stop();
    }

    @Test
    void validInvoicePassesAsUblXml() throws Exception {
        String result = run(false, INVOICE);

        assertTrue(result.contains("Invoice"), result);
        assertTrue(result.contains("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2"), result);
        assertTrue(result.contains("INV-1"), result);
        assertFalse(result.contains("\n  "), result);
    }

    @Test
    void prettyPrintFormatsTheOutput() throws Exception {
        String result = run(true, INVOICE);

        assertTrue(result.contains("INV-1"), result);
        assertTrue(result.contains("\n "), result);
    }

    @Test
    void nonUblDocumentFails() {
        assertThrows(CamelExecutionException.class, () -> run(false, "<order><id>1</id></order>"));
    }

    @Test
    void invoiceThatBreaksTheSchemaFails() {
        String withoutIssueDate = INVOICE.replace("<cbc:IssueDate>2026-10-18</cbc:IssueDate>", "");

        assertThrows(CamelExecutionException.class, () -> run(false, withoutIssueDate));
    }

    private String run(boolean prettyPrint, String body) throws Exception {
        PluginHelper.getRoutesLoader(context).loadRoutes(
            ResourceHelper.resolveMandatoryResource(context, "classpath:kamelets/ubl-action.kamelet.yaml"));

        TemplatedRouteBuilder.builder(context, "ubl-action")
            .routeId("ubl-action-route")
            .parameter("routeId", "ubl-action-route")
            .parameter("stepId", "1")
            .parameter("in", "direct:in")
            .parameter("sink", "true")
            .parameter("prettyPrint", String.valueOf(prettyPrint))
            .add();

        try (ProducerTemplate template = context.createProducerTemplate()) {
            return template.requestBody("direct:in", body, String.class);
        }
    }
}
