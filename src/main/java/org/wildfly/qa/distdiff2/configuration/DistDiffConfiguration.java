package org.wildfly.qa.distdiff2.configuration;

import java.io.File;

import org.wildfly.qa.distdiff2.excludelist.ExclusionPhase;
import org.kohsuke.args4j.Option;

/**
 * Configuration
 * <p>
 * Holds and reads configuration values from environment
 */
public class DistDiffConfiguration {

    @Option(name = "-a", aliases = {
            "--folderA"}, usage = "Folder with distribution A", required = true, metaVar = "DIR")
    private File folderA;

    @Option(name = "-b", aliases = {
            "--folderB"}, usage = "Folder with distribution B", required = true, metaVar = "DIR")
    private File folderB;

    @Option(name = "-o", aliases = {"--output"}, usage = "Output directory for reports", metaVar = "DIR")
    private File output = new File("output");

    @Option(name = "-i", aliases = {"--ignore-same-items"}, usage = "Do not report same items (report only those which are changed somehow)")
    private boolean ignoreSameItems = false;

    @Option(name = "-p", aliases = {
            "--patching"}, usage = "Enables support for the patching mechanism, make sure that '-a' points to the freshly unzipped NEW version and '-b' points to the OLD version with a patch applied (which updated it to the NEW version)", required = false, depends = {
            "-a", "-b"})
    private boolean patchingMechanismAware = false;

    @Deprecated
    @Option(name = "-h", aliases = {
            "--improved-hashing"}, usage = "[DEPRECATED] Use the improved hashing function for directory comparison (ignores poms, manifests and timestamps). To be used along with -p; not used by productization anymore", required = false, depends = {
            "-p"})
    private boolean improvedHashing = false;

    @Option(name = "-r", aliases = {"--rpm"}, usage = "RPM support", required = false)
    private boolean rpmAware = false;

    @Option(name = "-d", aliases = {
            "--decompile"}, usage = "Try to decompile classes from inspected JARs (it is done only for classes which didn't change the API). WARNING: this slows down execution quite heavily!", required = false)
    private boolean decompile = false;

    @Option(name = "--decompile-all",
            usage = "Decompile ALL classes (even those which changed the API). Must be used together with -d/--decompile",
            depends = "-d")
    private boolean decompileAll = false;

    @Option(name = "-s", aliases = "--from-sources", usage = "Expect that one or both the distributions were built from sources rather than productized, therefore some additional MANIFEST.MF attributes will be expected to be different", required = false)
    private boolean fromSources = false;

    @Option(name = "-c", aliases = "--binary-comparison", usage = "Performs also comparison of binary executable files. If two binary files differ, dist-diff will try to decompile those with 'objdump --all-headers --disassemble-all' tool and show different parts in the report. So there is made diff from the whole binary content. NOTE: this feature is available only on Linux based machines with installed 'objdump' utility. Also note that by binary executable we mean only executable files and static and dynamic libraries, not archives or pictures.", required = false)
    private boolean performFullBinaryComparison = false;

    @Option(name = "-C", aliases = "--binary-comparison-instruction", usage = "Performs also comparison of binary executable files. If two binary files differ, dist-diff will try to decompile those with 'objdump --disassemble' tool and show different parts in the report. In this case only differences in tables that are supposed to contain actual instructions are compared. NOTE: this feature is available only on Linux based machines with installed 'objdump' utility. Also note that by binary executable we mean only executable files and static and dynamic libraries, not archives or pictures.", required = false)
    private boolean performInstructionBinaryComparison = false;

    @Option(name = "--added", usage =
            "Path to file containing a list of expected added files. The default value is '"
                    + ExclusionPhase.DEFAULT_FILENAME_EXPECTED_ADDITIONS + "'", required = false)
    private String addedFilesFile = null;

    @Option(name = "-x", aliases = {"--xml-as-text"}, usage = "Parse all xml files as text file.", required = false)
    private boolean parseXmlAsText = false;

    @Option(name = "--xml-lenient-compare", usage = "If set to true, then different ordering of the elements between relevant XML files will not be considered as a difference.", required = false)
    private boolean compareXmlLeniently = false;

    @Option(name = "--removed", usage =
            "Path to file containing a list of expected removed files. The default value is '"
                    + ExclusionPhase.DEFAULT_FILENAME_EXPECTED_REMOVALS + "'", required = false)
    private String removedFilesFile = null;

    @Option(name = "--modified", usage =
            "Path to file containing a list of expected modified files. The default value is '"
                    + ExclusionPhase.DEFAULT_FILENAME_EXPECTED_MODIFICATIONS + "'", required = false)
    private String modifiedFilesFile = null;

    @Option(name = "-f", aliases = {"--permissions"}, usage = "Compare file permission attributes differences", required = false)
    private boolean permissionsDiff = false;


    @Option(name = "--precise-exclusion-matching", usage = "Use precise matching when comparing artifacts from report with files in exclusion list. With this option enabled, files won't be treated as included in exclusion list if any of their parent directory is already included in file exclusion list. In other words you need to specify each file explicitly to be excluded from the comparison despite it's parent directory.", required = false)
    private boolean preciseExclusionMatching = false;


    /**
     * Default constructor
     * Reads all configuration values from environment
     */
    public DistDiffConfiguration() {
    }

    public File getFolderA() {
        return folderA;
    }

    public void setFolderA(File folderA) {
        this.folderA = folderA;
    }

    public File getFolderB() {
        return folderB;
    }

    public void setFolderB(File folderB) {
        this.folderB = folderB;
    }

    public File getOutput() {
        return output;
    }

    public void setOutput(File output) {
        this.output = output;
    }

    public boolean isIgnoreSameItems() {
        return ignoreSameItems;
    }

    public void setIgnoreSameItems(boolean ignoreSameItems) {
        this.ignoreSameItems = ignoreSameItems;
    }

    public boolean isPatchingMechanismAware() {
        return patchingMechanismAware;
    }

    public void setPatchingMechanismAware(boolean patchingMechanismAware) {
        this.patchingMechanismAware = patchingMechanismAware;
    }

    public boolean isImprovedHashing() {
        return improvedHashing;
    }

    public void setImprovedHashing(boolean improvedHashing) {
        this.improvedHashing = improvedHashing;
    }

    public boolean isRpmAware() {
        return rpmAware;
    }

    public void setRpmAware(boolean rpmAware) {
        this.rpmAware = rpmAware;
    }

    public boolean isPermissionsDiff() {
        return permissionsDiff;
    }

    public void setPermissionsDiff(boolean permissionsDiff) {
        this.permissionsDiff = permissionsDiff;
    }

    public boolean isDecompile() {
        return decompile;
    }

    public void setDecompile(boolean decompile) {
        this.decompile = decompile;
    }

    public boolean isFromSources() {
        return fromSources;
    }

    public void setFromSources(boolean fromSources) {
        this.fromSources = fromSources;
    }

    public boolean isBinaryComparisonEnabled() {
        return performFullBinaryComparison || performInstructionBinaryComparison;
    }

    public boolean isFullBinaryComparisonEnabled() {
        return performFullBinaryComparison;
    }

    public void setFullBinaryComparison(boolean performFullBinaryComparison) {
        this.performFullBinaryComparison = performFullBinaryComparison;
    }

    public boolean isInstructionBinaryComparisonEnabled() {
        return performInstructionBinaryComparison;
    }

    public void setInstructionBinaryComparison(boolean performInstructionBinaryComparison) {
        this.performInstructionBinaryComparison = performInstructionBinaryComparison;
    }

    public boolean isParseXmlAsTextDisabled() {
        return !parseXmlAsText;
    }

    public boolean isCompareXmlLeniently() {
        return compareXmlLeniently;
    }

    public void setCompareXmlLeniently(boolean lenient) {
        this.compareXmlLeniently = lenient;
    }

    public String getAddedFilesFile() {
        return addedFilesFile;
    }

    public void setAddedFilesFile(String addedFilesFile) {
        this.addedFilesFile = addedFilesFile;
    }

    public String getRemovedFilesFile() {
        return removedFilesFile;
    }

    public void setRemovedFilesFile(String removedFilesFile) {
        this.removedFilesFile = removedFilesFile;
    }

    public boolean isDecompileAll() {
        return decompileAll;
    }

    public void setDecompileAll(boolean decompileAll) {
        this.decompileAll = decompileAll;
    }

    public String getModifiedFilesFile() {
        return modifiedFilesFile;
    }

    public void setModifiedFilesFile(String modifiedFilesFile) {
        this.modifiedFilesFile = modifiedFilesFile;
    }

    public boolean isPreciseExclusionMatching() {
        return preciseExclusionMatching;
    }

    public void setPreciseExclusionMatching(boolean preciseExclusionMatching) {
        this.preciseExclusionMatching = preciseExclusionMatching;
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "folderA=" + folderA +
                ", folderB=" + folderB +
                ", output=" + output +
                ", ignoreSameItems=" + ignoreSameItems +
                ", patchingMechanismAware=" + patchingMechanismAware +
                ", improvedHashing=" + improvedHashing +
                ", rpmAware=" + rpmAware +
                ", decompile=" + decompile +
                ", decompileAll=" + decompileAll +
                ", fromSources=" + fromSources +
                ", fullBinaryComparison='" + performFullBinaryComparison + '\'' +
                ", instructionBinaryComparison='" + performInstructionBinaryComparison + '\'' +
                ", addedFilesFile='" + addedFilesFile + '\'' +
                ", parseXmlAsText='" + parseXmlAsText + '\'' +
                ", compareXmlLeniently='" + compareXmlLeniently + '\'' +
                ", removedFilesFile='" + removedFilesFile + '\'' +
                ", modifiedFilesFile='" + modifiedFilesFile + '\'' +
                ", permissionsDiff=" + permissionsDiff + '\'' +
                ", preciseExclusionMatching=" + preciseExclusionMatching +
                '}';
    }
}
