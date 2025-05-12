package org.wildfly.qa.distdiff2.tools;

import java.util.LinkedList;

import com.sksamuel.diffpatch.DiffMatchPatch;

/**
 * This class extends {@link com.sksamuel.diffpatch.DiffMatchPatch} with our custom modifications.
 *
 * @author jstourac
 *
 */
public class DistDiffsDiffMatchPatch extends DiffMatchPatch {

    /**
     * Convert a {@link com.sksamuel.diffpatch.DiffMatchPatch.Diff} list into a pretty HTML report. This one shortens output to
     * show only differences with maximum of 4 lines before and 4 lines after the modified line part.
     *
     * @param diffs LinkedList of {@link com.sksamuel.diffpatch.DiffMatchPatch.Diff} objects.
     * @return HTML representation.
     */
    public String diff_prettyHtmlOnlyChanged(LinkedList<Diff> diffs) {
        StringBuilder html = new StringBuilder();
        for (Diff aDiff : diffs) {
            String text = aDiff.text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n",
                    "&para;<br>");
            switch (aDiff.operation) {
                case INSERT:
                    html.append("<ins style=\"background:#e6ffe6;\">").append(text).append("</ins>");
                    break;
                case DELETE:
                    html.append("<del style=\"background:#ffe6e6;\">").append(text).append("</del>");
                    break;
                case EQUAL:
                    // To avoid to print the whole content of compared files, we would like to show only something like
                    // unified diff. Thus in case that 'equal' part of diff contains more than 8 new-lines, we show only
                    // 4 first new-lines and 4 last new-lines. Thus we will have always 4 lines before and 4 lines after
                    // each diff at most.
                    String[] splitted = text.split("&para;<br>");
                    if (splitted.length > 8) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < 4; i++) {
                            sb.append(splitted[i]).append("&para;<br>");
                        }
                        sb.append("<br>...<br>");
                        for (int i = splitted.length - 4; i < splitted.length; i++) {
                            sb.append(splitted[i]).append("&para;<br>");
                        }
                        text = sb.toString();
                    }
                    html.append("<span>").append(text).append("</span>");
                    break;
                default:
                    // This should not happen - never
                    throw new IllegalStateException();
            }
        }
        return html.toString();
    }
}
