package org.assimbly.dil.blocks.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.event.domain.FlowEvent;
import org.assimbly.util.BaseDirectory;

import java.io.File;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class FailureProcessor implements Processor {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");

    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();

    public void process(Exchange exchange) throws Exception {

        // Write alert to disk
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        String today = now.format(DATE_FORMATTER);
        String timestamp = now.format(TIMESTAMP_FORMATTER);

        String exception = Optional.ofNullable(exchange.getException())
                .map(Throwable::getMessage)
                .orElse("unknown exception");

        // Convert ZonedDateTime to Date if FlowEvent strictly requires java.util.Date
        FlowEvent flowEvent = new FlowEvent(exchange.getFromRouteId(), now.toInstant(), exception);

        String flowId;
        if (!flowEvent.getFlowId().contains("-")) {
            flowId = flowEvent.getFlowId();
        } else {
            flowId = StringUtils.substringBefore(flowEvent.getFlowId(), "-");
        }

        File file = new File(baseDir + "/alerts/" + flowId + "/" + today + "_alerts.log");
        List<String> line = List.of(timestamp + " : " + flowEvent.getError());
        FileUtils.writeLines(file, line, true);

    }

}