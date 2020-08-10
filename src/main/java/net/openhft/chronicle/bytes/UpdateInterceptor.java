package net.openhft.chronicle.bytes;

/**
 * Represents an operation that accepts a single input argument then modifies that same instance.
 */
@FunctionalInterface
public interface UpdateInterceptor {

    /**
     * modifies {@code t} with changed data
     *
     * @param methodName the name of the method
     * @param t          the input argument
     */
    void update(String methodName, Object t);

}