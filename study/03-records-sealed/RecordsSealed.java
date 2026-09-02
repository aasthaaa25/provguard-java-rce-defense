/**
 * TOPIC: records, sealed interfaces, pattern-matching switch, var, text blocks.
 *
 * WHY IT MATTERS: sealed interfaces let the compiler PROVE a switch is
 * exhaustive - exactly what ProvGuard needs for its planned enforcement
 * Decision type (Allow/Log/Block), so a new decision kind can never be added
 * without every switch over it being forced to handle it.
 */
public class RecordsSealed {

    // A record: immutable data carrier, auto-generates equals/hashCode/toString.
    record ProvenanceSummary(String sinkType, int stackDepth) {}

    // Sealed interface: only these three implementations are permitted anywhere.
    sealed interface Decision permits Allow, Log, Block {}
    record Allow() implements Decision {}
    record Log(String reason) implements Decision {}
    record Block(String reason) implements Decision {}

    static String explain(Decision decision) {
        // Pattern-matching switch: exhaustive, no default needed, compiler-checked.
        return switch (decision) {
            case Allow a -> "allowed";
            case Log l -> "logged: " + l.reason();
            case Block b -> "BLOCKED: " + b.reason();
        };
    }

    public static void main(String[] args) {
        var summary = new ProvenanceSummary("PROCESS_EXECUTION", 3);
        System.out.println(summary); // record toString for free

        Decision[] decisions = { new Allow(), new Log("unusual depth"), new Block("known gadget chain") };
        for (Decision d : decisions) {
            System.out.println(explain(d));
        }

        String banner = """
                ProvGuard decision demo
                ------------------------
                sealed types make illegal states unrepresentable.""";
        System.out.println(banner);
    }
}
