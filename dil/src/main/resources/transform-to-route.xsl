<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output omit-xml-declaration="yes" indent="yes"/>

    <xsl:variable name="timestamp">
        <xsl:value-of select="format-dateTime(current-dateTime(),'[Y][M00][D00][H00][m00][s00][f000]')"/>
    </xsl:variable>

    <xsl:template match="/">

        <route>
            <xsl:variable name="flowid" select="//flows[1]/flow[1]/id"/>
            <xsl:attribute name="id" select="$flowid"/>
            <xsl:for-each select="//flows[1]/flow[1]/steps/step">
                <xsl:choose>
                    <xsl:when test="position() = 1">
                        <from>
                            <xsl:attribute name="uri">
                                <xsl:text>function:</xsl:text>
                                <xsl:choose>
                                    <xsl:when test="contains(uri, ':')">
                                        <xsl:value-of select="substring-before(uri, ':')"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="uri"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                                <xsl:text>-</xsl:text>
                                <xsl:value-of select="type"/>
                                <xsl:text>?</xsl:text>
                                <xsl:for-each select="links/link">
                                    <xsl:value-of select="bound"/>
                                    <xsl:text>=</xsl:text>
                                    <xsl:value-of select="concat(transport,':', $flowid,'-',id)"/>
                                    <xsl:if test="position() != last()">
                                        <xsl:text>&amp;</xsl:text>
                                    </xsl:if>
                                </xsl:for-each>
                                <xsl:if test="substring-after(uri, ':') != ''">
                                    <xsl:text>&amp;path=</xsl:text>
                                    <xsl:value-of select="substring-after(uri, ':')"/>
                                </xsl:if>
                                <xsl:if test="options">
                                    <xsl:text>&amp;</xsl:text>
                                    <xsl:for-each select="options/*">
                                        <xsl:value-of select="name()"/>
                                        <xsl:text>=</xsl:text>
                                        <xsl:value-of select="."/>
                                        <xsl:if test="position() != last()">
                                            <xsl:text>&amp;</xsl:text>
                                        </xsl:if>
                                    </xsl:for-each>
                                </xsl:if>
                                <xsl:text>&amp;location=ref:</xsl:text>
                                <xsl:choose>
                                    <xsl:when test="contains(uri, ':')">
                                        <xsl:value-of select="substring-before(uri, ':')"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="uri"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                                <xsl:text>-</xsl:text>
                                <xsl:value-of select="type"/>
                            </xsl:attribute>
                        </from>

                    </xsl:when>
                    <xsl:otherwise>
                        <step>
                            <xsl:attribute name="id" select="concat($flowid,'-', id)"/>
                            <to>
                                <xsl:attribute name="uri">
                                    <xsl:text>function:</xsl:text>
                                    <xsl:choose>
                                        <xsl:when test="contains(uri, ':')">
                                            <xsl:value-of select="substring-before(uri, ':')"/>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:value-of select="uri"/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                    <xsl:text>-</xsl:text>
                                    <xsl:value-of select="type"/>
                                    <xsl:text>?</xsl:text>
                                    <xsl:for-each select="links/link">
                                        <xsl:value-of select="bound"/>
                                        <xsl:text>=</xsl:text>
                                        <xsl:value-of select="concat(transport,':', $flowid,'-',id)"/>
                                        <xsl:if test="position() != last()">
                                            <xsl:text>&amp;</xsl:text>
                                        </xsl:if>
                                    </xsl:for-each>
                                    <xsl:if test="substring-after(uri, ':') != ''">
                                        <xsl:text>&amp;path=</xsl:text>
                                        <xsl:value-of select="substring-after(uri, ':')"/>
                                    </xsl:if>
                                    <xsl:if test="options">
                                        <xsl:text>&amp;</xsl:text>
                                        <xsl:for-each select="options/*">
                                            <xsl:value-of select="name()"/>
                                            <xsl:text>=</xsl:text>
                                            <xsl:value-of select="."/>
                                            <xsl:if test="position() != last()">
                                                <xsl:text>&amp;</xsl:text>
                                            </xsl:if>
                                        </xsl:for-each>
                                    </xsl:if>
                                    <xsl:text>&amp;location=ref:</xsl:text>
                                    <xsl:choose>
                                        <xsl:when test="contains(uri, ':')">
                                            <xsl:value-of select="substring-before(uri, ':')"/>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:value-of select="uri"/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                    <xsl:text>-</xsl:text>
                                    <xsl:value-of select="type"/>
                                </xsl:attribute>
                            </to>
                        </step>
                    </xsl:otherwise>
                </xsl:choose>


            </xsl:for-each>
        </route>

    </xsl:template>

</xsl:stylesheet>
