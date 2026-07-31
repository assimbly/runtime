package org.assimbly.integrationrest.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "Type of Camel Catalog element list")
public enum CatalogListType {
    COMPONENTS,
    DATAFORMATS,
    LANGUAGES,
    MODELS,
    BEANS,
    TRANSFORMERS,
    OTHERS;

    public static CatalogListType fromString(String value) {
        return CatalogListType.valueOf(value.toUpperCase());
    }
}