package com.mojin.qidon.Keystore;

import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FuckYou {
    private static final byte[] FUCKER = new byte[8];
    private static final char[] SUPER_FUCKER = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    public static boolean isFucking(byte[] fuckMother, int[] fuckFather) {
        for (int i = 0; 32 > i; i++) {
            if ((fuckMother[i] & 0xFF) != fuckFather[i]) {
                return false;
            }
        }
        return true;
    }

    public static byte[][] fuckYou(String fuckAll) {
        String fucker = motherFucker(fuckAll);
        if (fucker == null || fucker.isEmpty()) {
            fucker = everyoneFucker(fuckAll);
        }
        RandomAccessFile fuck = null;
        try {
            fuck = new RandomAccessFile(fucker, "r");
            fuck.seek(fuck.length() - 6);
            fuck.read(FUCKER, 0, 4);
            int fucking = FUCKER[0] & 0xFF;
            fucking |= (FUCKER[1] & 0xFF) << 8;
            fucking |= (FUCKER[2] & 0xFF) << 16;
            fucking |= (FUCKER[3] & 0xFF) << 24;
            fuck.seek(fucking - 24);
            fuck.read(FUCKER);
            int fucked = FUCKER[4] & 0xFF;
            fucked |= (FUCKER[5] & 0xFF) << 8;
            fucked |= (FUCKER[6] & 0xFF) << 16;
            fucked |= (FUCKER[7] & 0xFF) << 24 << 32;
            fucked |= FUCKER[0] & 0xFF;
            fucked |= (FUCKER[1] & 0xFF) << 8;
            fucked |= (FUCKER[2] & 0xFF) << 16;
            fucked |= (FUCKER[3] & 0xFF) << 24;
            fucked &= 0xFFFFFFFF;
            ByteBuffer fuckYou = ByteBuffer.allocate(fucked);
            fuckYou.order(ByteOrder.LITTLE_ENDIAN);
            fuck.seek(fucking - fucked);
            fuck.readFully(fuckYou.array());
            byte[] fuckMother = fuckEveryone(fuckYou, 0x7109871a);
            byte[] fuckFather = fuckEveryone(fuckYou, 0xf05368c0);
            return new byte[][] {fuckMother, fuckFather};
        } catch (IOException fucked) {
            fucked.printStackTrace();
            return null;
        } finally {
            if (fuck != null) {
                try {
                    fuck.close();
                } catch (IOException fucked) {
                }
            }
        }
    }

    private static byte[] fuckEveryone(ByteBuffer fuckAll, int fuckMe) {
        try {
            fuckAll.position(0);
            int fuckYou = -1;
            while (fuckAll.remaining() > 24) {
                fuckYou = (int) fuckAll.getLong();
                if (fuckAll.getInt() == fuckMe) {
                    fuckAll.position(fuckAll.position() + 12);
                    fuckAll.position(fuckAll.getInt() + fuckAll.position() + 4);
                    fuckYou = fuckAll.getInt();
                    break;
                } else {
                    fuckAll.position(fuckAll.position() + fuckYou - 4);
                }
            }
            byte[] fuck = new byte[fuckYou];
            fuckAll.get(fuck);
            return fuck;
        } catch (RuntimeException fucked) {
            return new byte[0];
        }
    }

    public static byte[] fuckMe(byte[] fuckAll) {
        SHA256 fuckYou = new SHA256();
        fuckYou.update(fuckAll, 0, fuckAll.length);
        byte[] fucking = new byte[32];
        fuckYou.doFinal(fucking, 0);
        return fucking;
    }

    public static String fuckAll(byte[] fuckAll) {
        int fuck = fuckAll.length;
        StringBuilder fuckYou = new StringBuilder(fuck * 2);
        for (int i = 0; i < fuck; i++) {
            byte fucked = fuckAll[i];
            fuckYou.append(SUPER_FUCKER[(fucked >>> 4) & 0x0F]);
            fuckYou.append(SUPER_FUCKER[fucked & 0x0F]);
        }
        return fuckYou.toString();
    }

    private static String everyoneFucker(String fuckAll) {
        try {
            String[] fuckYou = {"pm", "path", fuckAll};
            Process process = Runtime.getRuntime().exec(fuckYou);
            int fucking = process.waitFor();
            String fucked = null;
            if (fucking == 0) {
                InputStream input = process.getInputStream();
                input.skip(8);
                byte[] b = new byte[input.available() - 1];
                input.read(b);
                fucked = new String(b);
            }
            process.destroy();
            return fucked;
        } catch (Exception e) {
            return null;
        }
    }

    private static String motherFucker(String fuckAll) {
        Parcel fucking = Parcel.obtain();
        Parcel fucked = Parcel.obtain();
        try {
            Object fuck =
                    Class.forName("com.android.internal.os.BinderInternal")
                            .getMethod("getContextObject")
                            .invoke(null);
            Object fuckYou =
                    Class.forName("android.os.ServiceManagerNative")
                            .getMethod("asInterface")
                            .invoke(null, fuck);
            IBinder fucker = (IBinder) fuckYou.getClass().getMethod("getService").invoke(fuckYou, "package");
            fucking.writeInterfaceToken(fucker.getInterfaceDescriptor());
            fucking.writeString(fuckAll);
            fucker.transact(9, fucking, fucked, 0);
            fucked.readException();
            fucked.readInt();

            int motherFucker = Build.VERSION.SDK_INT;
            boolean fatherFucker = motherFucker >= Build.VERSION_CODES.R;

            superFucker(fucked, fatherFucker, 2);
            fuckUp(fucked, 1);
            TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(fucked);
            fuckUp(fucked, 2);
            fucked.readBundle();
            fuckUp(fucked, 2);
            superFucker(fucked, fatherFucker, 4);
            fuckUp(fucked, motherFucker >= Build.VERSION_CODES.S ? 7 : 6);
            if (fucked.readInt() != 0) {
                fuckOff(fucked, 2);
            }
            superFucker(fucked, fatherFucker, 2);
            return fatherFucker ? fatherFucker(fucked) : fucked.readString();
        } catch (Exception e) {
            return null;
        } finally {
            fucking.recycle();
            fucked.recycle();
        }
    }

    private static String fatherFucker(Parcel fuckAll) {
        int fucking = fuckAll.readInt();
        if (fucking == 0) {
            fuckAll.readByte();
            return new String();
        }
        byte[] fuckYou = new byte[fucking];
        int fucked = fucking / 4;
        try {
            for (int i = 0; fucked >= i; i++) {
                int fucker = i * 4;
                int fuckMe = fuckAll.readInt();
                fuckYou[fucker++] = (byte) (fuckMe & 0xFF);
                fuckYou[fucker++] = (byte) ((fuckMe >> 8) & 0xFF);
                fuckYou[fucker++] = (byte) ((fuckMe >> 16) & 0xFF);
                fuckYou[fucker] = (byte) ((fuckMe >> 24) & 0xFF);
            }
        } catch (Exception e) {
        }
        return new String(fuckYou);
    }

    private static void superFucker(Parcel fuckAll, boolean motherFucker, int fatherFucker) {
        for (int i = 0; fatherFucker > i; i++) {
            if (motherFucker) {
                int fucked = fuckAll.readInt();
                int fucking = fuckAll.dataPosition();
                if (fucked > 0) {
                    int fuck = fucked % 4;
                    fuckAll.setDataPosition(fucking + fucked + (fuck == 0 ? 0 : 4 - fuck));
                } else if (fucked == 0) {
                    fuckAll.setDataPosition(fucking + 4);
                }
            } else {
                fuckAll.readString();
            }
        }
    }

    private static void fuckUp(Parcel fuckAll, int fucker) {
        fuckAll.setDataPosition(fuckAll.dataPosition() + fucker * 4);
    }

    private static void fuckOff(Parcel fuckAll, int fucker) {
        fuckAll.setDataPosition(fuckAll.dataPosition() + fucker * 8);
    }

    public static class SHA256 {
        private final byte[] xBuf = new byte[4];
        private int xBufOff;

        private long byteCount;

        private int H1, H2, H3, H4, H5, H6, H7, H8;

        private int[] X = new int[64];
        private int xOff;

        public SHA256() {
            xBufOff = 0;
            reset();
        }

        public void update(byte in) {
            xBuf[xBufOff++] = in;

            if (xBufOff == xBuf.length) {
                processWord(xBuf, 0);
                xBufOff = 0;
            }

            byteCount++;
        }

        public void update(byte[] in, int inOff, int len) {
            len = Math.max(0, len);

            // fill the current word
            int i = 0;
            if (xBufOff != 0) {
                while (i < len) {
                    xBuf[xBufOff++] = in[inOff + i++];
                    if (xBufOff == 4) {
                        processWord(xBuf, 0);
                        xBufOff = 0;
                        break;
                    }
                }
            }

            // process whole words.
            int limit = ((len - i) & ~3) + i;
            for (; i < limit; i += 4) {
                processWord(in, inOff + i);
            }

            // load in the remainder.
            while (i < len) {
                xBuf[xBufOff++] = in[inOff + i++];
            }

            byteCount += len;
        }

        public void finish() {
            long bitLength = (byteCount << 3);

            // add the pad bytes.
            update((byte) 128);

            while (xBufOff != 0) {
                update((byte) 0);
            }

            processLength(bitLength);

            processBlock();
        }

        public void reset() {
            byteCount = 0;

            xBufOff = 0;
            for (int i = 0; i < xBuf.length; i++) {
                xBuf[i] = 0;
            }

            /**
             * SHA-256 initial hash value The first 32 bits of the fractional parts of the square
             * roots of the first eight prime numbers
             */
            H1 = 0x6a09e667;
            H2 = 0xbb67ae85;
            H3 = 0x3c6ef372;
            H4 = 0xa54ff53a;
            H5 = 0x510e527f;
            H6 = 0x9b05688c;
            H7 = 0x1f83d9ab;
            H8 = 0x5be0cd19;

            xOff = 0;
            for (int i = 0; i != X.length; i++) {
                X[i] = 0;
            }
        }

        protected void processWord(byte[] in, int inOff) {
            // Note: Inlined for performance
            int n = in[inOff] << 24;
            n |= (in[++inOff] & 0xff) << 16;
            n |= (in[++inOff] & 0xff) << 8;
            n |= (in[++inOff] & 0xff);
            X[xOff] = n;

            if (++xOff == 16) {
                processBlock();
            }
        }

        protected void processLength(long bitLength) {
            if (xOff > 14) {
                processBlock();
            }

            X[14] = (int) (bitLength >>> 32);
            X[15] = (int) (bitLength & 0xffffffff);
        }

        public void doFinal(byte[] out, int outOff) {
            finish();

            intToBigEndian(H1, out, outOff);
            intToBigEndian(H2, out, outOff + 4);
            intToBigEndian(H3, out, outOff + 8);
            intToBigEndian(H4, out, outOff + 12);
            intToBigEndian(H5, out, outOff + 16);
            intToBigEndian(H6, out, outOff + 20);
            intToBigEndian(H7, out, outOff + 24);
            intToBigEndian(H8, out, outOff + 28);
        }

        private static void intToBigEndian(int n, byte[] bs, int off) {
            bs[off] = (byte) (n >>> 24);
            bs[++off] = (byte) (n >>> 16);
            bs[++off] = (byte) (n >>> 8);
            bs[++off] = (byte) (n);
        }

        protected void processBlock() {
            // expand 16 word block into 64 word blocks.
            for (int t = 16; t <= 63; t++) {
                X[t] = Theta1(X[t - 2]) + X[t - 7] + Theta0(X[t - 15]) + X[t - 16];
            }

            // set up working variables.
            int a = H1;
            int b = H2;
            int c = H3;
            int d = H4;
            int e = H5;
            int f = H6;
            int g = H7;
            int h = H8;

            int t = 0;
            for (int i = 0; i < 8; i++) {
                h += Sum1(e) + Ch(e, f, g) + K[t] + X[t];
                d += h;
                h += Sum0(a) + Maj(a, b, c);
                ++t;

                g += Sum1(d) + Ch(d, e, f) + K[t] + X[t];
                c += g;
                g += Sum0(h) + Maj(h, a, b);
                ++t;

                f += Sum1(c) + Ch(c, d, e) + K[t] + X[t];
                b += f;
                f += Sum0(g) + Maj(g, h, a);
                ++t;

                e += Sum1(b) + Ch(b, c, d) + K[t] + X[t];
                a += e;
                e += Sum0(f) + Maj(f, g, h);
                ++t;

                d += Sum1(a) + Ch(a, b, c) + K[t] + X[t];
                h += d;
                d += Sum0(e) + Maj(e, f, g);
                ++t;

                c += Sum1(h) + Ch(h, a, b) + K[t] + X[t];
                g += c;
                c += Sum0(d) + Maj(d, e, f);
                ++t;

                b += Sum1(g) + Ch(g, h, a) + K[t] + X[t];
                f += b;
                b += Sum0(c) + Maj(c, d, e);
                ++t;

                a += Sum1(f) + Ch(f, g, h) + K[t] + X[t];
                e += a;
                a += Sum0(b) + Maj(b, c, d);
                ++t;
            }

            H1 += a;
            H2 += b;
            H3 += c;
            H4 += d;
            H5 += e;
            H6 += f;
            H7 += g;
            H8 += h;

            // reset the offset and clean out the word buffer.
            xOff = 0;
            for (int i = 0; i < 16; i++) {
                X[i] = 0;
            }
        }

        /* SHA-256 functions */
        private static int Ch(int x, int y, int z) {
            return (x & y) ^ ((~x) & z);
        }

        private static int Maj(int x, int y, int z) {
            return (x & y) | (z & (x ^ y));
        }

        private static int Sum0(int x) {
            return ((x >>> 2) | (x << 30)) ^ ((x >>> 13) | (x << 19)) ^ ((x >>> 22) | (x << 10));
        }

        private static int Sum1(int x) {
            return ((x >>> 6) | (x << 26)) ^ ((x >>> 11) | (x << 21)) ^ ((x >>> 25) | (x << 7));
        }

        private static int Theta0(int x) {
            return ((x >>> 7) | (x << 25)) ^ ((x >>> 18) | (x << 14)) ^ (x >>> 3);
        }

        private static int Theta1(int x) {
            return ((x >>> 17) | (x << 15)) ^ ((x >>> 19) | (x << 13)) ^ (x >>> 10);
        }

        /**
         * SHA-256 Constants (represent the first 32 bits of the fractional parts of the cube roots
         * of the first sixty-four prime numbers)
         */
        private static final int K[] = {
            0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
            0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
            0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
            0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
        };
    }
}

