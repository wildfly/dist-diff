# Dist-diff2
Dist-diff2 compares two distributions of Java based applications. Prepares report about differences between provided distributions.
Results are reported as txt, html and xml files for easier and faster analysis.

Report contains:

 * Information about added files and folders
 * Information about removed files and folders
 * Information about different files and folders
 * Analysis of different files (compares XML files at DOM level, jar file at API level, ...)

### How to execute
Here is the list of arguments and options that can be passed to the tool. Some of them are further explained below.

```
  Options "-a (--folderA)" and "-b (--folderB)" are required

  DistDiff2 -- comparison tool for Java based applications

  --added VAL                          : Path to file containing a list of
                                         expected added files. The default value
                                         is 'expected-added-files.txt'
  --decompile-all                      : Decompile ALL classes (even those which
                                         changed the API). Must be used together
                                         with -d/--decompile (default: false)
  --modified VAL                       : Path to file containing a list of
                                         expected modified files. The default
                                         value is 'expected-modified-files.txt'
  --precise-exclusion-matching         : Use precise matching when comparing
                                         artifacts from report with files in
                                         exclusion list. With this option
                                         enabled, files won't be treated as
                                         included in exclusion list if any of
                                         their parent directory is already
                                         included in file exclusion list. In
                                         other words you need to specify each
                                         file explicitly to be excluded from the
                                         comparison despite it's parent
                                         directory. (default: false)
  --removed VAL                        : Path to file containing a list of
                                         expected removed files. The default
                                         value is 'expected-removed-files.txt'
  --xml-lenient-compare                : If set to true, then different ordering
                                         of the elements between relevant XML
                                         files will not be considered as a
                                         difference. (default: false)
  -C (--binary-comparison-instruction) : Performs also comparison of binary
                                         executable files. If two binary files
                                         differ, dist-diff will try to decompile
                                         those with 'objdump --disassemble' tool
                                         and show different parts in the report.
                                         In this case only differences in tables
                                         that are supposed to contain actual
                                         instructions are compared. NOTE: this
                                         feature is available only on Linux
                                         based machines with installed 'objdump'
                                         utility. Also note that by binary
                                         executable we mean only executable
                                         files and static and dynamic libraries,
                                         not archives or pictures. (default:
                                         false)
  -a (--folderA) DIR                   : Folder with distribution A
  -b (--folderB) DIR                   : Folder with distribution B
  -c (--binary-comparison)             : Performs also comparison of binary
                                         executable files. If two binary files
                                         differ, dist-diff will try to decompile
                                         those with 'objdump --all-headers
                                         --disassemble-all' tool and show
                                         different parts in the report. So there
                                         is made diff from the whole binary
                                         content. NOTE: this feature is
                                         available only on Linux based machines
                                         with installed 'objdump' utility. Also
                                         note that by binary executable we mean
                                         only executable files and static and
                                         dynamic libraries, not archives or
                                         pictures. (default: false)
  -d (--decompile)                     : Try to decompile classes from inspected
                                         JARs (it is done only for classes which
                                         didn't change the API). WARNING: this
                                         slows down execution quite heavily!
                                         (default: false)
  -f (--permissions)                   : Compare file permission attributes
                                         differences (default: false)
  -h (--improved-hashing)              : [DEPRECATED] Use the improved hashing
                                         function for directory comparison
                                         (ignores poms, manifests and
                                         timestamps). To be used along with -p;
                                         not used by productization anymore
                                         (default: false)
  -i (--ignore-same-items)             : Do not report same items (report only
                                         those which are changed somehow)
                                         (default: false)
  -o (--output) DIR                    : Output directory for reports (default:
                                         output)
  -p (--patching)                      : Enables support for patching mechanism,
                                         make sure that '-a' points
                                         to the freshly unzipped NEW version and
                                         '-b' points to the OLD version with a
                                         patch applied (which updated it to the
                                         NEW version) (default: false)
  -r (--rpm)                           : RPM support (default: false)
  -s (--from-sources)                  : Expect that one or both the
                                         distributions were built from sources
                                         rather than productized, therefore some
                                         additional MANIFEST.MF attributes will
                                         be expected to be different (default:
                                         false)
  -x (--xml-as-text)                   : Parse all xml files as text file.
                                         (default: false)

  DistDiff2 version: 1.0.2-SNAPSHOT
```




Example: `java -jar dist-diff2-jar-with-dependencies.jar -a 611/er3 -b 611/er3 -i`

### How to compile

    mvn clean install

Distribution is copied as `dist-diff2-$VERSION-jar-with-dependencies.jar` into your local Maven repository.

### How it works
 * Takes two basic parameters which represent folders of compared distributions
 * Calculates added and missing files
 * Prepares list of artifacts and executes concrete phases on this list
 * Calculates MD5 sum for all artifacts
 * Calls registered phases
 * Generates XML report
 * Executes XSLT on prepared XML report and generates other output formats

### How to implement a new extension
 * Extend `org.wildfly.qa.distdiff2.phase.ProcessPhase`
 * Register your extension in `org.wildfly.qa.distdiff2.DistDiff2Main#main` method

### Dist-diff2 with respect to the patching mechanism
 * Enable using the option `-p` or `--patching`
 * Make sure that
   * `-a` points to the freshly unzipped NEW version
   * `-b` points to the OLD version with a patch applied (which updated it to the NEW version)
   * [DEPRECATED] `-h` activates the improved hashing mechanism (excluding rebuilds of `jboss-as-*` modules which don't change anything).
     * This is kept just in case that this approach will be used again some time in the future.

### RPM support
 * Enable using the option `-r` or `--rpm`
 * With RPM support enabled, dist-diff2 tries to 'deversionize' jars, because in a RPM-installed distribution, version strings are stripped from the jars
   * eg. `jboss-remoting-3.2.19.GA.jar` becomes `jboss-remoting.jar`
 * With RPM support enabled, always make sure that
   * `-a` points to the ZIP distribution
   * `-b` points to the root of the RPM distribution (`/usr/share/jbossas`|`/opt/rh/eap7/root/usr/share/wildfly`)
 * About mapping from filenames in ZIP to filenames in RPM:
   * most of the time, dist-diff2 will be able to deduce the RPM filename automatically
   * but there are some cases where jars don't follow the standard naming rules. You can put such exceptions into the
     file `zip-to-rpm-filename-mapping.properties` in the current working directory from where you run `dist-diff2`.
     The format is `FILENAME_IN_ZIP=FILENAME_IN_RPM`, for example:
     * `jbossws-cxf-resources-4.2.4.Final-jboss720.jar=jbossws-cxf-resources-jboss720.jar`

### File Permissions changes
 * Add `-f` or `--permissions` flag.
 * Supports POSIX style permissions (rwx) and generic permissions on unsupported filesystems.

### Jar Decompile process
 * For Java class decompilation process there is used [third-party library - CFR](http://www.benf.org/other/cfr/) as a maven dependency.

## Releasing a new version
Please note that the prerequisite is to have read/write permission to the repo configured in `pom.xml` `scm` tag.

1. `mvn release:clean`
2. `mvn release:prepare` — if the process fails here, revert using `mvn release:rollback`
3. `mvn release:perform`
