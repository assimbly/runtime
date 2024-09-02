/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.assimbly.dil.blocks.beans;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultHeaderFilterStrategy;

public class CustomHttpHeaderFilterStrategy extends DefaultHeaderFilterStrategy {

    private static final String DATE_HEADER = "date";
    private static final String USE_CUSTOM_DATE_HEADER = "useCustomDateHeader";

    public CustomHttpHeaderFilterStrategy() {
        initialize();
    }

    protected void initialize() {

        getOutFilter().add("content-length");
        getOutFilter().add("content-type");
        //getOutFilter().add("host");
        getOutFilter().add("cache-control");
        getOutFilter().add("connection");
        getOutFilter().add("date");
        getOutFilter().add("pragma");
        getOutFilter().add("trailer");
        getOutFilter().add("transfer-encoding");
        getOutFilter().add("upgrade");
        getOutFilter().add("via");
        getOutFilter().add("warning");

        setLowerCase(true);

        // filter headers begin with "Camel" or "org.apache.camel"
        // must ignore case for Http based transports
        setOutFilterStartsWith(CAMEL_FILTER_STARTS_WITH);
        setInFilterStartsWith(CAMEL_FILTER_STARTS_WITH);
    }

    @Override
    public boolean applyFilterToCamelHeaders(String headerName, Object headerValue, Exchange exchange) {
        if (skipFilter(headerName, exchange)) {
            // filter is not applied
            return false;
        }
        // it will apply filter
        return super.applyFilterToCamelHeaders(headerName, headerValue, exchange);
    }

    @Override
    public boolean applyFilterToExternalHeaders(String headerName, Object headerValue, Exchange exchange) {
        if (skipFilter(headerName, exchange)) {
            // filter is not applied
            return false;
        }
        // it will apply filter
        return super.applyFilterToExternalHeaders(headerName, headerValue, exchange);
    }

    private boolean skipFilter(String headerName, Exchange exchange) {
        // Check if the key is the date header
        if (headerName.equalsIgnoreCase(DATE_HEADER)) {
            Object useCustomHeaderObj = exchange.getProperty(USE_CUSTOM_DATE_HEADER);
            boolean useCustomHeader = Boolean.parseBoolean(String.valueOf(useCustomHeaderObj));

            // If useCustomHeader is true, do not filter the date header
            if (useCustomHeader) {
                return true;
            }
        }
        return false;
    }
}
