<?xml version="1.0"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                
  <xsl:output indent="yes"/>                

  <xsl:template match="/" >
  
    <xsl:choose>
        <xsl:when test="integrations">
            <xsl:copy-of select="/"/>            
        </xsl:when>
        <xsl:when test="//*:route">

            <integrations>
                <integration>
                  <id>1</id>
                  <name>default</name>
                  <type>FULL</type>
                  <environmentName>PRODUCTION</environmentName>
                  <stage>PRODUCTION</stage>
                  <flows>
                    <flow>
                      <id>
                        <xsl:choose>
                            <xsl:when test="//*:camelContext/@id">
    					        <xsl:value-of select="//*:camelContext/@id"/>
                            </xsl:when>
                            <xsl:when test="//*:routes/@id">
        				        <xsl:value-of select="//*:routes/@id"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of  select="current-dateTime()"/>
                            </xsl:otherwise>
                        </xsl:choose>    
					  </id>
                      <name>
                        <xsl:choose>
                            <xsl:when test="//*:camelContext/@id">
    					        <xsl:value-of select="//*:camelContext/@id"/>
                            </xsl:when>
                            <xsl:when test="//*:routes/@id">
        				        <xsl:value-of select="//*:routes/@id"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of  select="current-dateTime()"/>
                            </xsl:otherwise>
                        </xsl:choose>
					  </name>
                      <type>esb</type>
                      <components>
                         <xsl:for-each-group select="//*:from|//*:to|//*:toD" group-by="substring-before(@uri, ':' )" >
                            <component>
                              <xsl:value-of select="substring-before(@uri, ':' )"/>
                            </component>
                          </xsl:for-each-group>
                     </components>  					  
                      <endpoints>
                        <xsl:for-each select="//*:route">
                            <endpoint>
                              <id>
                                 <xsl:choose>
                                    <xsl:when test="@id">
                        		        <xsl:value-of select="@id"/>                              
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of  select="generate-id(.)"/>
                                    </xsl:otherwise>                            
                                </xsl:choose>                               </id>
                              <type>route</type>
                              <route_id>
                                 <xsl:choose>
                                    <xsl:when test="@id">
                    			        <xsl:value-of select="@id"/>                              
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of  select="generate-id(.)"/>
                                    </xsl:otherwise>                            
                                </xsl:choose>                                  
                              </route_id>
                            </endpoint>                            
                        </xsl:for-each>
                      </endpoints>
                    </flow>
                  </flows>
                  <routes>
                        <xsl:for-each select="//*:route">
                            <route>
                                 <xsl:choose>
                                    <xsl:when test="@id">
                                        <xsl:attribute name="id" select="@id"/> 
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:attribute name="id" select="generate-id(.)"/> 
                                    </xsl:otherwise>                            
                                </xsl:choose>
                                <xsl:copy-of select="./*" copy-namespaces="no"/>
                            </route>
                        </xsl:for-each>
                    </routes>
                </integration>
              </integrations>    
    
         </xsl:when>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>