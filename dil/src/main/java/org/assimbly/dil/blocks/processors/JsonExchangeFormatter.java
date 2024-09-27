package org.assimbly.dil.blocks.processors;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Future;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePropertyKey;
import org.apache.camel.Message;
import org.apache.camel.Route;
import org.apache.camel.spi.Configurer;
import org.apache.camel.spi.ExchangeFormatter;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;
import org.apache.camel.support.ExceptionHelper;
import org.apache.camel.support.ExchangeHelper;
import org.apache.camel.support.MessageHelper;
import org.apache.camel.util.ObjectHelper;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Default {@link ExchangeFormatter} that have fine grained options to configure what to include in the output.
 */
@UriParams
@Configurer
public class JsonExchangeFormatter implements ExchangeFormatter {

    protected static final String LS = System.lineSeparator();
    @UriParam(label = "formatting", description = "Show route ID.")
    private boolean showRouteId;
    @UriParam(label = "formatting", description = "Show route Group.")
    private boolean showRouteGroup;
    @UriParam(label = "formatting", description = "Show the unique exchange ID.")
    private boolean showExchangeId;
    @UriParam(label = "formatting", description = "Shows the Message Exchange Pattern (or MEP for short).")
    private boolean showExchangePattern;
    @UriParam(label = "formatting", description = "Show the exchange properties (only custom).")
    private boolean showProperties;
    @UriParam(label = "formatting", description = "Show all the exchange properties (both internal and custom).")
    private boolean showAllProperties;
    @UriParam(label = "formatting", description = "Show the variables.")
    private boolean showVariables;
    @UriParam(label = "formatting", description = "Show the message headers.")
    private boolean showHeaders;
    @UriParam(label = "formatting", defaultValue = "true",
            description = "Whether to skip line separators when logging the message body."
                    + " This allows to log the message body in one line, setting this option to false will preserve any line separators from the body, which then will log the body as is.")
    private boolean skipBodyLineSeparator = true;
    @UriParam(label = "formatting", defaultValue = "true", description = "Show the message body.")
    private boolean showBody = true;
    @UriParam(label = "formatting", defaultValue = "true", description = "Show the body Java type.")
    private boolean showBodyType = true;
    @UriParam(label = "formatting",
            description = "If the exchange has an exception, show the exception message (no stacktrace)")
    private boolean showException;
    @UriParam(label = "formatting",
            description = "f the exchange has a caught exception, show the exception message (no stack trace)."
                    + " A caught exception is stored as a property on the exchange (using the key org.apache.camel.Exchange#EXCEPTION_CAUGHT) and for instance a doCatch can catch exceptions.")
    private boolean showCaughtException;
    @UriParam(label = "formatting",
            description = "Show the stack trace, if an exchange has an exception. Only effective if one of showAll, showException or showCaughtException are enabled.")
    private boolean showStackTrace;
    @UriParam(label = "formatting",
            description = "Quick option for turning all options on. (multiline, maxChars has to be manually set if to be used)")
    private boolean showAll;
    @UriParam(label = "formatting",
            description = "If enabled Camel will on Future objects wait for it to complete to obtain the payload to be logged.")
    private boolean showFuture;
    @UriParam(label = "formatting", defaultValue = "true",
            description = "Whether Camel should show cached stream bodies or not (org.apache.camel.StreamCache).")
    private boolean showCachedStreams = true;
    @UriParam(label = "formatting",
            description = "Whether Camel should show stream bodies or not (eg such as java.io.InputStream). Beware if you enable this option then "
                    + "you may not be able later to access the message body as the stream have already been read by this logger. To remedy this you will have to use Stream Caching.")
    private boolean showStreams;
    @UriParam(label = "formatting", description = "If enabled Camel will output files")
    private boolean showFiles;
    @UriParam(label = "formatting", defaultValue = "10000", description = "Limits the number of characters logged per line.")
    private int maxChars = 10000;
    private JSONObject json;


    @Override
    public String format(Exchange exchange) {

        json = new JSONObject();

        addExchangeInfo(exchange);

        addRouteInfo(exchange);

        addProperties(exchange);

        addVariables(exchange);

        addHeaders(exchange);

        addBody(exchange.getIn());

        addExceptionInfo(exchange);

        return json.toString(3);

    }

    private void addExchangeInfo(Exchange exchange) {
        if (showAll || showExchangeId) {
            json.put("ExchangeId", exchange.getExchangeId());
        }
        if (showAll || showExchangePattern) {
            json.put("ExchangePattern", exchange.getPattern());
        }
    }

    private void addRouteInfo(Exchange exchange) {
        if (showAll || showRouteGroup || showRouteId) {
            Route route = ExchangeHelper.getRoute(exchange);
            if (route != null) {
                if (showAll || showRouteGroup) {
                    json.put("RouteGroup", route.getGroup());
                }
                if (showAll || showRouteId) {
                    json.put("RouteId", route.getRouteId());
                }
            }
        }
    }

    private void addProperties(Exchange exchange) {
        if (showAll || showAllProperties || showProperties) {
            JSONObject properties = getJsonFromMap(filterHeaderAndProperties(exchange.getProperties()));
            json.put("Properties", properties);
        }
    }

    private void addVariables(Exchange exchange) {
        if (showAll || showVariables) {
            if (exchange.hasVariables()) {
                JSONObject variables = getJsonFromMap(filterHeaderAndProperties(exchange.getVariables()));
                json.put("Variables", variables);
            }
        }
    }

    private void addHeaders(Exchange exchange) {
        if (showAll || showHeaders) {
            JSONObject headers = getJsonFromMap(filterHeaderAndProperties(exchange.getIn().getHeaders()));
            json.put("Headers", headers);
        }
    }

    private void addBody(Message in) {
        if (showAll || showBodyType) {
            json.put("BodyType", getBodyTypeAsString(in));
        }
        if (showAll || showBody) {
            String body = getBodyAsString(in);
            if (skipBodyLineSeparator && body != null) {
                body = body.replace(LS, "");
            }
            json.put("Body", body);
        }
    }

    private void addExceptionInfo(Exchange exchange) {
        if (showAll || showException || showCaughtException) {
            Exception exception = exchange.getException();
            boolean caught = false;

            if ((showAll || showCaughtException) && exception == null) {
                exception = exchange.getProperty(ExchangePropertyKey.EXCEPTION_CAUGHT, Exception.class);
                caught = true;
            }

            if (exception != null) {
                String exceptionType = exception.getClass().getCanonicalName();
                String exceptionMessage = exception.getMessage();

                if (caught) {
                    json.put("CaughtExceptionType", exceptionType);
                    json.put("CaughtExceptionMessage", exceptionMessage);
                } else {
                    json.put("ExceptionType", exceptionType);
                    json.put("ExceptionMessage", exceptionMessage);
                }

                if (showAll || showStackTrace) {
                    json.put("StackTrace", ExceptionHelper.stackTraceToString(exception));
                }
            }
        }
    }

    /**
     * Filters the headers or properties before formatting them. No default behavior, but can be overridden.
     */
    protected Map<String, Object> filterHeaderAndProperties(Map<String, Object> map) {
        return map;
    }

    public boolean isShowRouteId() {
        return showRouteId;
    }

    /**
     * Shows the route id (if exchange is being processed in routes)
     */
    public void setShowRouteId(boolean showRouteId) {
        this.showRouteId = showRouteId;
    }

    /**
     * Shows the route group (if exchange is being processed in routes)
     */
    public boolean isShowRouteGroup() {
        return showRouteGroup;
    }

    public void setShowRouteGroup(boolean showRouteGroup) {
        this.showRouteGroup = showRouteGroup;
    }

    public boolean isShowExchangeId() {
        return showExchangeId;
    }

    /**
     * Show the unique exchange ID.
     */
    public void setShowExchangeId(boolean showExchangeId) {
        this.showExchangeId = showExchangeId;
    }

    public boolean isShowProperties() {
        return showProperties;
    }

    /**
     * Show the exchange properties (only custom). Use showAllProperties to show both internal and custom properties.
     */
    public void setShowProperties(boolean showProperties) {
        this.showProperties = showProperties;
    }

    public boolean isShowAllProperties() {
        return showAllProperties;
    }

    /**
     * Show all of the exchange properties (both internal and custom).
     */
    public void setShowAllProperties(boolean showAllProperties) {
        this.showAllProperties = showAllProperties;
    }

    public boolean isShowVariables() {
        return showVariables;
    }

    /**
     * Show the variables.
     */
    public void setShowVariables(boolean showVariables) {
        this.showVariables = showVariables;
    }

    public boolean isShowHeaders() {
        return showHeaders;
    }

    /**
     * Show the message headers.
     */
    public void setShowHeaders(boolean showHeaders) {
        this.showHeaders = showHeaders;
    }

    public boolean isSkipBodyLineSeparator() {
        return skipBodyLineSeparator;
    }

    /**
     * Whether to skip line separators when logging the message body. This allows to log the message body in one line,
     * setting this option to false will preserve any line separators from the body, which then will log the body as is.
     */
    public void setSkipBodyLineSeparator(boolean skipBodyLineSeparator) {
        this.skipBodyLineSeparator = skipBodyLineSeparator;
    }

    public boolean isShowBodyType() {
        return showBodyType;
    }

    /**
     * Show the body Java type.
     */
    public void setShowBodyType(boolean showBodyType) {
        this.showBodyType = showBodyType;
    }

    public boolean isShowBody() {
        return showBody;
    }

    /*
     * Show the message body.
     */
    public void setShowBody(boolean showBody) {
        this.showBody = showBody;
    }

    public boolean isShowAll() {
        return showAll;
    }

    /**
     * Quick option for turning all options on. (multiline, maxChars has to be manually set if to be used)
     */
    public void setShowAll(boolean showAll) {
        this.showAll = showAll;
    }

    public boolean isShowException() {
        return showException;
    }

    /**
     * If the exchange has an exception, show the exception message (no stacktrace)
     */
    public void setShowException(boolean showException) {
        this.showException = showException;
    }

    public boolean isShowStackTrace() {
        return showStackTrace;
    }

    /**
     * Show the stack trace, if an exchange has an exception. Only effective if one of showAll, showException or
     * showCaughtException are enabled.
     */
    public void setShowStackTrace(boolean showStackTrace) {
        this.showStackTrace = showStackTrace;
    }

    public boolean isShowCaughtException() {
        return showCaughtException;
    }

    /**
     * If the exchange has a caught exception, show the exception message (no stack trace). A caught exception is stored
     * as a property on the exchange (using the key {@link org.apache.camel.Exchange#EXCEPTION_CAUGHT} and for instance
     * a doCatch can catch exceptions.
     */
    public void setShowCaughtException(boolean showCaughtException) {
        this.showCaughtException = showCaughtException;
    }

    public int getMaxChars() {
        return maxChars;
    }

    /**
     * Limits the number of characters logged per line.
     */
    public void setMaxChars(int maxChars) {
        this.maxChars = maxChars;
    }

    public boolean isShowFuture() {
        return showFuture;
    }

    /**
     * If enabled Camel will on Future objects wait for it to complete to obtain the payload to be logged.
     */
    public void setShowFuture(boolean showFuture) {
        this.showFuture = showFuture;
    }

    public boolean isShowExchangePattern() {
        return showExchangePattern;
    }

    /**
     * Shows the Message Exchange Pattern (or MEP for short).
     */
    public void setShowExchangePattern(boolean showExchangePattern) {
        this.showExchangePattern = showExchangePattern;
    }

    public boolean isShowCachedStreams() {
        return showCachedStreams;
    }

    /**
     * Whether Camel should show cached stream bodies or not (org.apache.camel.StreamCache).
     */
    public void setShowCachedStreams(boolean showCachedStreams) {
        this.showCachedStreams = showCachedStreams;
    }

    public boolean isShowStreams() {
        return showStreams;
    }

    /**
     * Whether Camel should show stream bodies or not (eg such as java.io.InputStream). Beware if you enable this option
     * then you may not be able later to access the message body as the stream have already been read by this logger. To
     * remedy this you will have to use Stream Caching.
     */
    public void setShowStreams(boolean showStreams) {
        this.showStreams = showStreams;
    }

    public boolean isShowFiles() {
        return showFiles;
    }

    /**
     * If enabled Camel will output files
     */
    public void setShowFiles(boolean showFiles) {
        this.showFiles = showFiles;
    }

    // Implementation methods
    //-------------------------------------------------------------------------
    protected String getBodyAsString(Message message) {
        if (message.getBody() instanceof Future) {
            if (!isShowFuture()) {
                // just use to string of the future object
                return message.getBody().toString();
            }
        }

        return MessageHelper.extractBodyForLogging(message, null, isShowCachedStreams(), isShowStreams(), isShowFiles(), getMaxChars(message));
    }

    private int getMaxChars(Message message) {
        int maxChars = getMaxChars();
        if (message.getExchange() != null) {
            String globalOption = message.getExchange().getContext().getGlobalOption(Exchange.LOG_DEBUG_BODY_MAX_CHARS);
            if (globalOption != null) {
                maxChars = message.getExchange().getContext().getTypeConverter().convertTo(Integer.class, globalOption);
            }
        }
        return maxChars;
    }

    protected String getBodyTypeAsString(Message message) {
        String answer = ObjectHelper.classCanonicalName(message.getBody());
        if (answer != null && answer.startsWith("java.lang.")) {
            return answer.substring(10);
        }
        return answer;
    }

    private JSONObject getJsonFromMap(Map<String, Object> map) throws JSONException {
        JSONObject jsonData = new JSONObject();
        for (String key : map.keySet()) {
            Object value = map.get(key);
            if (value instanceof Map<?, ?>) {
                value = getJsonFromMap((Map<String, Object>) value);
            }
            jsonData.put(key, value);
        }
        return jsonData;
    }

}