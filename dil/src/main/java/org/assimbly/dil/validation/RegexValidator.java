package org.assimbly.dil.validation;

import org.assimbly.dil.validation.beans.Regex;

import java.util.AbstractMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexValidator {

    public AbstractMap.SimpleEntry<Integer, String> validate(Regex expression) {

        AbstractMap.SimpleEntry<Integer, String> response;
        String regex = expression.getExpression();

        if (regex.isEmpty()) {
            return new AbstractMap.SimpleEntry<>(-1, "Regex cannot be empty");
        }

        try {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher("");
            response = new AbstractMap.SimpleEntry<>(1, String.valueOf(matcher.groupCount()));

        } catch (PatternSyntaxException e) {
            response = new AbstractMap.SimpleEntry<>(-1, e.getMessage());
        }

        return response;
    }

}
