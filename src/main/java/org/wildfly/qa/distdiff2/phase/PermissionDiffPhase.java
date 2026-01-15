package org.wildfly.qa.distdiff2.phase;

import org.apache.log4j.Logger;
import org.wildfly.qa.distdiff2.artifacts.Artifact;
import org.wildfly.qa.distdiff2.errors.ErrorEvent;
import org.wildfly.qa.distdiff2.results.Status;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;


/**
 * PermissionDiffPhase - File Permission Comparison Phase
 *
 * <h3>Purpose</h3>
 * This phase compares file permissions between distributions to detect permission changes.
 * It supports both POSIX permissions (rwx style) on Unix-like systems and generic permissions
 * on filesystems that don't support POSIX attributes.
 *
 * <h3>Processing Logic</h3>
 * <ol>
 *   <li><b>Filesystem Detection</b>: Determines if the filesystem supports POSIX permissions
 *     <ul>
 *       <li>POSIX filesystems: Use PosixFilePermission (rwxr-xr-x style)</li>
 *       <li>Non-POSIX filesystems: Use generic readable/writable/executable checks</li>
 *     </ul>
 *   </li>
 *   <li><b>Permission Comparison</b>: Compares permissions for files in both distributions
 *     <ul>
 *       <li>Read permission attributes from both files</li>
 *       <li>Compare permission sets</li>
 *       <li>Record differences if permissions don't match</li>
 *     </ul>
 *   </li>
 *   <li><b>Status Update</b>: Only updates status if artifact was previously SAME
 *     <ul>
 *       <li>Sets permissionDiffOnly flag to indicate only permissions changed</li>
 *       <li>Changes status from SAME to DIFFERENT</li>
 *       <li>Note: This relies on being executed as one of the last phases</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h3>Status Transitions</h3>
 * <table border="1">
 *   <caption>Status transitions based on permission analysis</caption>
 *   <tr><th>From Status</th><th>Condition</th><th>To Status</th></tr>
 *   <tr><td>SAME</td><td>Permissions differ</td><td>DIFFERENT (with permissionDiffOnly=true)</td></tr>
 *   <tr><td>Any other status</td><td>Permissions differ</td><td>No change (already marked different)</td></tr>
 * </table>
 *
 * <h3>Permission Types</h3>
 * <ul>
 *   <li><b>POSIX Permissions</b>: Standard Unix permissions (owner, group, others)
 *     <ul>
 *       <li>Read (r), Write (w), Execute (x) for each category</li>
 *       <li>Example: rwxr-xr-x (755)</li>
 *     </ul>
 *   </li>
 *   <li><b>Generic Permissions</b>: Basic file attributes on non-POSIX systems
 *     <ul>
 *       <li>Readable, Writable, Executable flags</li>
 *       <li>Used on Windows and other non-Unix filesystems</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>Configuration Impact</h3>
 * <ul>
 *   <li><code>-f / --permissions</code>: Must be enabled for this phase to run</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * <ul>
 *   <li>Should run LATE in the phase pipeline (after content comparison phases)</li>
 *   <li>Only modifies SAME artifacts, so other phases must run first</li>
 * </ul>
 *
 * <h3>Important Notes</h3>
 * <ul>
 *   <li>The status update logic assumes this phase runs near the end of the pipeline</li>
 *   <li>If run early, permission differences on already-DIFFERENT files won't be captured</li>
 *   <li>The permissionDiffOnly flag helps distinguish pure permission changes from content changes</li>
 * </ul>
 *
 * @see Status
 * @see ProcessPhase
 * @author Martin Schvarcbacher
 */
public final class PermissionDiffPhase extends ProcessPhase {
    private static final Logger LOGGER = Logger.getLogger(PermissionDiffPhase.class);

    @Override
    public void process() {
        for (Artifact artifact : results.getArtifacts()) {
            try {
                processArtifact(artifact);
            } catch (Exception e) {
                LOGGER.error(e.getStackTrace());
                context.handleError(new ErrorEvent("Error processing: ", artifact));
            }
        }
    }

    /**
     * Tries to determine if both target paths support POSIX file permissions
     *
     * @param artifact source artifact
     * @return POSIX permissions supported
     */
    private boolean isPosixFileSystem(Artifact artifact) {
        try {
            boolean supportA = false, supportB = false;
            if (artifact.getPathA() != null) {
                FileStore fileStoreA = Files.getFileStore(Paths.get(artifact.getPathA()));
                supportA = fileStoreA.supportsFileAttributeView(PosixFileAttributeView.class);
            }
            if (artifact.getPathB() != null) {
                FileStore fileStoreB = Files.getFileStore(Paths.get(artifact.getPathB()));
                supportB = fileStoreB.supportsFileAttributeView(PosixFileAttributeView.class);
            }
            return supportA && supportB;
        } catch (IOException ex) {
            LOGGER.error(ex.getStackTrace());
        }
        return false;
    }

    /**
     * Tries to determine if both target paths support ACL file permissions
     *
     * @param artifact source artifact
     * @return POSIX permissions supported
     */
    private boolean isAclFileSystem(Artifact artifact) {
        try {
            boolean supportA = false, supportB = false;
            if (artifact.getPathA() != null) {
                FileStore fileStoreA = Files.getFileStore(Paths.get(artifact.getPathA()));
                supportA = fileStoreA.supportsFileAttributeView(AclFileAttributeView.class);
            }
            if (artifact.getPathB() != null) {
                FileStore fileStoreB = Files.getFileStore(Paths.get(artifact.getPathB()));
                supportB = fileStoreB.supportsFileAttributeView(AclFileAttributeView.class);
            }
            return supportA && supportB;
        } catch (IOException ex) {
            LOGGER.error(ex.getStackTrace());
        }
        return false;
    }


    private void processArtifact(Artifact artifact) throws IOException {
        PermissionsRepresentation permissionsA;
        PermissionsRepresentation permissionsB;
        String lineSeparator = "<br/>";

        if (isPosixFileSystem(artifact)) {
            permissionsA = new PosixPermissions(artifact.getPathA());
            permissionsB = new PosixPermissions(artifact.getPathB());
        } else if (isAclFileSystem(artifact)) {
            permissionsA = new AccessControlListPermissions(artifact.getPathA());
            permissionsB = new AccessControlListPermissions(artifact.getPathB());
            lineSeparator = "<hr/>";
        } else {
            permissionsA = new GenericPermissions(artifact.getPathA());
            permissionsB = new GenericPermissions(artifact.getPathB());
        }

        if (!permissionsA.equals(permissionsB)) {
            LOGGER.info("Artifact '" + artifact.getRelativePath() + "': Permission differences detected (A=" + permissionsA.getPermissions() + ", B=" + permissionsB.getPermissions() + ")");
            artifact.setPermissionDiff("A: " + permissionsA.getPermissions() + lineSeparator +
                    "B: " + permissionsB.getPermissions());
            if (artifact.getStatus() == Status.SAME) {
                // WARNING: this set is quite tricky as it does not have to work in case when
                // PermissionDiffPhase is not executed as a last of the diffing phases!
                LOGGER.info("Artifact '" + artifact.getRelativePath() + "': Marking as DIFFERENT (permission-only difference)");
                artifact.setPermissionDiffOnly(true);
                artifact.setStatus(Status.DIFFERENT, this.getClass().getSimpleName(),
                    "Permissions differ (A: " + permissionsA.getPermissions() + ", B: " + permissionsB.getPermissions() + ")");
            }
        }
    }

    private interface PermissionsRepresentation {
        /**
         * Specifies how to treat permission comparison when one or both files are deleted
         * TRUE: files will be equal
         * FALSE: files will differ, resulting in Status.DIFFERENT
         */
        boolean DELETED_FILES_ARE_EQUAL = true;

        String getPermissions();

        boolean equals(PermissionsRepresentation other);
    }

    /**
     * POSIX permissions in format rwxrwxrwx
     */
    private static class PosixPermissions implements PermissionsRepresentation {
        private String permissions;

        PosixPermissions(String source) throws IOException {
            if (source == null) {
                return;
            }
            Path path = Paths.get(source);
            Set<PosixFilePermission> set;
            set = Files.getPosixFilePermissions(path);
            this.permissions = PosixFilePermissions.toString(set);
        }

        @Override
        public String getPermissions() {
            return permissions;
        }

        public boolean equals(PermissionsRepresentation other) {
            if (other == null) {
                return false;
            }
            if (this.getPermissions() == null || other.getPermissions() == null) {
                return DELETED_FILES_ARE_EQUAL;
            }
            return this.getPermissions().equals(other.getPermissions());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || !(obj instanceof PermissionsRepresentation)) {
                return false;
            }
            PermissionsRepresentation other = (PermissionsRepresentation) obj;
            return equals(other);
        }

        @Override
        public int hashCode() {
            return getPermissions() != null ? getPermissions().hashCode() : 0;
        }
    }

    /**
     * Generic file permissions in format: 'rwx' if operation is allowed or '---' otherwise
     */
    private static class GenericPermissions implements PermissionsRepresentation {
        private String permissions;

        GenericPermissions(String source) {
            if (source == null) {
                return;
            }
            Path path = Paths.get(source);
            StringBuilder sb = new StringBuilder();

            boolean isReadable = Files.isReadable(path);
            boolean isExecutable = Files.isExecutable(path);
            boolean isWritable = Files.isWritable(path);

            //create file permission description format
            sb.setLength(0);
            sb.append(isReadable ? "r" : "-");
            sb.append(isWritable ? "w" : "-");
            sb.append(isExecutable ? "x" : "-");
            this.permissions = sb.toString();
        }

        @Override
        public String getPermissions() {
            return permissions;
        }

        public boolean equals(PermissionsRepresentation other) {
            if (other == null) {
                return false;
            }
            if (this.getPermissions() == null || other.getPermissions() == null) {
                return DELETED_FILES_ARE_EQUAL;
            }
            return this.getPermissions().equals(other.getPermissions());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || !(obj instanceof GenericPermissions)) {
                return false;
            }
            PermissionsRepresentation other = (PermissionsRepresentation) obj;
            return equals(other);
        }

        @Override
        public int hashCode() {
            return getPermissions() != null ? getPermissions().hashCode() : 0;
        }
    }

    /**
     * Windows Access Control List file permissions
     */
    private static class AccessControlListPermissions implements PermissionsRepresentation {
        private String permissions;

        AccessControlListPermissions(String source) throws IOException {
            if (source == null) {
                return;
            }
            Path path = Paths.get(source);
            StringBuilder sb = new StringBuilder();
            List<String> permissionList = new ArrayList<>();

            AclFileAttributeView aclFileAttributes = Files.getFileAttributeView(path, AclFileAttributeView.class);
            for (AclEntry aclEntry : aclFileAttributes.getAcl()) {
                permissionList.add(aclEntry.principal() + ":<br/>" + aclEntryFormat(aclEntry));
            }
            Collections.sort(permissionList, String.CASE_INSENSITIVE_ORDER);
            for (String line : permissionList) {
                sb.append(line);
                sb.append("<br/>");
            }
            sb.delete(sb.length() - 5, sb.length());
            this.permissions = sb.toString();
        }

        private String aclEntryFormat(AclEntry entry) {
            Set<AclEntryPermission> permissions = entry.permissions();
            String[] nameField = new String[AclEntryPermission.values().length];
            Arrays.fill(nameField, "null");

            for (AclEntryPermission permission : permissions) {
                nameField[permission.ordinal()] = permission.name();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (String name : nameField) {
                sb.append(name);
                sb.append(";");
            }
            sb.delete(sb.length() - 1, sb.length());
            sb.append("]");
            return sb.toString();
        }

        @Override
        public String getPermissions() {
            return permissions;
        }

        public boolean equals(PermissionsRepresentation other) {
            if (other == null) {
                return false;
            }
            if (this.getPermissions() == null || other.getPermissions() == null) {
                return DELETED_FILES_ARE_EQUAL;
            }
            return this.getPermissions().equals(other.getPermissions());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || !(obj instanceof PermissionsRepresentation)) {
                return false;
            }
            PermissionsRepresentation other = (PermissionsRepresentation) obj;
            return equals(other);
        }

        @Override
        public int hashCode() {
            return getPermissions() != null ? getPermissions().hashCode() : 0;
        }
    }

}
