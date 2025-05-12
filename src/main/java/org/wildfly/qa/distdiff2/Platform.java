package org.wildfly.qa.distdiff2;

/**
 * Based on Platform.groovy from noe-core project but quite highly modified.
 *
 * @author Jan Stourac (jstourac@redhat.com)
 */
public class Platform {
    private static final int PLATFORM_I386 = 32;
    private static final int PLATFORM_X64 = 64;
    private static final int PLATFORM_SPARC64 = 66;

    private static String getOsName() {
        return System.getProperty("os.name");
    }

    private static String getOsArch() {
        return System.getProperty("os.arch");
    }

    private static int getSolarisPreferredArch() {
        return Integer.parseInt(System.getProperty("solaris.preferred.arch", "32"));
    }

    private static String getOsVersion() {
        return System.getProperty("os.version");
    }

    public static boolean isWindows() {
        return getOsName().matches(".*[Ww]indows.*");
    }

    public static boolean isLinux() {
        return getOsName().matches("[Ll]inux.*");
    }

    public static boolean isRHEL() {
        return isLinux();
    }

    public static boolean isSolaris() {
        return "SunOS".equals(getOsName());
    }

    public static boolean isHP() {
        return "HP-UX".equals(getOsName());
    }

    public static boolean isMac() {
        return getOsName().contains("Mac OS");
    }

    public static boolean isAix() {
        return "AIX".equals(getOsName());
    }

    public static boolean isIA64() {
        return "IA64N".equals(getOsArch());
    }

    public static boolean isX64() {
        return "amd64".equals(getOsArch()) || isIA64() || (isSolaris() && getSolarisPreferredArch() == PLATFORM_X64);
    }

    public static boolean isX86() {
        String osArch = getOsArch();

        return "x386".equals(osArch) || "x86".equals(osArch) || "i386".equals(osArch) || "i686".equals(osArch) ||
                (isSolaris() && getSolarisPreferredArch() == PLATFORM_I386);
    }

    public static boolean isSparc() {
        String osArch = getOsArch();

        return "sparc".equals(osArch) || "sparcv9".equals(osArch);
    }

    public static boolean isSparc64() {
        return (isSparc() && (getSolarisPreferredArch() == PLATFORM_SPARC64 || isX64()));
    }

    public static boolean isPpc64() {
        return "ppc64".equals(getOsArch());
    }

    public static boolean isRHEL4() {
        return isRHEL() && (getOsVersion().matches(".*EL[^5678][a-zA-Z]*"));
    }

    public static boolean isRHEL5() {
        return isRHEL() && (getOsVersion().matches(".*el5.*"));
    }

    public static boolean isRHEL6() {
        return isRHEL() && (getOsVersion().matches(".*el6.*"));
    }

    public static boolean isRHEL7() {
        return isRHEL() && (getOsVersion().matches(".*el7.*"));
    }

    public static boolean isSolaris11() {
        return isSolaris() && (getOsVersion().matches("5\\.11"));
    }

    public static boolean isSolaris10() {
        return isSolaris() && (getOsVersion().matches("5\\.10"));
    }

    public static boolean isSolaris9() {
        return isSolaris() && (getOsVersion().matches("5\\.9"));
    }

    public static boolean isHP11() {
        return isHP() && (getOsVersion().matches(".*11.*"));
    }

}
