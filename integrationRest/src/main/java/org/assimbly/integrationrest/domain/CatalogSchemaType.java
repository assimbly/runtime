package org.assimbly.integrationrest.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "Type of Camel Catalog schema definition")
public enum CatalogSchemaType {
    COMPONENT,
    LANGUAGE,
    DATAFORMAT,
    MAIN,
    MODEL,
    TRANSFORMER,
    OTHER;

    public static CatalogSchemaType fromString(String value) {
        return CatalogSchemaType.valueOf(value.toUpperCase());
    }
}