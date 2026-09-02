package provguard.enforcement;

/**
 * The three enforcement outcomes. Sealed so every switch over a Decision is
 * exhaustive and compiler-checked - adding a fourth outcome later forces
 * every call site to be updated, which is exactly the safety property
 * wanted for a security decision type.
 */
public sealed interface Decision {
    record Allow() implements Decision {}
    record Log(String reason) implements Decision {}
    record Block(String reason) implements Decision {}
}
