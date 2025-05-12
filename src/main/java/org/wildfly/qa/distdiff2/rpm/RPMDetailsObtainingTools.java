package org.wildfly.qa.distdiff2.rpm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.log4j.Logger;

/**
 * @author Jan Martiska
 */
public class RPMDetailsObtainingTools {

    private static final Logger LOGGER = Logger.getLogger(RPMDetailsObtainingTools.class.getName());

    public static String getFullPackageName(String filePath) {
        String[] cmd = new String[]{"rpm", "-qf", filePath};
        String[] cmdArchitecture = new String[]{"rpm", "-qf", "--qf", " %{ARCH}", filePath};
        try {

            String s = execCmd(cmd);
            String architecture = execCmd(cmdArchitecture);
            if (architecture != null) {
                architecture = architecture.trim();
            }
            if (s != null) {
                s = s.trim();
                if (architecture != null) {
                    if (s.contains(architecture)) {
                        return s + ".rpm";
                    } else {
                        return s + "." + architecture + ".rpm";
                    }
                }
            }
            LOGGER.debug("unable to get RPM package owning " + filePath + ", rpm -qf output is: " + s);
            return null;
        } catch (IOException e) {
            LOGGER.warn("unable to get RPM package owning " + filePath, e);
            return null;
        }
    }

    // info about all formattings tags for rpm --qf can be obtained via rpm --querytags
    public static String getShortPackageName(String filePath) {
        String[] cmd = new String[]{"rpm", "-qf", "--qf", " %{NAME}.%{ARCH}", filePath};
        try {

            String s = execCmd(cmd);
            if (s != null) {
                s = s.trim();
            }
            return s;
        } catch (IOException e) {
            LOGGER.warn("unable to get RPM package owning " + filePath, e);
            return null;
        }
    }


    /**
     * **************************************************
     * OPTION 2 : do a yum list for every package separately
     * this seems to take longer than needed..
     * **************************************************
     */
    private static Map<String, List<String>> packageVersionsCache = new HashMap<>();

    public static List<String> getAllAvailablePackageVersionsCallingYumSeparately(String packageShortName) {
        if (packageVersionsCache.containsKey(packageShortName)) {
            LOGGER.debug("List of available versions of this package had been computed earlier.");
            return packageVersionsCache.get(packageShortName);
        }
        String[] cmd = new String[]{"yum", "list", "available", "--showduplicates", "-q", packageShortName};
        try {
            String output = execCmd(cmd);
            if (output != null && !output.isEmpty()) {
                StringReader reader = new StringReader(output);
                BufferedReader bufferedReader = new BufferedReader(reader);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] parsed = parseYumListOutputLine(line);
                    if (parsed != null) {
                        String version;
                        for (String s : parsed) {
                            version = emit(s.trim());
                            if (version == null) {
                                continue;
                            }
                            if (!packageVersionsCache.containsKey(packageShortName)) {
                                packageVersionsCache.put(packageShortName, new ArrayList<String>());
                            }
                            packageVersionsCache.get(packageShortName).add(version);
                            LOGGER.debug("Found package version: package=" + packageShortName + ", version=" + version);
                        }
                    }
                }
                return packageVersionsCache.get(packageShortName);
            }
            LOGGER.error("Cannot get available versions of package " + packageShortName
                    + " because yum list didn't return any usable output");
            return null;
        } catch (IOException e) {
            LOGGER.warn("unable to get available versions of package " + packageShortName, e);
            return null;
        }
    }

   /* private static List<String> getAllAvailablePackageVersionsCallingYumSeparatelyRHEL6(String packageShortName) {
        String[] cmd = new String[] {"yum", "list", "available", "--showduplicates", "-q", packageShortName};
        try {
            String output = execCmd(cmd);
            if (output != null && !output.isEmpty()) {
                StringReader reader = new StringReader(output);
                BufferedReader bufferedReader = new BufferedReader(reader);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] parsed = parseYumListOutputLine(line);
                    if (parsed != null) {
                        String shortName = parsed[0];
                        String version = parsed[1];
                        if (shortName == null || version == null) {
                            continue;
                        }
                        if (shortName.isEmpty() || version.isEmpty()) {
                            LOGGER.warn("Error in parsing yum list output, found package=" + shortName
                                    + ", version=" + version);
                            continue;
                        }
                        if (packageVersionsCache.containsKey(shortName)) {
                            packageVersionsCache.get(shortName).add(version);
                        } else {
                            packageVersionsCache.put(shortName, new ArrayList<String>());
                            packageVersionsCache.get(shortName).add(version);
                        }
                        LOGGER.debug("Found package version: package=" + shortName + ", version=" + version);


                    }
                }
                return packageVersionsCache.get(packageShortName);
            }
            LOGGER.error("Cannot get available versions of package " + packageShortName
                    + " because yum list didn't return any usable output");
            return null;
        } catch (IOException e) {
            LOGGER.warn("unable to get available versions of package " + packageShortName, e);
            return null;
        }
    }*/

    private static String emit(String string) {
        if (string.matches("[0-9].*")) {
            LOGGER.trace("String " + string + " looks like a version");
            return string;
        } else {
            LOGGER.trace("String " + string + " doesn't look like a version");
            return null;
        }
    }

    private static String[] parseYumListOutputLine(String line) {
        if (line.contains("Available")) {
            return null;
        }
        if (line.contains("Loaded plugins")) {
            return null;
        }
        if (line.contains("This system")) {
            return null;
        }
        String[] split = line.split("\\s+");
        LOGGER.trace("Yum output line " + line + ", split to " + split.length + " parts");
        return split;
    }


    public static String execCmd(String[] cmd) throws java.io.IOException {
        Scanner s = null;
        try {
            LOGGER.trace("Executing command: " + Arrays.toString(cmd));
            Process p = Runtime.getRuntime().exec(cmd);
            int ret = p.waitFor();
            LOGGER.trace("Command returned: " + ret);
            if (ret != 0) {
                if (LOGGER.isTraceEnabled()) {
                    printStream(p.getErrorStream());
                    printStream(p.getInputStream());
                }
                return null;
            }
            s = new Scanner(p.getInputStream());
            s.useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        } catch (InterruptedException e) {
            LOGGER.error(e);
            return null;
        } finally {
            if (s != null) {
                s.close();
            }
        }
    }

    private static void printStream(InputStream stream) throws IOException {
        InputStreamReader reader = new InputStreamReader(stream);
        BufferedReader br = new BufferedReader(reader);
        String line = br.readLine();
        if (line != null && !line.isEmpty()) {
            LOGGER.trace("Process output: ");
            do {
                if (!line.isEmpty()) {
                    LOGGER.trace(line);
                }
                line = br.readLine();
            } while (line != null);
        }

    }
}
