package provguard.enforcement;

/**
 * Thrown by EnforcementEngine to veto a dangerous sink call before it
 * executes. Deliberately unchecked: this is a policy decision propagating
 * up to whatever called the sink, not a recoverable I/O-style failure that
 * every caller should be forced to handle explicitly.
 *
 * Extends {@link SecurityException} (a bootstrap-native, java.lang type)
 * rather than a plain RuntimeException on purpose: it is both semantically
 * correct (this genuinely is a security decision denying an operation) and
 * practically important - code outside this package that wants to catch
 * "ProvGuard blocked something" without depending on this exact class can
 * catch the standard JDK type, which is guaranteed to be the exact same
 * Class object everywhere in the JVM regardless of which classloader ends
 * up loading this particular subclass (see docs/DESIGN_DECISIONS.md section
 * 4E for why that guarantee matters here).
 */
public final class SinkBlockedException extends SecurityException {
    public SinkBlockedException(String message) {
        super(message);
    }
}
