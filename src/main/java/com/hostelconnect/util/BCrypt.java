package com.hostelconnect.util;

import java.security.SecureRandom;

/**
 * BCrypt password hashing implementation based on OpenBSD blowfish.
 */
public class BCrypt {
    private static final int BCRYPT_SALT_LEN = 16;
    private static final int BLOWFISH_NUM_ROUNDS = 16;

    private static final int P_orig[] = {
        0x243f6a88, 0x85a308d3, 0x13198a2e, 0x03707344,
        0xa4093822, 0x299f31d0, 0x082efa98, 0xec4e6c89,
        0x452821e6, 0x38d01377, 0xbe5466cf, 0x34e90c6c,
        0xc0ac29b7, 0xc97c50dd, 0x3f84d5b5, 0xb5470917,
        0x9216d5d9, 0x8979fb1b
    };

    private static final int S_orig[] = {
        0xd1310ba6, 0x98dfb5ac, 0x2ffd72db, 0xd01adfb7,
        0xb8e1afed, 0x6a267e96, 0xba7c9045, 0xf12c7f99,
        0x24a19947, 0xb3916cf7, 0x0801f2e2, 0x858efc16,
        0x636920d8, 0x71574e69, 0xa458fea3, 0xf4933d7e,
        0x0d95748f, 0x728eb658, 0x718bcd58, 0x82154aee,
        0x7b54a41d, 0xc25a59b5, 0x9c30d539, 0x2af26013,
        0xc5d1b023, 0x286085f0, 0xca417918, 0xb8db38ef,
        0x8e79dcb0, 0x603a180e, 0x6c9e0e8b, 0xb01e8a3e,
        0xd71577c1, 0xbd314b27, 0x78af2fda, 0x55605c60,
        0xe65525f3, 0xaa55ab94, 0x57489862, 0x63e81440,
        0x55ca396a, 0x2aab10b6, 0xb4cc5c34, 0x1141e8ce,
        0xa15486af, 0x7c72e993, 0xb3ee1411, 0x636fbc2a,
        0x2ba9c55d, 0x741831f6, 0xce5c3e16, 0x9b87931e,
        0xafd6ba33, 0x6c24cf5c, 0x7a325381, 0x28958677,
        0x3b8f4898, 0x6b4bb9af, 0xc4bfe81b, 0x66282193,
        0x61d809cc, 0xfb21a991, 0x487cac60, 0x5dec8032,
        0xef845d5d, 0xe98575b1, 0xdc262302, 0xeb651b88,
        0x23893e81, 0xd396acc5, 0x0f6d6ff3, 0x83f44239,
        0x2e0b4482, 0xa4842004, 0x69c8f04a, 0x9e1f9b5e,
        0x21c66842, 0xf6e96c9a, 0x670c9c61, 0xabd388f0,
        0x6a51a0d2, 0xd8542f68, 0x960fa728, 0xab5133a3,
        0x6eef0b6c, 0x137a3be4, 0xba3bf050, 0x7efb2bbe,
        0x9b1147b7, 0x7f4aace1, 0x47b973e6, 0x789b8893,
        0xf2448316, 0x4d667c2f, 0x9815041a, 0x0b8a071b,
        0x4509180c, 0x235e12ec, 0x256daef4, 0x79ea0f24,
        0x995383a5, 0x1646292b, 0xa7978283, 0xc27ac606,
        0x22c2a048, 0x462215c0, 0x217d526e, 0xbfd397d4,
        0xa75af0a2, 0x1e3a6509, 0x1b131705, 0xadc694d6,
        0x71869e80, 0x05b9b8b9, 0xbf3f29b4, 0x4036720c,
        0x77c44199, 0xb8b8ec0b, 0x9606d223, 0x48ee4768,
        0x9a50dd20, 0x9599589f, 0x9043fe42, 0x51c513e9,
        0x2e06c39a, 0x45f8e5f2, 0x3d027732, 0x68545e69,
        0x2646f888, 0x750943cc, 0x868b23c2, 0x402b4129,
        0x681878b2, 0x2fb3a505, 0x73ff75c9, 0x7e5b65ff,
        0x47e1ff7b, 0x6758652d, 0x6f63412a, 0x00f738f7,
        0x98b8c292, 0x0f2f35f8, 0x9924c43a, 0xb5ffb892,
        0x90680d90, 0x0374e2d2, 0xebfa1ee4, 0x9f564756,
        0x36b04297, 0x3855ff55, 0x5be377fa, 0x2dfb0a32,
        0x95c805a8, 0x5e56e026, 0x9e6c4660, 0x71f34f77,
        0x7c73dbca, 0x8085fa49, 0x973a2414, 0xa74d1c44,
        0x45199616, 0x1870b240, 0xd88a0329, 0xa1ae516c,
        0x9e17b833, 0x41f3e792, 0xeb1a7428, 0x247754b2,
        0x27242d54, 0x93306869, 0xa316f73a, 0x81504936,
        0x56a6ea72, 0x61986683, 0x1f0f005c, 0xa59f790c,
        0x83827ec3, 0x06713cf7, 0x93757db6, 0x7ef33f2d,
        0x391f17fe, 0x5990264b, 0x18e8055c, 0x71790403,
        0xa77f9850, 0x21a415ff, 0x0283c749, 0xb8e08d66,
        0xd1d49fca, 0x274a242c, 0x78ab9c70, 0xd03770e2,
        0x96985392, 0x253b7c84, 0x4b786cbe, 0x6e2df409,
        0x7ecf8b5a, 0x9061093c, 0x3ac57022, 0x46a6f059,
        0xb7548b8a, 0xd4681604, 0x8544da93, 0x255b7194,
        0xc77bc2a6, 0xd3840787, 0x0986e729, 0x8e5792ec,
        0x7a637dbf, 0x84f18370, 0x07b19889, 0xb3388716,
        0xeb919a32, 0xd73be90f, 0xc1438992, 0x608e0638,
        0x6002f1a6, 0xd9f6a623, 0x9599589f, 0xc42e0cae,
        0x91d960f4, 0x00a74859, 0x1e36fc48, 0xdd128e44,
        0x344d5c19, 0xf6a761e8, 0xa53b7829, 0x43867b9a,
        0xb6894382, 0x9d727b12, 0x14f923b0, 0x8a92f033,
        0xae5f63ec, 0x2b069dcd, 0xa2fd2130, 0xf60882e9,
        0x79a61bb8, 0x762b322a, 0xa2c95e1e, 0x8fd0ff9f,
        0x044be2a7, 0x89299cf7, 0x619e0758, 0x41ef3f48,
        0xb5244510, 0xbb0f59ee, 0x6b876403, 0x5eb1c110,
        0xa7ef1108, 0xcd6be692, 0xd581457f, 0x1174620b,
        0xa0a9117a, 0x011b98a4, 0x261ef2fc, 0x20562e84
    };

    private static final char base64_code[] = {
        '.', '/', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
        'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
        'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
        'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5',
        '6', '7', '8', '9'
    };

    private static final byte index_64[] = {
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  0,  1,
        54, 55, 56, 57, 58, 59, 60, 61, 62, 63, -1, -1, -1, -1, -1, -1,
        -1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16,
        17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, -1, -1, -1, -1, -1,
        -1, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42,
        43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, -1, -1, -1, -1, -1
    };

    private int P[];
    private int S[];

    private static int char64(char x) {
        if (x < 0 || x > 127) return -1;
        return index_64[x];
    }

    private static String encode_base64(byte d[], int len) {
        int off = 0;
        StringBuilder rs = new StringBuilder();
        int c1, c2;

        while (off < len) {
            c1 = (d[off++] & 0xff);
            rs.append(base64_code[(c1 >> 2) & 0x3f]);
            c1 = (c1 & 0x03) << 4;
            if (off >= len) {
                rs.append(base64_code[c1 & 0x3f]);
                break;
            }
            c2 = (d[off++] & 0xff);
            c1 |= (c2 >> 4) & 0x0f;
            rs.append(base64_code[c1 & 0x3f]);
            c1 = (c2 & 0x0f) << 2;
            if (off >= len) {
                rs.append(base64_code[c1 & 0x3f]);
                break;
            }
            c2 = (d[off++] & 0xff);
            c1 |= (c2 >> 6) & 0x03;
            rs.append(base64_code[c1 & 0x3f]);
            rs.append(base64_code[c2 & 0x3f]);
        }
        return rs.toString();
    }

    private static byte[] decode_base64(String s, int maxolen) {
        StringBuilder rs = new StringBuilder();
        int off = 0, slen = s.length(), olen = 0;
        byte ret[] = new byte[maxolen];
        byte c1, c2, c3, c4, o;

        while (off < slen - 1 && olen < maxolen) {
            c1 = (byte) char64(s.charAt(off++));
            c2 = (byte) char64(s.charAt(off++));
            if (c1 == -1 || c2 == -1) break;
            o = (byte) (c1 << 2);
            o |= (c2 & 0x30) >> 4;
            ret[olen++] = o;
            if (olen >= maxolen || off >= slen) break;
            c3 = (byte) char64(s.charAt(off++));
            if (c3 == -1) break;
            o = (byte) ((c2 & 0x0f) << 4);
            o |= (c3 & 0x3c) >> 2;
            ret[olen++] = o;
            if (olen >= maxolen || off >= slen) break;
            c4 = (byte) char64(s.charAt(off++));
            o = (byte) ((c3 & 0x03) << 6);
            o |= c4;
            ret[olen++] = o;
        }
        return ret;
    }

    private void encipher(int lr[], int off) {
        int i, n, l = lr[off], r = lr[off + 1];
        l ^= P[0];
        for (i = 0; i <= BLOWFISH_NUM_ROUNDS - 2; i += 2) {
            n = S[(l >> 24) & 0xff];
            n += S[0x100 | ((l >> 16) & 0xff)];
            n ^= S[0x200 | ((l >> 8) & 0xff)];
            n += S[0x300 | (l & 0xff)];
            r ^= n ^ P[i + 1];

            n = S[(r >> 24) & 0xff];
            n += S[0x100 | ((r >> 16) & 0xff)];
            n ^= S[0x200 | ((r >> 8) & 0xff)];
            n += S[0x300 | (r & 0xff)];
            l ^= n ^ P[i + 2];
        }
        lr[off] = r ^ P[BLOWFISH_NUM_ROUNDS + 1];
        lr[off + 1] = l;
    }

    private void init_key() {
        P = (int[]) P_orig.clone();
        S = (int[]) S_orig.clone();
    }

    private void key(byte key[]) {
        int i, j, k, data, dlen = key.length;
        int lr[] = {0, 0};

        for (i = 0, k = 0; i < BLOWFISH_NUM_ROUNDS + 2; ++i) {
            data = 0;
            for (j = 0; j < 4; ++j) {
                data = (data << 8) | (key[k] & 0xff);
                k = (k + 1) % dlen;
            }
            P[i] ^= data;
        }

        for (i = 0; i < BLOWFISH_NUM_ROUNDS + 2; i += 2) {
            encipher(lr, 0);
            P[i] = lr[0];
            P[i + 1] = lr[1];
        }

        for (i = 0; i < 4 * 256; i += 2) {
            encipher(lr, 0);
            S[i] = lr[0];
            S[i + 1] = lr[1];
        }
    }

    private void ekskey(byte data[], byte key[]) {
        int i, j, k, data_len = data.length, key_len = key.length;
        int lr[] = {0, 0};

        for (i = 0, k = 0; i < BLOWFISH_NUM_ROUNDS + 2; ++i) {
            int d = 0;
            for (j = 0; j < 4; ++j) {
                d = (d << 8) | (key[k] & 0xff);
                k = (k + 1) % key_len;
            }
            P[i] ^= d;
        }

        for (i = 0, k = 0; i < BLOWFISH_NUM_ROUNDS + 2; i += 2) {
            lr[0] ^= ((data[k % data_len] & 0xff) << 24)
                | ((data[(k + 1) % data_len] & 0xff) << 16)
                | ((data[(k + 2) % data_len] & 0xff) << 8)
                | (data[(k + 3) % data_len] & 0xff);
            k = (k + 4) % data_len;
            lr[1] ^= ((data[k % data_len] & 0xff) << 24)
                | ((data[(k + 1) % data_len] & 0xff) << 16)
                | ((data[(k + 2) % data_len] & 0xff) << 8)
                | (data[(k + 3) % data_len] & 0xff);
            k = (k + 4) % data_len;
            encipher(lr, 0);
            P[i] = lr[0];
            P[i + 1] = lr[1];
        }

        for (i = 0; i < 4 * 256; i += 2) {
            lr[0] ^= ((data[k % data_len] & 0xff) << 24)
                | ((data[(k + 1) % data_len] & 0xff) << 16)
                | ((data[(k + 2) % data_len] & 0xff) << 8)
                | (data[(k + 3) % data_len] & 0xff);
            k = (k + 4) % data_len;
            lr[1] ^= ((data[k % data_len] & 0xff) << 24)
                | ((data[(k + 1) % data_len] & 0xff) << 16)
                | ((data[(k + 2) % data_len] & 0xff) << 8)
                | (data[(k + 3) % data_len] & 0xff);
            k = (k + 4) % data_len;
            encipher(lr, 0);
            S[i] = lr[0];
            S[i + 1] = lr[1];
        }
    }

    private byte[] crypt_raw(byte password[], byte salt[], int log_rounds, int cdata[]) {
        int rounds, i, j;
        int clen = cdata.length;
        byte ret[];

        if (log_rounds < 4 || log_rounds > 31) {
            throw new IllegalArgumentException("Bad number of rounds");
        }
        rounds = 1 << log_rounds;
        if (salt.length != BCRYPT_SALT_LEN) {
            throw new IllegalArgumentException("Bad salt length");
        }

        init_key();
        ekskey(salt, password);
        for (i = 0; i < rounds; i++) {
            key(password);
            key(salt);
        }

        for (i = 0; i < 64; i++) {
            for (j = 0; j < (clen >> 1); j++) {
                encipher(cdata, j << 1);
            }
        }

        ret = new byte[clen * 4];
        for (i = 0, j = 0; i < clen; i++) {
            ret[j++] = (byte) ((cdata[i] >> 24) & 0xff);
            ret[j++] = (byte) ((cdata[i] >> 16) & 0xff);
            ret[j++] = (byte) ((cdata[i] >> 8) & 0xff);
            ret[j++] = (byte) (cdata[i] & 0xff);
        }
        return ret;
    }

    private static final int bf_crypt_ciphertext[] = {
        0x4f727068, 0x65616e42, 0x65686f6c,
        0x64657253, 0x63727964, 0x6f756274
    };

    public static String hashpw(String password, String salt) {
        BCrypt B;
        String real_salt;
        byte passwordb[], saltb[], hashed[];
        char minor = (char) 0;
        int rounds, off = 0;
        StringBuilder rs = new StringBuilder();

        if (salt.charAt(0) != '$' || salt.charAt(1) != '2') {
            throw new IllegalArgumentException("Invalid salt version");
        }
        if (salt.charAt(2) == '$') {
            off = 3;
        } else {
            minor = salt.charAt(2);
            if (minor != 'a' && minor != 'b' && minor != 'y' || salt.charAt(3) != '$') {
                throw new IllegalArgumentException("Invalid salt revision");
            }
            off = 4;
        }

        if (salt.charAt(off + 2) > '$') {
            throw new IllegalArgumentException("Missing salt rounds");
        }
        rounds = Integer.parseInt(salt.substring(off, off + 2));
        real_salt = salt.substring(off + 3, off + 25);
        try {
            passwordb = (password + (minor >= 'a' ? "\0" : "")).getBytes("UTF-8");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        saltb = decode_base64(real_salt, BCRYPT_SALT_LEN);

        B = new BCrypt();
        hashed = B.crypt_raw(passwordb, saltb, rounds, (int[]) bf_crypt_ciphertext.clone());

        rs.append("$2a$");
        if (rounds < 10) rs.append("0");
        rs.append(rounds);
        rs.append("$");
        rs.append(encode_base64(saltb, saltb.length));
        rs.append(encode_base64(hashed, bf_crypt_ciphertext.length * 4 - 1));
        return rs.toString();
    }

    public static String gensalt(int log_rounds) {
        byte rnd[] = new byte[BCRYPT_SALT_LEN];
        new SecureRandom().nextBytes(rnd);
        StringBuilder rs = new StringBuilder();
        rs.append("$2a$");
        if (log_rounds < 10) rs.append("0");
        rs.append(log_rounds);
        rs.append("$");
        rs.append(encode_base64(rnd, rnd.length));
        return rs.toString();
    }

    public static String gensalt() {
        return gensalt(10);
    }

    public static boolean checkpw(String plaintext, String hashed) {
        try {
            return hashpw(plaintext, hashed).equals(hashed);
        } catch (Exception e) {
            return false;
        }
    }
}
