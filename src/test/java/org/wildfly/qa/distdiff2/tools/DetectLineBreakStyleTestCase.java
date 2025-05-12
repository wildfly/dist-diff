package org.wildfly.qa.distdiff2.tools;

import org.junit.Assert;
import org.junit.Test;

/**
 * Ability to detect windows/unix style line endings in a text file
 * @author Jan Martiska
 */
public class DetectLineBreakStyleTestCase {

    @Test
    public void unixLineEndingTest() throws Exception {
        final String path = ClassLoader.getSystemResource("fileanalysis/linebreaks/XMLSchema.dtd.unix").getPath();
        Assert.assertEquals(LineBreakStyle.UNIX, Tools.detectLineBreakStyle(path));
    }

    @Test
    public void windowsLineEndingTest() throws Exception {
        final String path = ClassLoader.getSystemResource("fileanalysis/linebreaks/XMLSchema.dtd.windows").getPath();
        Assert.assertEquals(LineBreakStyle.WINDOWS, Tools.detectLineBreakStyle(path));
    }


}
