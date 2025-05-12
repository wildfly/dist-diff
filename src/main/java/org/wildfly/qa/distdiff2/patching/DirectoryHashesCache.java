package org.wildfly.qa.distdiff2.patching;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.tools.Tools;

/**
 * Used for caching results from computation of directories' MD5 hashes, so we don't have to compute a hash multiple times
 * for each module (each file in that directory will require to know the hash).
 * This might look like a close-to-zero optimization, but it actually saves ~6 seconds on every run!
 * In the underlying map, the key is a file handle to a directory, the value is a numeric hash code computed from all files in the directory.
 * @author Jan Martiska
 */
public class DirectoryHashesCache {

    public DirectoryHashesCache() {
        moduleHashesCache = new HashMap<>();
    }

    private final Map<File, Integer> moduleHashesCache;
    private static final Logger LOGGER = Logger.getLogger(DirectoryHashesCache.class);

    public int getHash(File directory) throws Exception {
        LOGGER.debug("Getting hash for directory " + directory.getAbsolutePath());
        int result;
        if (moduleHashesCache.containsKey(directory)) {
            result = moduleHashesCache.get(directory);
            LOGGER.debug("Hash for directory " + directory.getAbsolutePath() + " is " + result);
        } else {
            result = Tools.calculateHashOfDirectory(directory);
            LOGGER.debug("Hash for directory " + directory.getAbsolutePath() + "computed as " + result);
            moduleHashesCache.put(directory, result);
        }
        return result;
    }

}
