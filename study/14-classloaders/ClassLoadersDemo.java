/**
 * TOPIC: classloaders and parent-first delegation.
 *
 * WHY IT MATTERS: this is the exact mechanism behind three real bugs fixed
 * while building provguard.agent.BootstrapInjector - see
 * docs/DESIGN_DECISIONS.md section 4 for the full story of what went wrong
 * and why. This demo shows the delegation model in isolation, without any
 * of the ByteBuddy/instrumentation complexity that made those bugs hard to
 * diagnose.
 */
public class ClassLoadersDemo {

    static class Marker {
    }

    public static void main(String[] args) {
        // The three-tier delegation chain every class lookup climbs before
        // falling back to searching locally.
        ClassLoader systemLoader = ClassLoadersDemo.class.getClassLoader();
        ClassLoader platformLoader = systemLoader.getParent();
        ClassLoader bootstrapLoader = platformLoader.getParent(); // null - represented by the JVM itself

        System.out.println("This class's loader:      " + systemLoader);
        System.out.println("Its parent (platform):     " + platformLoader);
        System.out.println("Its parent (bootstrap):    " + bootstrapLoader + " (null means 'the JVM itself')");

        // java.lang.String is loaded by the bootstrap classloader - the root
        // of the delegation chain, above even the platform classloader.
        System.out.println("String's loader:            " + String.class.getClassLoader());

        // A class we wrote ourselves is loaded by the system/app classloader.
        System.out.println("Marker's loader:            " + Marker.class.getClassLoader());

        // THE KEY LESSON (see docs/DESIGN_DECISIONS.md): a PARENT classloader
        // can NEVER see classes loaded by a CHILD classloader. Bootstrap
        // cannot see Marker unless Marker's jar is explicitly appended to
        // bootstrap's own search path via Instrumentation.appendToBootstrap-
        // ClassLoaderSearch(...) - exactly what BootstrapInjector does for
        // real, in provguard.agent, so that Advice bytecode woven into a
        // bootstrap-loaded JDK class (java.lang.ProcessBuilder) can call back
        // into provguard.provenance.EventBuffer.
        System.out.println();
        System.out.println("This is exactly why provguard.agent.BootstrapInjector exists:");
        System.out.println("java.lang.ProcessBuilder is bootstrap-loaded and can never see");
        System.out.println("provguard.provenance.EventBuffer unless we explicitly inject it.");
    }
}
