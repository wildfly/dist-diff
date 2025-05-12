package org.wildfly.qa.distdiff2.phases.binaryFilesDiffs;

import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.Platform;
import org.wildfly.qa.distdiff2.artifacts.FileArtifact;
import org.wildfly.qa.distdiff2.configuration.DistDiff2Context;
import org.wildfly.qa.distdiff2.execution.DistDiff2Execution;
import org.wildfly.qa.distdiff2.results.Status;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * This testsuite performs dist-diff operation and then check result for expected changes regarding to the compared binary
 * files.
 *
 * @author jstourac
 *
 */
public class BinaryFilesDiffsTestCase {

    private static final String PATH_A = "src/test/resources/binaryComparison/a";
    private static final String PATH_B = "src/test/resources/binaryComparison/b";
    private static final String EXPECTED_FULL_BINARY_DIFF = "<span>architecture: i386:x86-64, flags 0x00000150:<br>HAS_SYMS, DYNAMIC, D_PAGED" +
            "<br>start address 0x0000000000000520<br><br><br>...<br> filesz 0x0000000000000240 memsz 0x0000000000000248 flags rw-<br>" +
            " DYNAMIC off 0x0000000000000de0 vaddr 0x0000000000200de0 paddr 0x0000000000200de0 align 2**3<br> filesz 0x00000000000001e0" +
            " memsz 0x00000000000001e0 flags rw-<br> NOTE off 0x0000000000000<br></span><del style=\"background:#ffe6e6;\">19</del><ins style=\"background:#e6ffe6;\">00"
            + "</ins><span>0 vaddr 0x0000000000000</span><del style=\"background:#ffe6e6;\">19</del><ins style=\"background:#e6ffe6;\">00"
            + "</ins><span>0 paddr 0x0000000000000</span><del style=\"background:#ffe6e6;\">19</del><ins style=\"background:#e6ffe6;\">00"
            + "</ins><span>0 align 2**</span><del style=\"background:#ffe6e6;\">2</del><ins style=\"background:#e6ffe6;\">3</ins><span><br> "
            + "filesz 0x00000000000000</span><del style=\"background:#ffe6e6;\">24</del><ins style=\"background:#e6ffe6;\">00</ins><span> "
            + "memsz 0x00000000000000</span><del style=\"background:#ffe6e6;\">24</del><ins style=\"background:#e6ffe6;\">00</ins><span> "
            + "flags r--<br> STACK off 0x0000000000000000 vaddr 0x0000000000000000 paddr 0x0000000000000000 align 2**</span><del style="
            + "\"background:#ffe6e6;\">4</del><ins style=\"background:#e6ffe6;\">3</ins><span><br> filesz 0x0000000000000000 memsz "
            + "0x0000000000000000 flags rw-</span><del style=\"background:#ffe6e6;\"><br> RELRO off 0x0000000000000dc0 vaddr 0x0000000000200dc0 "
            + "paddr 0x0000000000200dc0 align 2**0<br> filesz 0x0000000000000240 memsz 0x0000000000000240 flags r--</del><span><br><br>"
            + "Dynamic Section:<br> NEEDED libpython3.5m.so.1.0<br><br>...<br><br>Sections:<br>Idx Name Size VMA LMA File off Algn<br> 0 "
            + "<br></span><del style=\"background:#ffe6e6;\">.note.gnu.build-id 00000024 0000000000000190 0000000000000190 00000190 2**2<br> "
            + "CONTENTS, ALLOC, LOAD, READONLY, DATA<br> 1 </del><span>.gnu.hash 00000038 00000000000001b8 00000000000001b8 000001b8 2**3<br> "
            + "CONTENTS, ALLOC, LOAD, READONLY, DATA<br> </span><del style=\"background:#ffe6e6;\">2</del><ins style=\"background:#e6ffe6;\">1"
            + "</ins><span> .dynsym 00000120 00000000000001f0 00000000000001f0 000001f0 2**3<br> CONTENTS, ALLOC, LOAD, READONLY, DATA<br> "
            + "</span><del style=\"background:#ffe6e6;\">3</del><ins style=\"background:#e6ffe6;\">2</ins><span> .dynstr 000000d6 0000000000000310 "
            + "0000000000000310 00000310 2**0<br> CONTENTS, ALLOC, LOAD, READONLY, DATA<br> </span><del style=\"background:#ffe6e6;\">4</del>"
            + "<ins style=\"background:#e6ffe6;\">3</ins><span> .gnu.version 00000018 00000000000003e6 00000000000003e6 000003e6 2**1<br> "
            + "CONTENTS, ALLOC, LOAD, READONLY, DATA<br> </span><del style=\"background:#ffe6e6;\">5</del><ins style=\"background:#e6ffe6;\">4"
            + "</ins><span> .gnu.version_r 00000020 0000000000000400 0000000000000400 00000400 2**3<br> CONTENTS, ALLOC, LOAD, READONLY, DATA<br> "
            + "</span><del style=\"background:#ffe6e6;\">6</del><ins style=\"background:#e6ffe6;\">5</ins><span> .rela.dyn 000000c0 "
            + "0000000000000420 0000000000000420 00000420 2**3<br> CONTENTS, ALLOC, LOAD, READONLY, DATA<br> </span><del style=\"background:#ffe6e6;\">"
            + "7</del><ins style=\"background:#e6ffe6;\">6</ins><span> .init 0000001a 00000000000004e0 00000000000004e0 000004e0 2**2<br> "
            + "CONTENTS, ALLOC, LOAD, READONLY, CODE<br> </span><del style=\"background:#ffe6e6;\">8</del><ins style=\"background:#e6ffe6;\">7"
            + "</ins><span> .plt 00000010 0000000000000500 0000000000000500 00000500 2**4<br> CONTENTS, ALLOC, LOAD, READONLY, CODE<br> </span>"
            + "<del style=\"background:#ffe6e6;\">9</del><ins style=\"background:#e6ffe6;\">8</ins><span> .plt.got 00000010 0000000000000510 "
            + "0000000000000510 00000510 2**3<br> CONTENTS, ALLOC, LOAD, READONLY, CODE<br> </span><del style=\"background:#ffe6e6;\">10</del>"
            + "<ins style=\"background:#e6ffe6;\"> 9</ins><span> .text 00000100 0000000000000520 0000000000000520 00000520 2**4<br> CONTENTS, "
            + "ALLOC, LOAD, READONLY, CODE<br> 1</span><del style=\"background:#ffe6e6;\">1</del><ins style=\"background:#e6ffe6;\">0</ins><span> "
            + ".fini 00000009 0000000000000620 0000000000000620 00000620 2**2<br> CONTENTS, ALLOC, LOAD, READONLY, CODE<br> 1</span><del "
            + "style=\"background:#ffe6e6;\">2</del><ins style=\"background:#e6ffe6;\">1</ins><span> .eh_frame 00000004 0000000000000630 0000000000000630 "
            + "00000630 2**3<br> CONTENTS, ALLOC, LOAD, READONLY, DATA<br> 1</span><del style=\"background:#ffe6e6;\">3</del>"
            + "<ins style=\"background:#e6ffe6;\">2</ins><span> .init_array 00000008 0000000000200dc0 0000000000200dc0 00000dc0 2**3<br> "
            + "CONTENTS, ALLOC, LOAD, DATA<br> 1</span><del style=\"background:#ffe6e6;\">4</del><ins style=\"background:#e6ffe6;\">3</ins><span> "
            + ".fini_array 00000008 0000000000200dc8 0000000000200dc8 00000dc8 2**3<br> CONTENTS, ALLOC, LOAD, DATA<br> 1</span><del "
            + "style=\"background:#ffe6e6;\">5</del><ins style=\"background:#e6ffe6;\">4</ins><span> .jcr 00000008 0000000000200dd0 0000000000200dd0 "
            + "00000dd0 2**3<br> CONTENTS, ALLOC, LOAD, DATA<br> 1</span><del style=\"background:#ffe6e6;\">6</del><ins style=\"background:#e6ffe6;\">5"
            + "</ins><span> .data.rel.ro 00000008 0000000000200dd8 0000000000200dd8 00000dd8 2**3<br> CONTENTS, ALLOC, LOAD, DATA<br> 1</span><del "
            + "style=\"background:#ffe6e6;\">7</del><ins style=\"background:#e6ffe6;\">6</ins><span> .dynamic 000001e0 0000000000200de0 0000000000200de0 "
            + "00000de0 2**3<br> CONTENTS, ALLOC, LOAD, DATA<br> 1</span><del style=\"background:#ffe6e6;\">8</del><ins style=\"background:#e6ffe6;\">7"
            + "</ins><span> .got 00000040 0000000000200fc0 0000000000200fc0 00000fc0 2**3<br> CONTENTS, ALLOC, LOAD, DATA<br> 1</span><del "
            + "style=\"background:#ffe6e6;\">9</del><ins style=\"background:#e6ffe6;\">8</ins><span> .bss 00000008 0000000000201000 0000000000201000 "
            + "00001000 2**0<br> ALLOC<br> </span><del style=\"background:#ffe6e6;\">20</del><ins style=\"background:#e6ffe6;\">19</ins><span> "
            + ".gnu_debuglink 00000018 0000000000000000 0000000000000000 00001000 2**2<br> CONTENTS, READONLY<br> 2</span><del "
            + "style=\"background:#ffe6e6;\">1</del><ins style=\"background:#e6ffe6;\">0</ins><span> .gnu_debugdata 000002cc 0000000000000000 "
            + "0000000000000000 00001018 2**0<br> CONTENTS, READONLY<br>SYMBOL TABLE:<br>no symbols<br><br><br><br></span><del "
            + "style=\"background:#ffe6e6;\">Disassembly of section .note.gnu.build-id:<br><br>0000000000000190 &lt;.note.gnu.build-id&gt;:<br> "
            + "190: 04 00 add $0x0,%al<br> 192: 00 00 add %al,(%rax)<br> 194: 14 00 adc $0x0,%al<br> 196: 00 00 add %al,(%rax)<br> 198: 03 00 add "
            + "(%rax),%eax<br> 19a: 00 00 add %al,(%rax)<br> 19c: 47 rex.RXB<br> 19d: 4e 55 rex.WRX push %rbp<br> 19f: 00 4e c9 add %cl,-0x37(%rsi)<br> "
            + "1a2: 28 90 ce 78 a2 79 sub %dl,0x79a278ce(%rax)<br> 1a8: 31 33 xor %esi,(%rbx)<br> 1aa: dd 3d 74 e0 b6 79 fnstsw 0x79b6e074(%rip) "
            + "# 79b6e224 &lt;_end@@Base+0x7996d21c&gt;<br> 1b0: 04 c3 add $0xc3,%al<br> 1b2: 69 .byte 0x69<br> 1b3: cf iret <br><br></del>"
            + "<span>Disassembly of section .gnu.hash:<br><br>00000000000001b8 &lt;.gnu.hash&gt;:<br> 1b8: 03 00 add (%rax),%eax<br><br>...<br><br>"
            + "Disassembly of section .init:<br><br>00000000000004e0 &lt;<br></span><del style=\"background:#ffe6e6;\">_</del><ins "
            + "style=\"background:#e6ffe6;\">.</ins><span>init</span><del style=\"background:#ffe6e6;\">@@Base</del><span>&gt;:<br> 4e0: 48 83 ec 08 "
            + "sub $0x8,%rsp<br> 4e4: 48 8b 05 f5 0a 20 00 mov 0x200af5(%rip),%rax # 200fe0 &lt;__gmon_start__&gt;<br> 4eb: 48 85 c0 test "
            + "%rax,%rax<br><br>...<br><br>Disassembly of section .plt:<br><br>0000000000000500 &lt;<br></span><del style=\"background:#ffe6e6;\">.plt</del>"
            + "<ins style=\"background:#e6ffe6;\">_init@@Base+0x20</ins><span>&gt;:<br> 500: ff 35 c2 0a 20 00 pushq 0x200ac2(%rip) # "
            + "200fc8 &lt;_fini@@Base+0x2009a8&gt;<br> 506: ff 25 c4 0a 20 00 jmpq *0x200ac4(%rip) # 200fd0 &lt;_fini@@Base+0x2009b0&gt;<br> "
            + "50c: 0f 1f 40 00 nopl 0x0(%rax)<br><br>...<br><br>Disassembly of section .fini:<br><br>0000000000000620 &lt;<br></span><del "
            + "style=\"background:#ffe6e6;\">_</del><ins style=\"background:#e6ffe6;\">.</ins><span>fini</span><del style=\"background:#ffe6e6;\">"
            + "@@Base</del><span>&gt;:<br> 620: 48 83 ec 08 sub $0x8,%rsp<br> 624: 48 83 c4 08 add $0x8,%rsp<br> 628: c3 retq <br><br>Disassembly of "
            + "section .eh_frame:<br><br>0000000000000630 &lt;</span><del style=\"background:#ffe6e6;\">.eh_frame</del><ins style=\"background:#e6ffe6;\">"
            + "_fini@@Base+0x10</ins><span>&gt;:<br> 630: 00 00 add %al,(%rax)<br> ...<br><br><br>...<br><br>Disassembly of section .bss:<br><br>"
            + "0000000000201000 &lt;<br></span><del style=\"background:#ffe6e6;\">__</del><ins style=\"background:#e6ffe6;\">.</ins><span>bss</span>"
            + "<del style=\"background:#ffe6e6;\">_start@@Base</del><span>&gt;:<br> ...<br><br>Disassembly of section .gnu_debuglink:<br><br>"
            + "0000000000000000 &lt;</span><del style=\"background:#ffe6e6;\">.gnu_debuglink</del><ins style=\"background:#e6ffe6;\">"
            + "__bss_start@@Base-0x201000</ins><span>&gt;:<br> 0: 6c insb (%dx),%es:(%rdi)<br> 1: 69 62 70 79 74 68 6f imul $0x6f687479,0x70(%rdx),%esp<br> "
            + "8: 6e outsb %ds:(%rsi),(%dx)<br><br>...<br> 2c4: 02 00 add (%rax),%al<br> 2c6: 00 00 add %al,(%rax)<br> 2c8: 00 04 59 add %al,(%rcx,%rbx,2)"
            + "<br> 2cb: 5a pop %rdx<br></span>";

    private static final String EXPECTED_INSTRUCTION_BINARY_DIFF = "<span><br><br>Disassembly of section " +
            ".init:<br><br>00000000000004e0 &lt;</span><del style=\"background:#ffe6e6;\">_</del><ins " +
            "style=\"background:#e6ffe6;\">.</ins><span>init</span><del style=\"background:#ffe6e6;" +
            "\">@@Base</del><span>&gt;:<br> 4e0: 48 83 ec 08 sub $0x8,%rsp<br> 4e4: 48 8b 05 f5 0a 20 00 mov 0x200af5" +
            "(%rip),%rax # 200fe0 &lt;__gmon_start__&gt;<br> 4eb: 48 85 c0 test %rax," +
            "%rax<br><br>...<br><br>Disassembly of section .plt:<br><br>0000000000000500 &lt;<br></span><del " +
            "style=\"background:#ffe6e6;\">.plt</del><ins style=\"background:#e6ffe6;" +
            "\">_init@@Base+0x20</ins><span>&gt;:<br> 500: ff 35 c2 0a 20 00 pushq 0x200ac2(%rip) # 200fc8 &lt;" +
            "_fini@@Base+0x2009a8&gt;<br> 506: ff 25 c4 0a 20 00 jmpq *0x200ac4(%rip) # 200fd0 &lt;" +
            "_fini@@Base+0x2009b0&gt;<br> 50c: 0f 1f 40 00 nopl 0x0(%rax)<br><br>...<br><br>Disassembly of section " +
            ".fini:<br><br>0000000000000620 &lt;<br></span><del style=\"background:#ffe6e6;\">_</del><ins " +
            "style=\"background:#e6ffe6;\">.</ins><span>fini</span><del style=\"background:#ffe6e6;" +
            "\">@@Base</del><span>&gt;:<br> 620: 48 83 ec 08 sub $0x8,%rsp<br> 624: 48 83 c4 08 add $0x8,%rsp<br> " +
            "628: c3 retq <br></span>";

    // This variable is set to true in case that current machine is capable to perform binary comparison. Otherwise it is se to
    // false.
    private boolean doWeHaveTools = false;

    private static final Logger LOGGER = Logger.getLogger(BinaryFilesDiffsTestCase.class);

    @Before
    public void performDistDiffRun() {
        if (!Platform.isLinux()) {
            LOGGER.warn("Binary comparison feature is available on the Unix-like machines with 'objdump' tool installed."
                    + " This system is not Linux thus skipping this phase completely.");
            return;
        }

        doWeHaveTools = true;
    }

    @Test
    public void noBinaryComparisonResultTest() throws SecurityException {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context context = builder
                .pathA(PATH_A)
                .pathB(PATH_B)
                .detectServerDistributionAndRegisterPhases()
                .build();

        new DistDiff2Execution(context).execute();

        FileArtifact binary = (FileArtifact)context.getResults().getArtifacts().get(0);

        Assert.assertEquals(Status.DIFFERENT, binary.getStatus());
        Assert.assertNull(binary.getTextDiff());
    }

    @Test
    public void fullBinaryComparisonResultTest() throws SecurityException {
        // Let's execute binary comparison tests on RHEL7+ only.
        // Probably due to the different versions of objdump utility between
        // RHEL versions, the generated binary differences are not exactly
        // same between RHEL versions. As we prepared expected diffs on RHEL7
        // let's execute these two tests only on such system or later.
        Assume.assumeFalse(Platform.isRHEL4());
        Assume.assumeFalse(Platform.isRHEL5());
        Assume.assumeFalse(Platform.isRHEL6());

        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context context = builder
                .pathA(PATH_A)
                .pathB(PATH_B)
                .fullBinaryComparison(true)
                .detectServerDistributionAndRegisterPhases()
                .build();

        new DistDiff2Execution(context).execute();

        FileArtifact binary = (FileArtifact) context.getResults().getArtifacts().get(0);

        Assert.assertEquals(Status.DIFFERENT, binary.getStatus());

        if (doWeHaveTools) {
            Assert.assertNotNull(binary.getTextDiff());
            Assert.assertEquals(EXPECTED_FULL_BINARY_DIFF, binary.getTextDiff().replaceAll("\\s+", " "));
        } else {
            Assert.assertNull(binary.getTextDiff());
        }
    }

    @Test
    public void instructionOnlyBinaryComparisonResultTest() throws SecurityException {
        // Let's execute binary comparison tests on RHEL7+ only.
        // Probably due to the different versions of objdump utility between
        // RHEL versions, the generated binary differences are not exactly
        // same between RHEL versions. As we prepared expected diffs on RHEL7
        // let's execute these two tests only on such system or later.
        Assume.assumeFalse(Platform.isRHEL4());
        Assume.assumeFalse(Platform.isRHEL5());
        Assume.assumeFalse(Platform.isRHEL6());

        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context context = builder
                .pathA(PATH_A)
                .pathB(PATH_B)
                .instructionBinaryComparison(true)
                .detectServerDistributionAndRegisterPhases()
                .build();

        new DistDiff2Execution(context).execute();

        FileArtifact binary = (FileArtifact) context.getResults().getArtifacts().get(0);

        Assert.assertEquals(Status.DIFFERENT, binary.getStatus());

        if (doWeHaveTools) {
            Assert.assertNotNull(binary.getTextDiff());
            Assert.assertEquals(EXPECTED_INSTRUCTION_BINARY_DIFF, binary.getTextDiff().replaceAll("\\s+", " "));
        } else {
            Assert.assertNull(binary.getTextDiff());
        }
    }

    @Test
    public void fullBinaryComparisonIdenticalFilesTest() throws SecurityException {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context context = builder
                .pathA(PATH_A)
                .pathB(PATH_A)
                .fullBinaryComparison(true)
                .detectServerDistributionAndRegisterPhases()
                .build();

        new DistDiff2Execution(context).execute();

        FileArtifact binary = (FileArtifact) context.getResults().getArtifacts().get(0);

        Assert.assertEquals(Status.SAME, binary.getStatus());
    }

    @Test
    public void instructionBinaryComparisonIdenticalFilesTest() throws SecurityException {
        DistDiff2Context.Builder builder = new DistDiff2Context.Builder();
        final DistDiff2Context context = builder
                .pathA(PATH_A)
                .pathB(PATH_A)
                .instructionBinaryComparison(true)
                .detectServerDistributionAndRegisterPhases()
                .build();

        new DistDiff2Execution(context).execute();

        FileArtifact binary = (FileArtifact) context.getResults().getArtifacts().get(0);

        Assert.assertEquals(Status.SAME, binary.getStatus());
    }
}
