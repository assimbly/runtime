package org.assimbly.dil.validation.saxon;

import net.sf.saxon.Configuration;

public class SaxonConfiguration extends Configuration {

    public SaxonConfiguration() {
        registerExtensionFunction(new UuidExtensionFunction());
    }
}
