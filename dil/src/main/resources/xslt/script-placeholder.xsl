<?xml version="1.0" encoding="UTF-8"?>
<!-- Loaded by the xslt-script kamelet when its endpoint starts; each message is transformed with the script from the CamelXsltStylesheet header instead. -->
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
