package org.wildfly.qa.distdiff2.tools;

import java.io.InputStream;
import java.util.Properties;

/**
 * @author Jan Martiska
 */
public class DistDiff2Version {

    public static final String VERSION;

    static {
        String v;
        try {
            InputStream s = ClassLoader.getSystemResourceAsStream("version/dist-diff2-version.properties");
            Properties versionProps = new Properties();
            versionProps.load(s);
            v = versionProps.getProperty("version", "(unknown version)");
        } catch (Exception e) {
            v = "(unknown version)";
            e.printStackTrace();
        }
        VERSION = v;
    }

}
