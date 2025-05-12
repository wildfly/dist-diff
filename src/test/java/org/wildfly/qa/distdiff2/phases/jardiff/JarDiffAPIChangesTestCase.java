package org.wildfly.qa.distdiff2.phases.jardiff;

import org.wildfly.qa.distdiff2.artifacts.JarArtifact;
import org.wildfly.qa.distdiff2.configuration.DistDiff2Context;
import org.wildfly.qa.distdiff2.execution.DistDiff2Execution;
import org.wildfly.qa.distdiff2.jardiff.ClassDiff;
import org.wildfly.qa.distdiff2.jardiff.JarDiffPhase;
import org.wildfly.qa.distdiff2.phase.MD5SumsPhase;
import org.wildfly.qa.distdiff2.results.Results;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * @author Jan Martiska
 */
public class JarDiffAPIChangesTestCase {

    private static DistDiff2Context ctx;
    private static Results results;
    private static JarArtifact theJar;

    @BeforeClass
    public static void prepareResults() {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        ctx = builder
                .pathA("src/test/resources/jardiff/a")
                .pathB("src/test/resources/jardiff/b")
                .processPhase(MD5SumsPhase.class)
                .processPhase(JarDiffPhase.class)
                .build();
        DistDiff2Execution execution = new DistDiff2Execution(ctx);
        execution.execute();
        results = ctx.getResults();
        theJar = (JarArtifact)results.findArtifactBySimpleName("thejar.jar");
        Assert.assertNotNull(theJar);
    }

    @Test
    public void addedClass() {
        Assert.assertTrue(theJar.getJarDiff().getAddedClasses().contains("AddedClass"));
    }

    @Test
    public void removedClass() {
        Assert.assertTrue(theJar.getJarDiff().getRemovedClasses().contains("RemovedClass"));
    }

    @Test
    public void addedMethod() throws NotFoundException {
        final ClassDiff classDiff = theJar.getJarDiff().getClassDiffs().get("ClassWithChanges");
        final CtMethod method = classDiff.getAddedMethods().toArray(new CtMethod[1])[0];
        Assert.assertEquals("addedMethod", method.getName());
        Assert.assertEquals(CtClass.voidType, method.getReturnType());
    }

    @Test
    public void removedMethod() throws NotFoundException {
        final ClassDiff classDiff = theJar.getJarDiff().getClassDiffs().get("ClassWithChanges");
        final CtMethod method = classDiff.getRemovedMethods().toArray(new CtMethod[1])[0];
        Assert.assertEquals("removedMethod", method.getName());
        Assert.assertEquals(CtClass.voidType, method.getReturnType());
    }
}
