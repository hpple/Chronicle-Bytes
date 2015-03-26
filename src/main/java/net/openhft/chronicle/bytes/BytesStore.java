package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.ReferenceCounted;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import static java.lang.Math.min;

/**
 * A reference to some bytes with fixed extents.
 * Only offset access within the capacity is possible.
 */
public interface BytesStore<B extends BytesStore<B, Underlying>, Underlying>
        extends RandomDataInput<B, Access<Underlying>, Underlying>,
        RandomDataOutput<B, Access<Underlying>, Underlying>, ReferenceCounted {
    static BytesStore wrap(byte[] bytes) {
        return HeapBytesStore.wrap(ByteBuffer.wrap(bytes));
    }

    static BytesStore wrap(ByteBuffer bb) {
        return bb.isDirect()
                ? NativeBytesStore.wrap(bb)
                : HeapBytesStore.wrap(bb);
    }

    default Bytes<Underlying> bytes() {
        return bytes(UnderflowMode.PADDED);
    }

    default Bytes bytes(UnderflowMode underflowMode) {
        switch (underflowMode) {
            case BOUNDED:
                return new BytesStoreBytes(this);
            case ZERO_EXTEND:
            case PADDED:
                return new ZeroedBytes(this, underflowMode);
            default:
                throw new UnsupportedOperationException("Unknown known mode " + underflowMode);
        }
    }

    /**
     * @return The smallest position allowed in this buffer.
     */
    default long start() {
        return 0L;
    }

    /**
     * @return the actual capacity available before resizing.
     */
    default long realCapacity() {
        return capacity();
    }

    /**
     * @return The maximum limit you can set.
     */
    long capacity();

    /**
     * Perform a set of actions with a temporary bounds mode.
     */
    default BytesStore with(long position, long length, Consumer<Bytes> bytesConsumer) {
        if (position + length > capacity())
            throw new BufferUnderflowException();
        BytesStoreBytes bsb = new BytesStoreBytes(this);
        bsb.position(position);
        bsb.limit(position + length);
        bytesConsumer.accept(bsb);
        return this;
    }

    /**
     * Use this test to determine if an offset is considered safe.
     */
    default boolean inStore(long offset) {
        return start() <= offset && offset < safeLimit();
    }

    default long safeLimit() {
        return capacity();
    }

    void storeFence();

    void loadFence();


    default void copyTo(BytesStore store) {
        long copy = min(capacity(), store.capacity());
        Access.copy(access(), accessHandle(), accessOffset(start()),
                store.access(), store.accessHandle(), store.accessOffset(store.start()), copy);
    }

    Underlying underlyingObject();

    @Override
    default boolean isNative() {
        return underlyingObject() == null;
    }

    default B zeroOut(long start, long end) {
        if (start < start() || end > capacity() || end > start)
            throw new IllegalArgumentException();
        access().zeroOut(accessHandle(), accessOffset(start), end - start);
        return (B) this;
    }

    boolean compareAndSwapInt(long offset, int expected, int value);

    boolean compareAndSwapLong(long offset, long expected, long value);

    default int addAndGetInt(long offset, int adding) {
        return BytesUtil.getAndAddInt(this, offset, adding) + adding;
    }

    default int getAndAddInt(long offset, int adding) {
        return BytesUtil.getAndAddInt(this, offset, adding);
    }

    default long addAndGetLong(long offset, long adding) {
        return BytesUtil.getAndAddLong(this, offset, adding) + adding;
    }

    default long getAndAddLong(long offset, long adding) {
        return BytesUtil.getAndAddLong(this, offset, adding);
    }

    // this "needless" override is needed for better erasure while accessing raw Bytes/BytesStore
    @Override
    Access<Underlying> access();
}
