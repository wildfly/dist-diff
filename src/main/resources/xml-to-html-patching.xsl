<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml">
    <xsl:output method="html" indent="yes" version="4.0"/>

    <xsl:template match="/dist-diff2">
        <html>
            <head>
                <title>Dist-diff2 <xsl:value-of select="version"/> report</title>
                <script>
                    function toggleVisibility(id) {
                        var element = document.getElementById(id);
                        var button = document.getElementById(id + '-toggle');
                        if (element) {
                            if (element.style.display != 'none') {
                                element.style.display = 'none';
                                button.innerHTML = 'Show ' + button.name
                            } else {
                                element.style.display = 'inline';
                                button.innerHTML = 'Hide ' + button.name
                            }
                        }
                    }

                    function showLightbox(id) {
                        // close all lightboxes if ESC key is pressed
                        document.onkeypress = function (evt) {
                            if (evt.keyCode == 27) {
                                var boxes = document.getElementsByClassName("white_content");
                                var nodes = Array.prototype.slice.call(boxes, 0);
                                nodes.forEach(function (box) {
                                    closeLightbox(box.id)
                                });
                                document.getElementById('fade').style.display = 'none';
                            }

                        }
                        document.getElementById(id).style.display = 'block';
                        document.getElementById('fade').style.display = 'block';
                    }

                    function closeLightbox(id) {
                        document.getElementById(id).style.display = 'none';
                        document.getElementById('fade').style.display = 'none';
                    }

                </script>
                <style type="text/css">
                    table.gridtable {
                        font-family: verdana, arial, sans-serif;
                        font-size: 11px;
                        color: #333333;
                        border: 1px #666666;
                        border-collapse: collapse;
                    }

                    table.gridtable th {
                        padding: 8px;
                        border: 1px solid #666666;
                        background-color: #dedede;
                    }

                    table.gridtable td {
                        padding: 8px;
                        border: 1px solid #666666;
                        background-color: #ffffff;
                    }

                    tr.added td {
                        padding: 8px;
                        border: 1px solid #666666;
                        background-color: #9cd3f8;
                    }

                    tr.not_patched td {
                        padding: 8px;
                        border: 1px solid #666666;
                        background-color: #f80003;
                    }

                    tr.patched td {
                        padding: 8px;
                        border: 1px solid #666666;
                        background-color: #f8f6e7;
                    }

                    tr.patched_unnecessarily td {
                        padding: 8px;
                        border: 1px solid #666666;
                        background-color: #e800f8;
                    }

                    tr.removed td {
                        padding: 8px;
                        border: 1px solid #666666;
                        background-color: #f6c7c7;
                    }

                    tr.error td {
                        padding: 8px;
                        border: 1px solid #666666;
                        background-color: #ff0000;
                    }

                    tr.different td {
                        padding: 8px;
                        border: 1px solid #666666;
                        background-color: #feffae;
                    }

                    tr.different_permissions td {
                        padding: 8px;
                        border: 1px solid #666666;
                        background-color: #e7c177;
                    }

                    tr.patched_wrong td {
                        padding: 8px;
                        border: 1px solid #666666;
                        background-color: #ff8607;
                    }

                    tr.version td {
                        padding: 8px;
                        border: 1px solid #666666;
                        background-color: #00ffae;
                    }

                    tr.expected_differences td {
                        padding: 8px;
                        border: 1px solid #666666;
                        background-color: #dedede;
                    }

                    tr.build td {
                        padding: 8px;
                        border: 1px solid #666666;
                        background-color: #0099ae;
                    }

                    table.jarDiff tr td {
                        border-style: none;
                        padding: 0;
                        font-family: "Courier New", Courier, monospace;
                        font-size: 11px;
                        word-wrap: break-word;
                    }

                    .black_overlay {
                        display: none;
                        position: fixed;
                        top: 0%;
                        left: 0%;
                        width: 100%;
                        height: 100%;
                        background-color: black;
                        z-index: 1001;
                        -moz-opacity: 0.8;
                        opacity: .80;
                        filter: alpha(opacity=80);
                    }

                    .white_content {
                        display: none;
                        position: fixed;
                        top: 25%;
                        left: 25%;
                        width: 50%;
                        height: 50%;
                        padding: 16px;
                        border: 16px solid orange;
                        background-color: white;
                        z-index: 1002;
                        overflow: auto;
                    }

                </style>
            </head>
            <body>
                <div id="fade" class="black_overlay">&#160;</div>
                <img src="data:image/png;base64,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"  alt="dist-diff2 stylish header"/>
                <ul>
                    <li>
                        Distribution A:
                        <xsl:value-of select="folderA"/>
                    </li>
                    <li>
                        Distribution B:
                        <xsl:value-of select="folderB"/>
                    </li>
                    <xsl:if test="contains(serverDistribution, 'WildFly')">
                        <li>
                            Short class change summary is available&#160;<a href="class-summary.txt">here</a>.
                        </li>
                        <xsl:if test="addedModules/addedModule">
                            <li>
                                Added modules:
                                <ul>
                                    <xsl:apply-templates select="addedModules"/>
                                </ul>
                            </li>
                        </xsl:if>
                        <xsl:if test="removedModules/removedModule">
                            <li>
                                Removed modules:
                                <ul>
                                    <xsl:apply-templates select="removedModules"/>
                                </ul>
                            </li>
                        </xsl:if>
                    </xsl:if>
                </ul>
                <xsl:apply-templates select="errorMessages"/>
                <h3>
                    <a id="artifacts">Artifacts</a>
                </h3>
                <table class="gridtable" style="margin-bottom:10px">
                    <thead>
                        <tr>
                            <th>Added</th>
                            <th>Removed</th>
                            <th>Different</th>
                            <th>Same</th>
                            <th>Other</th>
                            <th>Total</th>
                        </tr>
                    </thead>
                    <tbody>
                        <xsl:apply-templates select="artifactsNumbers"/>
                    </tbody>
                </table>
                <table class="gridtable">
                    <thead>
                        <tr>
                            <th>Status</th>
                            <th>Name</th>
                            <th>Path in A</th>
                            <th>Path in B</th>
                            <th>Mime Type</th>
                            <th>Size</th>
                        </tr>
                    </thead>
                    <tbody>
                        <xsl:apply-templates select="artifacts"/>
                    </tbody>
                </table>
                <ul>
                    <li>
                        Dist-diff2 version:
                        <xsl:value-of select="version"/>
                        <br/>
                    </li>
                    <li>
                        Command line arguments:
                        <xsl:value-of select="commandLineArguments"/>
                    </li>
                    <li>
                        System hostname and architecture:
                        <xsl:value-of select="systemHostnameArchitecture"/>
                    </li>
                </ul>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="addedModule|removedModule">
        <li>
            <xsl:value-of select="text()"/>
        </li>
    </xsl:template>

    <xsl:template match="artifacts">
        <xsl:apply-templates select="*"/>
    </xsl:template>

    <xsl:template match="artifactsNumbers">
	<tr>
		<td><xsl:value-of select="./a/text()"/></td>
		<td><xsl:value-of select="./r/text()"/></td>
		<td><xsl:value-of select="./d/text()"/></td>
		<td><xsl:value-of select="./s/text()"/></td>
		<td><xsl:value-of select="./o/text()"/></td>
		<td><xsl:value-of select="./t/text()"/></td>
	</tr>
    </xsl:template>

    <xsl:template match="folder">
        <xsl:variable name="TR-CLASS">
            <xsl:choose>
                <xsl:when test="@status = 'ADDED'">added</xsl:when>
                <xsl:when test="@status = 'REMOVED'">removed</xsl:when>
                <xsl:when test="@status = 'DIFFERENT'">different</xsl:when>
                <xsl:when test="@status = 'BUILD'">build</xsl:when>
                <xsl:when test="@status = 'VERSION'">version</xsl:when>
                <xsl:when test="@status = 'ERROR'">error</xsl:when>
                <xsl:when test="@status = 'NOT_PATCHED'">not_patched</xsl:when>
                <xsl:when test="@status = 'PATCHED_UNNECESSARILY'">patched_unnecessarily</xsl:when>
                <xsl:when test="@status = 'PATCHED'">patched</xsl:when>
                <xsl:when test="@status = 'PATCHED_WRONG'">patched_wrong</xsl:when>
                <xsl:when test="@status = 'EXPECTED_DIFFERENCES'">expected_differences</xsl:when>
            </xsl:choose>
        </xsl:variable>

        <tr class="{$TR-CLASS}">
            <td>
                <xsl:if test="not(contains(@status,'SAME'))">
                    <xsl:value-of select="@status"/>
                </xsl:if>
            </td>
            <td>
                <span style="font-weight:bold;">
                    <xsl:value-of select="@name"/>
                </span>
            </td>
            <td>
                <xsl:value-of select="@pathA"/>
            </td>
            <td>
                <xsl:value-of select="@pathB"/>
            </td>
            <td></td>
            <td></td>
        </tr>
        <!-- if there is a permissionDiff show it -->
        <xsl:if test="not(string-length(permissionDiff)=0)">
            <tr class="{$TR-CLASS}">
                <td colspan="1"></td>
                <td colspan="5">
                    <xsl:apply-templates select="permissionDiff"/>
                </td>
            </tr>
        </xsl:if>

    </xsl:template>

    <xsl:template match="file|jar|class|archive">
        <xsl:variable name="TR-CLASS">
            <xsl:choose>
                <xsl:when test="@status = 'ADDED'">added</xsl:when>
                <xsl:when test="@status = 'REMOVED'">removed</xsl:when>
                <xsl:when test="@status = 'DIFFERENT'">different<xsl:if test="permissionDiff">_permissions</xsl:if></xsl:when>
                <xsl:when test="@status = 'BUILD'">build</xsl:when>
                <xsl:when test="@status = 'VERSION'">version</xsl:when>
                <xsl:when test="@status = 'ERROR'">error</xsl:when>
                <xsl:when test="@status = 'NOT_PATCHED'">not_patched</xsl:when>
                <xsl:when test="@status = 'PATCHED_UNNECESSARILY'">patched_unnecessarily</xsl:when>
                <xsl:when test="@status = 'PATCHED'">patched</xsl:when>
                <xsl:when test="@status = 'PATCHED_WRONG'">patched_wrong</xsl:when>
                <xsl:when test="@status = 'EXPECTED_DIFFERENCES'">expected_differences</xsl:when>
            </xsl:choose>
        </xsl:variable>

        <tr class="{$TR-CLASS}">
            <td>
                <xsl:if test="not(contains(@status,'SAME'))">
                    <xsl:value-of select="@status"/>
                </xsl:if>
            </td>
            <td>
                <xsl:value-of select="@name"/>
                <xsl:if test="contains(@status,'VERSION') or contains(@status,'BUILD')">
                    <div style="margin: 5px">--></div>
                    <div>
                        <xsl:value-of select="build-information-b/@full-name"/>
                    </div>
                </xsl:if>
            </td>
            <td>
                <xsl:value-of select="@pathA"/>
            </td>
            <td>
                <xsl:value-of select="@pathB"/>
            </td>
            <td>
                <xsl:value-of select="@mime-type"/>
            </td>
            <td>
                <xsl:value-of select="@size"/>
            </td>
        </tr>
        <xsl:if test="not(string-length(textDiff)=0) or jarDiff or permissionDiff">
            <tr class="{$TR-CLASS}">
                <td colspan="1"></td>
                <td colspan="5">

                    <!-- if there is a textDiff, let's show it -->
                    <xsl:if test="not(string-length(textDiff)=0)">
                        <xsl:apply-templates select="textDiff"/>
                    </xsl:if>

                    <!-- show the JAR diff if available -->
                    <xsl:apply-templates select="jarDiff"/>

                    <!-- show the permission diff if available -->
                    <xsl:apply-templates select="permissionDiff"/>
                </td>
            </tr>
        </xsl:if>

    </xsl:template>

    <xsl:template match="errorMessages">
        <xsl:if test="errorMessage">
            <h3>Additional error messages:</h3>
            <ul>
                <xsl:for-each select="errorMessage">
                    <li>
                        <xsl:value-of select="."/>
                    </li>
                </xsl:for-each>
            </ul>
        </xsl:if>
    </xsl:template>

    <xsl:template match="jarDiff">
        <div>
            <xsl:variable name="ELEMENT_ID" select="generate-id(.)"/>
            <div>
                <button name="JARDIFF" id="{$ELEMENT_ID}-toggle" onclick="toggleVisibility('{$ELEMENT_ID}');">
                    Show JARDIFF
                </button>
            </div>
            <div id="{$ELEMENT_ID}" style="display:none">

                <table class="jarDiff">


                    <xsl:if test="manifestDiff">
                        <tr>
                            <td>
                                <b>MANIFEST.MF diff:</b>
                            </td>
                            <td>
                                <xsl:value-of select="manifestDiff" disable-output-escaping="yes"/>
                            </td>
                        </tr>
                    </xsl:if>


                    <!-- added classes -->
                    <xsl:if test="addedClasses/addedClass">
                        <tr>
                            <td>
                                <b>Added classes:</b>
                            </td>
                        </tr>
                        <xsl:for-each select="addedClasses/addedClass">
                            <tr>
                                <td/>
                                <td>
                                    <xsl:value-of select="."/>
                                </td>
                            </tr>
                        </xsl:for-each>

                    </xsl:if>

                    <!-- removed classes -->
                    <xsl:if test="removedClasses/removedClass">
                        <tr>
                            <td>
                                <b>Removed classes:</b>
                            </td>
                        </tr>
                        <xsl:for-each select="removedClasses/removedClass">
                            <tr>
                                <td/>
                                <td>
                                    <xsl:value-of select="."/>
                                </td>
                            </tr>
                        </xsl:for-each>

                    </xsl:if>

                    <!-- class diffs -->
                    <xsl:if test="classDiffs/entry">

                        <xsl:for-each select="classDiffs/entry">
                            <tr>   <!-- empty line for better readability -->
                                <td>&#160;</td>
                                <!-- can't use HTML entities in XSLT - this is a &nbsp; -->
                            </tr>
                            <tr>
                                <td>
                                    <b>
                                        <xsl:value-of select="key"/>
                                    </b>
                                </td>
                            </tr>

                            <!-- class format change -->
                            <xsl:if test="value/originalClassFormatVersion != value/newClassFormatVersion">
                                <tr>
                                    <td>
                                        Class format version change:
                                    </td>
                                    <td>
                                        Class format version changed from
                                        <xsl:value-of select="value/originalClassFormatVersion"/> to
                                        <xsl:value-of select="value/newClassFormatVersion"/>
                                    </td>
                                </tr>
                            </xsl:if>

                            <!-- added methods -->
                            <xsl:if test="value/addedMethods/addedMethod">
                                <tr>
                                    <td>
                                        Added methods:
                                    </td>
                                </tr>
                                <xsl:for-each select="value/addedMethods/addedMethod">
                                    <tr>
                                        <td colspan="1"/>
                                        <td>
                                            <xsl:value-of select="."/>
                                        </td>
                                    </tr>
                                </xsl:for-each>

                            </xsl:if>

                            <!-- removed methods -->
                            <xsl:if test="value/removedMethods/removedMethod">
                                <tr>
                                    <td>
                                        Removed methods:
                                    </td>
                                </tr>
                                <xsl:for-each select="value/removedMethods/removedMethod">
                                    <tr>
                                        <td colspan="1"/>
                                        <td>
                                            <xsl:value-of select="."/>
                                        </td>
                                    </tr>
                                </xsl:for-each>

                            </xsl:if>

                            <!-- method modifiers changes -->
                            <xsl:if test="value/methodModifiersChanges/methodModifiersChange">
                                <tr>
                                    <td>
                                        Method modifiers changes:
                                    </td>
                                </tr>
                                <xsl:for-each select="value/methodModifiersChanges/methodModifiersChange">
                                    <tr>
                                        <td colspan="1"/>
                                        <td>
                                            Method <xsl:value-of select="method"/>: from
                                            <i>
                                                <xsl:value-of select="oldModifiers"/>
                                            </i>
                                            to
                                            <i>
                                                <xsl:value-of select="newModifiers"/>
                                            </i>
                                        </td>
                                    </tr>
                                </xsl:for-each>

                            </xsl:if>

                            <!-- added fields -->
                            <xsl:if test="value/addedFields/addedField">
                                <tr>
                                    <td>
                                        Added fields:
                                    </td>
                                </tr>

                                <xsl:for-each select="value/addedFields/addedField">
                                    <tr>
                                        <td colspan="1"/>
                                        <td>
                                            <xsl:value-of select="."/>
                                        </td>
                                    </tr>
                                </xsl:for-each>

                            </xsl:if>

                            <!-- removed fields -->
                            <xsl:if test="value/removedFields/removedField">
                                <tr>
                                    <td>
                                        Removed fields:
                                    </td>
                                </tr>
                                <xsl:for-each select="value/removedFields/removedField">
                                    <tr>
                                        <td colspan="1"/>
                                        <td>
                                            <xsl:value-of select="."/>
                                        </td>
                                    </tr>
                                </xsl:for-each>

                            </xsl:if>

                            <!-- field modifiers changes -->
                            <xsl:if test="value/fieldModifiersChanges/fieldModifiersChange">
                                <tr>
                                    <td>
                                        Field modifiers changes:
                                    </td>
                                </tr>
                                <xsl:for-each select="value/fieldModifiersChanges/fieldModifiersChange">
                                    <tr>
                                        <td colspan="1"/>
                                        <td>
                                            Field <xsl:value-of select="field"/>: from
                                            <i>
                                                <xsl:value-of select="oldModifiers"/>
                                            </i>
                                            to
                                            <i>
                                                <xsl:value-of select="newModifiers"/>
                                            </i>
                                        </td>
                                    </tr>
                                </xsl:for-each>

                            </xsl:if>

                            <!-- decompiled code -->
                            <xsl:if test="value/html_sourceCodeDiff">
                                <xsl:variable name="LIGHTBOX_ID"
                                              select="generate-id(value/html_sourceCodeDiff)"/>
                                <tr>
                                    <td>
                                        Decompiled source diff:
                                    </td>
                                    <td>
                                        <a href="javascript:void(0);"
                                           onclick="showLightbox('{$LIGHTBOX_ID}')">
                                            Show
                                            decompiled source
                                        </a>
                                        <div id="{$LIGHTBOX_ID}" class="white_content">
                                            <a href="javascript:void(0);"
                                               onclick="closeLightbox('{$LIGHTBOX_ID}')">Close
                                            </a>
                                            <xsl:value-of select="value/html_sourceCodeDiff"
                                                          disable-output-escaping="yes"/>
                                            <br/>
                                            <a href="javascript:void(0);"
                                               onclick="closeLightbox('{$LIGHTBOX_ID}')">Close
                                            </a>
                                        </div>
                                    </td>
                                </tr>


                            </xsl:if>

                        </xsl:for-each>

                    </xsl:if>

                </table>


            </div>
        </div>
    </xsl:template>

    <xsl:template match="permissionDiff">
        <div>
            <xsl:variable name="ELEMENT_ID" select="generate-id(.)"/>
            <div>
                <button name="PERMISSIONS" id="{$ELEMENT_ID}-toggle"
                        onclick="toggleVisibility('{$ELEMENT_ID}');">Show PERMISSIONS
                </button>
            </div>
            <div id="{$ELEMENT_ID}" style="display:none;">
                <span style="font-family:Consolas,Monaco,Lucida Console,Liberation Mono,DejaVu Sans Mono,Bitstream Vera Sans Mono,Courier New, monospace;">
                    <xsl:value-of select="." disable-output-escaping="yes"/>
                </span>
            </div>
        </div>
    </xsl:template>

    <xsl:template match="textDiff">
        <div>
            <xsl:variable name="ELEMENT_ID" select="generate-id(.)"/>
            <div>
                <button name="DIFF" id="{$ELEMENT_ID}-toggle"
                        onclick="toggleVisibility('{$ELEMENT_ID}');">Show DIFF
                </button>
            </div>
            <div id="{$ELEMENT_ID}" style="display:none">
                <xsl:value-of select="." disable-output-escaping="yes"/>
            </div>
        </div>
    </xsl:template>

</xsl:stylesheet>
