/**
 * Processing Phases for Distribution Comparison
 *
 * <p>This package contains all phase implementations that collectively perform
 * the distribution comparison workflow. Each phase is responsible for a specific
 * aspect of comparison and updates artifact statuses based on its analysis.
 *
 * <h2>Phase Execution Order</h2>
 * <p>Phases must execute in a specific order due to dependencies:
 * <ol>
 *   <li>{@link org.wildfly.qa.distdiff2.phase.PatchingMechanismAwarenessPhase} - Handles patching-specific logic (optional)</li>
 *   <li>{@link org.wildfly.qa.distdiff2.phase.MD5SumsPhase} - Identifies binary differences via MD5 checksums</li>
 *   <li>{@link org.wildfly.qa.distdiff2.phase.XmlFilesComparePhase} - XML DOM comparison (optional)</li>
 *   <li>{@link org.wildfly.qa.distdiff2.phase.TextFilesDiffsPhase} - Generates text diffs and detects line break differences</li>
 *   <li>{@link org.wildfly.qa.distdiff2.phase.JarVersionComparePhase} - Matches version changes in JAR files</li>
 *   <li>{@link org.wildfly.qa.distdiff2.phase.JarDiffPhase} - Analyzes API differences in JAR files</li>
 *   <li>{@link org.wildfly.qa.distdiff2.phase.ModuleChangesSummaryPhase} - Summarizes module-level changes</li>
 *   <li>{@link org.wildfly.qa.distdiff2.phase.BinaryFilesDiffsPhase} - Binary decompilation diffs (optional)</li>
 *   <li>{@link org.wildfly.qa.distdiff2.phase.PermissionDiffPhase} - File permission comparison (optional)</li>
 *   <li>{@link org.wildfly.qa.distdiff2.phase.ShowOnlyChangedItemsPhase} - Filters unchanged items (optional)</li>
 *   <li>{@link org.wildfly.qa.distdiff2.phase.ExclusionPhase} - Removes excluded artifacts</li>
 *   <li>{@link org.wildfly.qa.distdiff2.phase.RPMDetailsPhase} - Adds RPM-specific information (optional)</li>
 *   <li>{@link org.wildfly.qa.distdiff2.phase.SortingPhase} - Sorts artifacts by priority</li>
 *   <li>{@link org.wildfly.qa.distdiff2.phase.ReportingPhase} - Generates XML, HTML, and TXT reports</li>
 * </ol>
 *
 * <h2>Status Flow Through Phases</h2>
 * <p>Artifact statuses progress through the pipeline as phases refine the analysis:
 * <pre>
 * Initial Comparison:
 *   Files only in A → REMOVED
 *   Files only in B → ADDED
 *   Files in both   → SAME
 *       ↓
 * MD5SumsPhase:
 *   SAME + MD5 differs → DIFFERENT
 *   PATCHED + MD5 differs → PATCHED_WRONG
 *   SAME (module.xml) + hash match → EXPECTED_DIFFERENCES (RPM mode)
 *       ↓
 * JarVersionComparePhase:
 *   REMOVED + ADDED (same jar, same version) → SAME or DIFFERENT
 *   REMOVED + ADDED (same jar, diff version) → VERSION
 *   REMOVED + ADDED (same jar, same version, diff build) → BUILD
 *       ↓
 * TextFilesDiffsPhase:
 *   DIFFERENT + only line breaks → DIFFERENT_LINE_BREAKS
 *   EXPECTED_DIFFERENCES (module.xml) + unexpected changes → DIFFERENT
 *   DIFFERENT (module.xml) + only expected changes → EXPECTED_DIFFERENCES
 *       ↓
 * Other phases refine statuses further...
 *       ↓
 * Final statuses: SAME, DIFFERENT, DIFFERENT_LINE_BREAKS, VERSION, BUILD,
 *                 EXPECTED_DIFFERENCES, PATCHED, PATCHED_WRONG, NOT_PATCHED, ERROR
 * </pre>
 *
 * <h2>Status Priority and Reporting</h2>
 * <p>Statuses have priority values that determine their order in reports:
 * <ol>
 *   <li>PATCHED_WRONG (120) - Highest priority, critical issues</li>
 *   <li>ERROR (100)</li>
 *   <li>NOT_PATCHED (60)</li>
 *   <li>ADDED (50)</li>
 *   <li>REMOVED (45)</li>
 *   <li>DIFFERENT (40)</li>
 *   <li>DIFFERENT_LINE_BREAKS (35)</li>
 *   <li>PATCHED_UNNECESSARILY (30)</li>
 *   <li>VERSION (20)</li>
 *   <li>BUILD (18)</li>
 *   <li>EXPECTED_DIFFERENCES (15)</li>
 *   <li>PATCHED (10)</li>
 *   <li>SAME (1) - Lowest priority</li>
 * </ol>
 *
 * <h2>Implementing a New Phase</h2>
 * <p>To add a new processing phase:
 * <ol>
 *   <li><b>Extend ProcessPhase</b>:
 *     <pre>
 *     public class MyCustomPhase extends ProcessPhase {
 *         private static final Logger LOGGER = Logger.getLogger(MyCustomPhase.class.getName());
 *
 *         {@literal @}Override
 *         public void process() {
 *             for (Artifact artifact : results.getArtifacts()) {
 *                 // Your processing logic here
 *             }
 *         }
 *     }
 *     </pre>
 *   </li>
 *   <li><b>Use Audit-Aware Status Changes</b>:
 *     <ul>
 *       <li>Always use {@code artifact.setStatus(newStatus, "MyCustomPhase", "reason")} with phase name and reason</li>
 *       <li>Add logging at decision points: {@code LOGGER.info("Artifact '" + artifact.getRelativePath() + "': reason")}</li>
 *       <li>Include relevant details in reasons (e.g., MD5 values, version numbers)</li>
 *     </ul>
 *   </li>
 *   <li><b>Document Thoroughly</b>:
 *     <ul>
 *       <li>Add comprehensive class-level Javadoc explaining:
 *         <ul>
 *           <li>Purpose of the phase</li>
 *           <li>Processing logic (step-by-step)</li>
 *           <li>Status transitions (with table if complex)</li>
 *           <li>Special cases and edge conditions</li>
 *           <li>Configuration impact</li>
 *           <li>Dependencies (which phases must run before/after)</li>
 *         </ul>
 *       </li>
 *       <li>Add method-level Javadoc for complex methods</li>
 *     </ul>
 *   </li>
 *   <li><b>Register the Phase</b>:
 *     <ul>
 *       <li>Add your phase to the appropriate server distribution configuration</li>
 *       <li>Ensure correct execution order based on dependencies</li>
 *     </ul>
 *   </li>
 *   <li><b>Test Thoroughly</b>:
 *     <ul>
 *       <li>Verify status history appears in XML reports</li>
 *       <li>Verify logging messages are clear and informative</li>
 *       <li>Test edge cases and error conditions</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h2>Best Practices</h2>
 * <ul>
 *   <li><b>Logging Levels</b>:
 *     <ul>
 *       <li>INFO: Major status transitions, significant decisions, match findings</li>
 *       <li>DEBUG: Detailed calculations, intermediate results</li>
 *       <li>WARN: Unexpected conditions, PATCHED_WRONG status</li>
 *       <li>ERROR: Error states, exceptions</li>
 *       <li>TRACE: Very detailed information (hash values, fine-grained comparisons)</li>
 *     </ul>
 *   </li>
 *   <li><b>Status Change Reasons</b>:
 *     <ul>
 *       <li>Be specific: Include actual values (MD5 hashes, version numbers, etc.)</li>
 *       <li>Be concise: One or two sentences maximum</li>
 *       <li>Be actionable: Help users understand what changed and why</li>
 *     </ul>
 *   </li>
 *   <li><b>Error Handling</b>:
 *     <ul>
 *       <li>Always catch exceptions and set status to ERROR with detailed reason</li>
 *       <li>Log errors with full stack traces</li>
 *       <li>Use context.handleError() for critical errors that should appear in report</li>
 *     </ul>
 *   </li>
 *   <li><b>Performance</b>:
 *     <ul>
 *       <li>Only process eligible artifacts (check status, file type, etc.)</li>
 *       <li>Avoid redundant calculations (reuse MD5 sums, etc.)</li>
 *       <li>Be mindful of memory usage with large distributions</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @see org.wildfly.qa.distdiff2.phase.ProcessPhase
 * @see org.wildfly.qa.distdiff2.results.Status
 * @see org.wildfly.qa.distdiff2.results.StatusChange
 * @see org.wildfly.qa.distdiff2.artifacts.Artifact
 */
package org.wildfly.qa.distdiff2.phase;
