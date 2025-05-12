<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="text" omit-xml-declaration="yes" indent="yes"/>
    <xsl:preserve-space elements="added removed modified file folder"/>
    <xsl:template match="/dist-diff2">
        Distributions
        Folder A : "<xsl:value-of select="folderA"/>"
        Folder B : "<xsl:value-of select="folderB"/>"

        <xsl:apply-templates select="artifacts"/>
    </xsl:template>

    <xsl:template match="artifacts">
        <xsl:apply-templates select="folder|file|jar|archive"/>
    </xsl:template>

    <xsl:template match="folder">
        <xsl:call-template name="make-space">
            <xsl:with-param name="STATUS">
                <xsl:if test="not(contains(@status,'SAME'))">
                    <xsl:value-of select="@status"/>
                </xsl:if>
            </xsl:with-param>
        </xsl:call-template>
        <xsl:value-of select="@relative-path"/>
        <xsl:text>
        </xsl:text>
    </xsl:template>

    <xsl:template match="jar|archive|file">
        <xsl:call-template name="make-space">
            <xsl:with-param name="STATUS">
                <xsl:if test="not(contains(@status,'SAME'))">
                    <xsl:value-of select="@status"/>
                </xsl:if>
            </xsl:with-param>
        </xsl:call-template>
        <xsl:value-of select="@relative-path"/>
        <xsl:text>
        </xsl:text>
    </xsl:template>

    <xsl:template name="make-space">
        <xsl:param name="STATUS">empty</xsl:param>
        <xsl:value-of select="substring(concat($STATUS, '                        '), 1, 22)"/>
    </xsl:template>
</xsl:stylesheet>
