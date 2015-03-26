/*
 * Copyright 2014 Higher Frequency Trading http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.bytes;

import java.nio.ByteOrder;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

abstract class CharSequenceAccess implements ReadAccess<CharSequence> {

    public static CharSequenceAccess charSequenceAccess(ByteOrder order) {
        return order == LITTLE_ENDIAN ?
                LittleEndianCharSequenceAccess.INSTANCE :
                BigEndianCharSequenceAccess.INSTANCE;
    }

    public static CharSequenceAccess nativeCharSequenceAccess() {
        return charSequenceAccess(ByteOrder.nativeOrder());
    }

    private static int ix(long offset) {
        return (int) (offset >> 1);
    }

    static long getLong(CharSequence input, long offset,
                        int char0Off, int char1Off, int char2Off, int char3Off) {
        int base = ix(offset);
        long char0 = input.charAt(base + char0Off);
        long char1 = input.charAt(base + char1Off);
        long char2 = input.charAt(base + char2Off);
        long char3 = input.charAt(base + char3Off);
        return char0 | (char1 << 16) | (char2 << 32) | (char3 << 48);
    }

    static long getUnsignedInt(CharSequence input, long offset,
                               int char0Off, int char1Off) {
        int base = ix(offset);
        long char0 = input.charAt(base + char0Off);
        long char1 = input.charAt(base + char1Off);
        return char0 | (char1 << 16);
    }

    private CharSequenceAccess() {}

    @Override
    public int readInt(CharSequence input, long offset) {
        return (int) readUnsignedInt(input, offset);
    }

    @Override
    public int readUnsignedShort(CharSequence input, long offset) {
        return input.charAt(ix(offset));
    }

    @Override
    public short readShort(CharSequence input, long offset) {
        return (short) input.charAt(ix(offset));
    }

    static int getUnsignedByte(CharSequence input, long offset, int shift) {
        return Primitives.unsignedByte(input.charAt(ix(offset)) >> shift);
    }

    @Override
    public byte readByte(CharSequence input, long offset) {
        return (byte) readUnsignedByte(input, offset);
    }

    static class LittleEndianCharSequenceAccess extends CharSequenceAccess {
        static final CharSequenceAccess INSTANCE = new LittleEndianCharSequenceAccess();

        private LittleEndianCharSequenceAccess() {}

        @Override
        public long readLong(CharSequence input, long offset) {
            return getLong(input, offset, 0, 1, 2, 3);
        }

        @Override
        public long readUnsignedInt(CharSequence input, long offset) {
            return getUnsignedInt(input, offset, 0, 1);
        }

        @Override
        public int readUnsignedByte(CharSequence input, long offset) {
            return getUnsignedByte(input, offset, ((int) offset & 1) << 3);
        }

        @Override
        public ByteOrder byteOrder(CharSequence input) {
            return LITTLE_ENDIAN;
        }
    }

    static class BigEndianCharSequenceAccess extends CharSequenceAccess {
        static final CharSequenceAccess INSTANCE = new BigEndianCharSequenceAccess();

        private BigEndianCharSequenceAccess() {}

        @Override
        public long readLong(CharSequence input, long offset) {
            return getLong(input, offset, 3, 2, 1, 0);
        }

        @Override
        public long readUnsignedInt(CharSequence input, long offset) {
            return getUnsignedInt(input, offset, 1, 0);
        }

        @Override
        public int readUnsignedByte(CharSequence input, long offset) {
            return getUnsignedByte(input, offset, (((int) offset & 1) ^ 1) << 3);
        }

        @Override
        public ByteOrder byteOrder(CharSequence input) {
            return BIG_ENDIAN;
        }
    }
}
