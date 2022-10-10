package org.assimbly.dil.blocks.beans;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;

import java.util.UUID;

public class UuidExtensionFunction extends ExtensionFunctionDefinition {

    @Override
    public StructuredQName getFunctionQName() {
        return new StructuredQName("uuid", "http://assimbly.org/uuid-saxon-extension", "uuid");
    }

    @Override
    public SequenceType[] getArgumentTypes() {
        return new SequenceType[0];
    }

    @Override
    public SequenceType getResultType(SequenceType[] sequenceTypes) {
        return SequenceType.SINGLE_STRING;
    }

    @Override
    public ExtensionFunctionCall makeCallExpression() {
        return new ExtensionFunctionCall() {
            @Override
            public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
                UUID uuid = UUID.randomUUID();

                return  StringValue.makeStringValue(uuid.toString());
            }
        };
    }
}