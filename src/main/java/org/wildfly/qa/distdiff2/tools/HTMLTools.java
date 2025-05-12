package org.wildfly.qa.distdiff2.tools;

/**
 * @author Jan Martiska
 */
public class HTMLTools {

    /**
     * Takes in a HTML snippet. Alters lines which contain some sort of diff so that the whole line will have a grey background, so they will be better visible.
     * Of course, if the whole line is ADDED or REMOVED in the diff, it will be colored anyway - this only has actual effect on lines which have changes that don't span the whole line.
     * Used for better readability of class diffs in the HTML report.
     */
    public static String fillLinesWithChangesWithGreyColor(String htmlSnippet) {
        StringBuilder builder = new StringBuilder();
        String[] strings = htmlSnippet.split("<br ?/?>");
        for (String s : strings) {
            if (s.contains("background:#")) {
                builder.append("<span style=\"background:#8CC0F5\">").append(s).append("</span>")
                        .append("<br />");
            } else {
                builder.append(s).append("<br />");
            }
        }
        return builder.toString();
    }
}
