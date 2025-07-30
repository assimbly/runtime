package org.assimbly.util.helper;

import org.apache.camel.Exchange;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExchangeHelper {

    private static final String HEADER_VARIABLE_REGEX = "\\$\\{headers?\\.(.+?)}";
    private static final String BODY_VARIABLE_REGEX = "\\$\\{body(As\\(String\\))?}";

    public static boolean hasVariables(String string){
        return (Pattern.compile(HEADER_VARIABLE_REGEX).matcher(string).find() || Pattern.compile(BODY_VARIABLE_REGEX).matcher(string).find());
    }
    private ExchangeHelper() {}

    public static String interpolate(String text, Exchange exchange){
        if(text == null)
            return null;

        for(String regex : Arrays.asList(HEADER_VARIABLE_REGEX, BODY_VARIABLE_REGEX)){
            text = replaceVariables(text, exchange, regex);
        }

        return text;
    }

    private static String replaceVariables(String text, Exchange exchange, String regex) {
        StringBuilder stringBuilder = new StringBuilder();
        Matcher m = Pattern.compile(regex).matcher(text);
        while(m.find()) {
            String value;
            switch (regex) {
                case HEADER_VARIABLE_REGEX:
                    value = exchange.getIn().getHeader(m.group(1), String.class);
                    break;
                case BODY_VARIABLE_REGEX:
                    value = exchange.getIn().getBody(String.class);
                    break;
                default:
                    return stringBuilder.toString();
            }
            if(value != null) {
                value = escapeDollarSign(value);
                m.appendReplacement(stringBuilder, unescapeExceptionalCharacters(value));
            }
        }
        m.appendTail(stringBuilder);
        return stringBuilder.toString();
    }

    public static String unescapeExceptionalCharacters(String str) {
        return str.replace("\\\\n", "\n")
                .replace("\\\\t", "\t")
                .replace("\\\\r", "\r");
    }

    static String escapeDollarSign(String str) {
        return str.replace("$", "\\$");
    }

}