package org.wildfly.qa.distdiff2.jardiff;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.sksamuel.diffpatch.DiffMatchPatch;

import org.apache.log4j.Logger;
import org.benf.cfr.reader.Main;
import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.artifacts.JarArtifact;
import org.wildfly.qa.distdiff2.configuration.DistDiffConfiguration;
import org.wildfly.qa.distdiff2.phase.ProcessPhase;
import org.wildfly.qa.distdiff2.results.Status;
import org.wildfly.qa.distdiff2.tools.HTMLTools;
import org.wildfly.qa.distdiff2.tools.Tools;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * Compares contents of JAR files
 * - compares classes on API level (not bytecode)
 * - lists changed/added/removed files
 *
 * @author Jan Martiska
 */
public class JarDiffPhase extends ProcessPhase {

    private static final Logger LOGGER = Logger.getLogger(JarDiffPhase.class.getName());


    @Override
    public void process() {
        Map<Tuple<String, String>, JarDiff> jarDiffs = new HashMap<>();
        for (Artifact artifact : results.getArtifacts()) {
            if (artifact instanceof JarArtifact &&
                    EnumSet.<Status>of(Status.DIFFERENT, Status.VERSION, Status.BUILD, Status.PATCHED_WRONG)
                            .contains(artifact.getStatus())) {
                LOGGER.trace("Processing diff of " + artifact.getPathA() + " against " + artifact.getPathB());
                JarArtifact jarArtifact = (JarArtifact) artifact;
                JarDiff diff = calculateDiff(jarArtifact);
                if ((diff != null) && !diff.isEmpty()) {
                    LOGGER.trace("Diff is not empty!");
                    jarDiffs.put(new Tuple<>(
                                    jarArtifact.getPathA()
                                            .replace(distDiffConfiguration.getFolderA().getAbsolutePath(), "")
                                            .substring(1),
                                    jarArtifact.getPathB()
                                            .replace(distDiffConfiguration.getFolderB().getAbsolutePath(), "")
                                            .substring(1)),
                            diff);
                    jarArtifact.setJarDiff(diff);
                    if (diff.isEmptyExceptChangesInManifest()) {
                        LOGGER.trace("JarDiff is empty except for changes in manifest.");
                        if (diff.manifestHasOnlyExpectedChangesIfAny()) {
                            LOGGER.trace(
                                    "Manifest has only expected/tolerated changes. Setting to EXPECTED_DIFFERENCES");
                            artifact.setStatus(Status.EXPECTED_DIFFERENCES);
                        } else {
                            LOGGER.trace("Manifest has unexpected changes.");
                        }
                    } else {
                        LOGGER.trace("There are changes in other places than manifest.");
                    }
                } else {
                    LOGGER.trace("Diff is empty!");
                }
            }
        }
        createClassSummaryReport(jarDiffs);
    }

    public void createClassSummaryReport(Map<Tuple<String, String>, JarDiff> jarDiffs) {
        File classSummaryFile = new File(distDiffConfiguration.getOutput(), "class-summary.txt");
        classSummaryFile.delete();
        // class summary report
        distDiffConfiguration.getOutput().mkdir();
        try (FileWriter writer = new FileWriter(classSummaryFile)) {
            writer.append("Class change summary between directories:\n");
            writer.append("--- ").append(distDiffConfiguration.getFolderA().toString()).append("\n");
            writer.append("--- ").append(distDiffConfiguration.getFolderB().toString()).append("\n\n");
            for (Map.Entry<Tuple<String, String>, JarDiff> jarDiffEntry : jarDiffs.entrySet()) {
                writer.append(jarDiffEntry.getKey().getX()).append(" against \n");
                writer.append(jarDiffEntry.getKey().getY()).append(" \n");
                writer.append(jarDiffEntry.getValue().toSimpleString());
                writer.append("\n");
            }
        } catch (IOException e) {
            LOGGER.warn("Cannot create class summary report", e);
        }
    }

    private JarDiff calculateDiff(JarArtifact artifact) {
        try (JarFile jarA = new JarFile(new File(artifact.getPathA()));
            JarFile jarB = new JarFile(new File(artifact.getPathB()))) {
            JarDiff jarDiff = new JarDiff();
            compareClassesFromJars(jarDiff, jarA, jarB, distDiffConfiguration);
            compareFilesFromJars(jarDiff, jarA, jarB);
            compareManifest(artifact, jarDiff);
            return jarDiff;
        } catch (IOException e) {
            LOGGER.warn("Cannot diff file " + artifact.getPathA(), e);
            return null;
        }
    }

    private void compareFilesFromJars(JarDiff jarDiff, JarFile jarA, JarFile jarB) {
        Set<String> filesA = getFilesFrom(jarA);
        Set<String> filesB = getFilesFrom(jarB);
        for (String fileInA : filesA) {
            if (!filesB.contains(fileInA)) {
                jarDiff.removedFile(fileInA);
            } else {
                // TODO diff the files.. did it change?
            }
        }
        for (String fileInB : filesB) {
            if (!filesA.contains(fileInB)) {
                jarDiff.addedFile(fileInB);
            }
        }
    }

    private DiffMatchPatch patch = new DiffMatchPatch();

    private String decompileClassUsingTheSameProcess(File containingJarPath, String className) {
        LOGGER.debug("decompiling class " + className + " from jar " + containingJarPath.getPath());
        String original = System.getProperty("java.class.path");
        System.setProperty("java.class.path", original + File.pathSeparator + containingJarPath.toString());
        ByteArrayOutputStream outGobblerBAOS = new ByteArrayOutputStream();
        PrintStream outGobbler = new PrintStream(outGobblerBAOS);
        PrintStream originalOut = System.out;
        System.setOut(outGobbler);
        try {
            Main.main(new String[] {className});
        } finally {
            System.setOut(originalOut);
            System.setProperty("java.class.path", original);
        }
        return outGobblerBAOS.toString();
    }

    private void compareClassesFromJars(JarDiff jarDiff, JarFile jarA, JarFile jarB,
                                        DistDiffConfiguration distDiffConfiguration) {
        Map<String, CtClass> classesA = getClassesFrom(jarA);
        Map<String, CtClass> classesB = getClassesFrom(jarB);
        for (Map.Entry<String, CtClass> classAEntry : classesA.entrySet()) {
            if (classesB.containsKey(classAEntry.getKey())) {
                // compare internals of these two classes
                ClassDiff classDiff = compareTwoClasses(classAEntry.getValue(),
                        classesB.get(classAEntry.getKey()));
                if (classDiff.isEmpty() || distDiffConfiguration.isDecompileAll()) {
                    if (distDiffConfiguration.isDecompile()) {
                    /* ignore classes with $bundle or $logger in their name
                       they are automatically generated, have no line information, therefore
                       the decompiled code is very different every time even though
                       nothing actually changed */
                        if (classAEntry.getKey().contains("$bundle") || classAEntry.getKey()
                                .contains("$logger")) {
                            continue;
                        }

                        // try decompilation because all other methods failed (there is no API difference)
                        String sourceCodeA = decompileClassUsingTheSameProcess(new File(jarA.getName()),
                                classAEntry.getKey());
                        String sourceCodeB = decompileClassUsingTheSameProcess(new File(jarB.getName()),
                                classAEntry.getKey());

                        if (sourceCodeA != null && sourceCodeB != null) {
                            sourceCodeA = removeGeneratedCommentFromDecompiledClassSource(sourceCodeA);
                            sourceCodeB = removeGeneratedCommentFromDecompiledClassSource(sourceCodeB);

                            LinkedList<DiffMatchPatch.Diff> diffs = patch.diff_main(sourceCodeA, sourceCodeB);
                            patch.diff_cleanupSemantic(diffs);

                            if (diffs.size() > 1) {
                                LOGGER.info(
                                        "Found non-empty diff (" + diffs.size() + " items) on class "
                                                + classAEntry
                                                .getKey() + " in jar: " + jarA.getName());
                                String html_sourceCodeDiff = patch.diff_prettyHtml(diffs);
                                html_sourceCodeDiff = html_sourceCodeDiff.replace("&para;",
                                        "");  // no idea why, but sometimes every <br> gets prepended with a paragraph character
                                html_sourceCodeDiff = HTMLTools
                                        .fillLinesWithChangesWithGreyColor(html_sourceCodeDiff);
                                html_sourceCodeDiff = "<pre>" + html_sourceCodeDiff + "</pre>";
                                classDiff.setHtml_sourceCodeDiff(html_sourceCodeDiff);
                                jarDiff.classDiff(classAEntry.getKey(), classDiff);
                            }
                        }
                    }
                } else {
                    jarDiff.classDiff(classAEntry.getKey(), classDiff);
                }
            } else {
                jarDiff.removedClass(classAEntry.getKey());
            }
        }
        for (Map.Entry<String, CtClass> classInB : classesB.entrySet()) {
            if (!classesA.containsKey(classInB.getKey())) {
                jarDiff.addedClass(classInB.getKey());
            }
        }
    }

    // the decompiler adds a comment to the beginning of the decompiled source, let's get rid of it
    private static String removeGeneratedCommentFromDecompiledClassSource(String input) {
        String[] split = input.split("\\*/");
        if (split.length > 1) {
            String[] actualCodeParts = new String[split.length - 1];
            System.arraycopy(split, 1, actualCodeParts, 0, split.length - 1);
            return Joiner.on("*/").join(actualCodeParts);
        } else {
            return split[0];
        }
    }

    private Set<String> getFilesFrom(JarFile jarFile) {
        return jarFile.stream()
                .map(JarEntry::getName)
                .filter(name -> !(name.endsWith(".class")))
                .collect(Collectors.toSet());
    }

    /**
     * Returns a map of classes in a JAR file.
     * The keys are class names (FQ), the values are classes' representations
     * Anonymous inner classes are ignored.
     */
    private Map<String, CtClass> getClassesFrom(JarFile jarFile) {
        Map<String, CtClass> result = new HashMap<>();
        ClassPool pool = new ClassPool();
        pool.appendSystemPath();

        // Iteration order seems to reflect the order in which files where added to the JAR, this order may differ for
        // JARs created on different OS platform or different Java version. Since we are using map of FQDN:class, then for
        // example with MultiRelease JARs, without deterministic order it may happen we would compare class from JAR root to
        // the one from META-INF/versions where they will obviously differ at least in class file version.
        // By sorting the iteration order we ensure we are comparing the same classes.
        // TODO given the above, with current implementation we are not supporting comparing all classes in case of MultiRelease
        //  JAR - we are ignoring potential multiple implementation of the same class and are only comparing the last class
        //  found, which given the natural order is the original one from JAR root and not the others from META-INF/versions.
        jarFile.stream()
                .sorted(Comparator.comparing(JarEntry::getName))
                .filter(jarEntry -> jarEntry.getName().endsWith(".class"))
                .forEach(jarEntry -> {
                    try (InputStream stream = jarFile.getInputStream(jarEntry)) {
                        CtClass clazz = pool.makeClass(stream);
                        result.put(clazz.getName(), clazz);
                    } catch (IOException e) {
                        LOGGER.error(e);
                    }
                });

        /*
          ignore anonymous inner classes
          we iterate through the result again, cannot do it in the previous cycle
          because JarFile.entries() doesn't guarantee sorting the files in a way that ensures
          that inner classes will come AFTER their enclosing classes, therefore it is not possible to
          get reference to the enclosing method if the enclosing class is not yet added to the class pool
         */
        // TODO - this whole part is probably irrelevant as 'clazz.getEnclosingBehavior' returns null always.
        Set<String> classNames = result.keySet();
        Iterator<String> iterator = classNames.iterator();
        while (iterator.hasNext()) {
            CtClass clazz = result.get(iterator.next());
            try {
                if (clazz.getEnclosingBehavior() != null) {
                    LOGGER.trace("Skipping anonymous inner class " + clazz.getName());
                    iterator.remove();
                }
            } catch (NullPointerException | NotFoundException e) {
                LOGGER.trace(
                        "Cannot determine if class is anonymous: " + clazz.getName() + ", because of: " + e
                                + ", presuming that YES");
                iterator.remove();
            }
        }
        return result;
    }

    // FIXME this expects particular behavior from CtMethod's equals method
    // it expects two methods with the same class name, method name, return type and argument list to be equal
    // and that modifiers don't make any difference in this regard (don't break the equality)
    // might break in a future javassist version...
    // we might need to implement a custom equality test for CtMethod
    private ClassDiff compareTwoClasses(final CtClass a, final CtClass b) {
        LOGGER.debug("Comparing two versions of class " + a.getName());
        ClassDiff result = new ClassDiff();

        // compare classes' modifiers
        int modifiersA = a.getModifiers();
        int modifiersB = b.getModifiers();
        if (modifiersA != modifiersB) {
            LOGGER.trace("Class modifiers changed from " + Modifier.toString(modifiersA) + " to " + Modifier
                    .toString(modifiersB));
            result.classModifiersChanged(modifiersA, modifiersB);
        }

        // magic numbers (class versions)
        int classVersionOriginal = a.getClassFile().getMajorVersion();
        int classVersionNew = b.getClassFile().getMajorVersion();
        result.setOriginalClassFormatVersion(classVersionOriginal);
        result.setNewClassFormatVersion(classVersionNew);
        LOGGER.trace("Class format: old=" + classVersionOriginal + ", new=" + classVersionNew);

        // compare the set of methods
        HashSet<CtMethod> methodsA = new HashSet<>(Arrays.asList(a.getDeclaredMethods()));
        HashSet<CtMethod> methodsB = new HashSet<>(Arrays.asList(b.getDeclaredMethods()));
        // find removed methods (present in A, but not present in B)
        for (CtMethod methodA : methodsA) {
            if (methodA.getName().contains("access$")) {
                // ignore synthetic accessors in inner classes
                LOGGER.trace("Skipping synthetic method " + methodA.getLongName());
                continue;
            }
            if (methodA.getName().contains("lambda$")) {
                // ignore methods generated from lambda expressions
                LOGGER.trace("Skipping lambda expression " + methodA.getLongName());
                continue;
            }
            if (!methodsB.contains(methodA)) {
                // FIXME here is the equals-behavior assumption
                LOGGER.trace("removed method : " + methodA);
                result.removedMethod(methodA);
            } else {
                CtMethod methodB = null;
                for (CtMethod method : methodsB) {
                    if (method.equals(methodA)) {
                        methodB = method;
                    }
                }
                if (methodB == null) {
                    throw new Error("This should not happen");
                }
                // compare visibility
                int modifiersMethodA = methodA.getModifiers();
                int modifiersMethodB = methodB.getModifiers();
                if (modifiersMethodA != modifiersMethodB) {
                    result.methodModifiersChanged(methodA, modifiersMethodA, modifiersMethodB);
                    LOGGER.trace("Method modifiers changed for " + methodA.getName());
                }
            }
        }
        // find added methods (present in B, but not present in A)
        for (CtMethod methodB : methodsB) {
            if (methodB.getName().contains("access$")) {
                // ignore synthetic accessors in inner classes
                continue;
            }
            if (methodB.getName().contains("lambda$")) {
                // ignore methods generated from lambda expressions
                LOGGER.trace("Skipping lambda expression " + methodB.getLongName());
                continue;
            }
            if (!methodsA.contains(methodB)) {
                result.addedMethod(methodB);
                LOGGER.trace("Added method: " + methodB);
            }
        }

        // compare fields
        // using just the field name for equality tests
        // store them in a map, where key is the name, value is the field
        Map<String, CtField> fieldsA = new HashMap<>();
        for (CtField field : a.getDeclaredFields()) {
            fieldsA.put(field.getName(), field);
        }
        Map<String, CtField> fieldsB = new HashMap<>();
        for (CtField field : b.getDeclaredFields()) {
            fieldsB.put(field.getName(), field);
        }
        for (CtField fieldInA : fieldsA.values()) {
            if (fieldsB.containsKey(fieldInA.getName())) {
                // compare visibility
                int modA = fieldInA.getModifiers();
                int modB = fieldsB.get(fieldInA.getName()).getModifiers();
                if (modA != modB) {
                    result.fieldModifierChanged(fieldInA, modA, modB);
                    LOGGER.trace("Field modifiers changed for field " + fieldInA.getName());
                }
            } else {
                result.removedField(fieldInA);
                LOGGER.trace("Removed field: " + fieldInA.getName());
            }
        }
        for (CtField fieldInB : fieldsB.values()) {
            if (!fieldsA.containsKey(fieldInB.getName())) {
                result.addedField(fieldInB);
                LOGGER.trace("Added field: " + fieldInB.getName());
            }
        }

        if (result.isEmpty()) {
            LOGGER.trace("No class diff found.");
        } else {
            LOGGER.trace("The class diff is not empty.");
        }
        return result;
    }

    private void compareManifest(JarArtifact artifact, JarDiff result) {
        LOGGER.debug("Comparing MANIFEST.MF of artifact " + artifact.getRelativePath());
        final DiffMatchPatch patch = new DiffMatchPatch();
        String fileA = artifact.getPathA();
        String fileB = artifact.getPathB();
        if (fileA != null && fileB != null) {
            try {
                Manifest manifestA = Tools.readManifestFromJar(fileA);
                Manifest manifestB = Tools.readManifestFromJar(fileB);
                artifact.setManifestA(manifestA);
                artifact.setManifestB(manifestB);

                String manifestAStr;
                String manifestBStr;
                if (manifestA != null && manifestB != null) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    manifestA.write(byteArrayOutputStream);
                    manifestAStr = byteArrayOutputStream.toString("utf-8");
                    byteArrayOutputStream = new ByteArrayOutputStream();
                    manifestB.write(byteArrayOutputStream);
                    manifestBStr = byteArrayOutputStream.toString("utf-8");
                    LinkedList<DiffMatchPatch.Diff> diffs = patch.diff_main(manifestAStr, manifestBStr);
                    patch.diff_cleanupSemantic(diffs);
                    String diff_prettyHtml = patch.diff_prettyHtml(diffs);
                    diff_prettyHtml = diff_prettyHtml.replaceAll("&para;", "");
                    result.setManifestDiff(diff_prettyHtml);

                    if (!diffs.isEmpty()) {
                        // some manifest attributes are tolerated (expected) to change and these changes should not be reported as an error
                        final List<String> toleratedAttributes = new ArrayList<>();
                        toleratedAttributes.add("Bnd-LastModified");
                        toleratedAttributes.add("Built-By");
                        toleratedAttributes.add("Created-By");
                        toleratedAttributes.add("Os-Arch");
                        toleratedAttributes.add("Os-Name");
                        toleratedAttributes.add("Build-Jdk");
                        toleratedAttributes.add("Os-Version");
                        toleratedAttributes.add("Build-Timestamp");

                        // if one/both of the distros are built from sources rather than productized, some more attributes are expected to be different
                        if (distDiffConfiguration.isFromSources()) {
                            toleratedAttributes.add("Java-Vendor");
                            toleratedAttributes.add("JBossAS-Release-Version");
                            toleratedAttributes.add("Java-Version");
                            toleratedAttributes.add("Specification-Version");
                            toleratedAttributes.add("Implementation-Version");
                            toleratedAttributes.add("Scm-Revision");
                            toleratedAttributes.add("Export-Package");
                            toleratedAttributes.add("Bundle-Version");
                        }

                        for (Object s : manifestA.getMainAttributes().keySet()) {
                            final String key = s.toString();
                            final String value = manifestA.getMainAttributes().getValue(key);
                            if (toleratedAttributes.contains(key)) {
                                LOGGER.trace(
                                        "Manifest entry " + key + "=" + value + " is tolerated, skipping");
                                continue;
                            }
                            String newValue = manifestB.getMainAttributes().getValue(key);
                            if (newValue == null) {
                                LOGGER.trace("Manifest entry " + key + " not found in B's manifest");
                                result.setManifestHasOnlyExpectedChanges(false);
                            } else if (!newValue.equals(value)) {
                                LOGGER.trace("Manifest entry " + key + "=" + value
                                        + " is different (changed to " + newValue
                                        + "! This is an unexpected change.");
                                result.setManifestHasOnlyExpectedChanges(false);
                            } else {
                                LOGGER.trace("Manifest entry " + key + "=" + value + " is equal.");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                artifact.setStatus(Status.ERROR);
            }
        } else {
            artifact.setStatus(Status.ERROR);
        }
    }


}
