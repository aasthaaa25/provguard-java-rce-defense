package provguard.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

/**
 * Makes ProvGuard's own classes visible to the bootstrap classloader.
 *
 * WHY THIS EXISTS (this is a genuine, non-optional JVM constraint, not an
 * implementation convenience): ByteBuddy's inline {@code Advice} copies advice
 * bytecode directly into the target method. When the target is a JDK core class
 * like {@code java.lang.ProcessBuilder}, that class is loaded by the bootstrap
 * classloader (represented as {@code null}). The bootstrap classloader cannot
 * resolve classes that live only on the application/system classloader's
 * classpath — a parent classloader can never see classes loaded by a child.
 * Without this step, the woven ProcessBuilder class would throw
 * {@code NoClassDefFoundError} the first time it tried to call back into
 * {@code provguard.provenance.EventBuffer}.
 *
 * The fix, per {@link Instrumentation#appendToBootstrapClassLoaderSearch}, is to
 * explicitly add a JAR containing our classes to the bootstrap classloader's
 * search path. When running as a packaged agent jar this is trivial (append the
 * agent's own jar). When running under a test runner, classes usually sit as
 * loose {@code .class} files in a build output directory rather than a jar, so
 * this class zips that directory into a temporary jar on the fly first.
 */
final class BootstrapInjector {

    private BootstrapInjector() {
    }

    /** Only files under this package prefix are ever copied into the bootstrap-visible jar. */
    private static final String OWN_PACKAGE_PREFIX = "provguard/";

    static void ensureVisible(Instrumentation instrumentation, Class<?> markerClass) {
        try {
            File origin = new File(markerClass.getProtectionDomain().getCodeSource().getLocation().toURI());
            File filteredJar = origin.isDirectory() ? filterDirectoryToTempJar(origin) : filterJarToTempJar(origin);
            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(filteredJar));
        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException(
                    "Failed to inject ProvGuard classes into the bootstrap classloader search path", e);
        }
    }

    /**
     * Builds a temp jar containing only {@code provguard/**} entries.
     *
     * This filtering is essential, not cosmetic: when running as a packaged
     * (shaded) agent jar, {@code markerClass}'s origin is the WHOLE fat jar,
     * which also bundles ByteBuddy's own classes (and its dependencies).
     * Appending that entire jar to the bootstrap search path would cause
     * ByteBuddy's classes to be loadable from BOTH the application classloader
     * and the bootstrap classloader, producing two independent, incompatible
     * definitions of the same class name — the JVM detects this at link time
     * and throws {@code LinkageError: loader constraint violation}. Restricting
     * the injected jar to only our own package keeps every third-party class
     * (ByteBuddy included) loaded exactly once, by its normal classloader.
     */
    private static File filterDirectoryToTempJar(File classesDir) throws IOException {
        File tempJar = createTempJar();
        Path base = classesDir.toPath();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(tempJar));
             Stream<Path> walk = Files.walk(base)) {
            for (Path path : (Iterable<Path>) walk.filter(Files::isRegularFile)::iterator) {
                String entryName = base.relativize(path).toString().replace(File.separatorChar, '/');
                if (!entryName.startsWith(OWN_PACKAGE_PREFIX)) {
                    continue;
                }
                jos.putNextEntry(new JarEntry(entryName));
                Files.copy(path, jos);
                jos.closeEntry();
            }
        }
        return tempJar;
    }

    private static File filterJarToTempJar(File sourceJar) throws IOException {
        File tempJar = createTempJar();
        try (JarFile source = new JarFile(sourceJar);
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(tempJar))) {
            Enumeration<JarEntry> entries = source.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().startsWith(OWN_PACKAGE_PREFIX)) {
                    continue;
                }
                jos.putNextEntry(new JarEntry(entry.getName()));
                try (InputStream in = source.getInputStream(entry)) {
                    in.transferTo(jos);
                }
                jos.closeEntry();
            }
        }
        return tempJar;
    }

    private static File createTempJar() throws IOException {
        File tempJar = File.createTempFile("provguard-bootstrap-", ".jar");
        tempJar.deleteOnExit();
        return tempJar;
    }
}
