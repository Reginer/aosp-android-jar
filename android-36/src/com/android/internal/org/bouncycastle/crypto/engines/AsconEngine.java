/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.crypto.engines;

import com.android.internal.org.bouncycastle.crypto.CipherParameters;
import com.android.internal.org.bouncycastle.crypto.CryptoServicesRegistrar;
import com.android.internal.org.bouncycastle.crypto.DataLengthException;
import com.android.internal.org.bouncycastle.crypto.InvalidCipherTextException;
import com.android.internal.org.bouncycastle.crypto.OutputLengthException;
import com.android.internal.org.bouncycastle.crypto.constraints.DefaultServiceProperties;
import com.android.internal.org.bouncycastle.crypto.modes.AEADCipher;
import com.android.internal.org.bouncycastle.crypto.params.AEADParameters;
import com.android.internal.org.bouncycastle.crypto.params.KeyParameter;
import com.android.internal.org.bouncycastle.crypto.params.ParametersWithIV;
import com.android.internal.org.bouncycastle.util.Arrays;
import com.android.internal.org.bouncycastle.util.Longs;
import com.android.internal.org.bouncycastle.util.Pack;

/**
 * ASCON AEAD v1.2, https://ascon.iaik.tugraz.at/
 * https://csrc.nist.gov/CSRC/media/Projects/lightweight-cryptography/documents/finalist-round/updated-spec-doc/ascon-spec-final.pdf
 * <p>
 * ASCON AEAD v1.2 with reference to C Reference Impl from: https://github.com/ascon/ascon-c
 * </p>
 * @hide This class is not part of the Android public SDK API
 */
public class AsconEngine
    implements AEADCipher
{
    /**
     * @hide This class is not part of the Android public SDK API
     */
    public enum AsconParameters
    {
        ascon80pq,
        ascon128a,
        ascon128
    }

    private enum State
    {
        Uninitialized,
        EncInit,
        EncAad,
        EncData,
        EncFinal,
        DecInit,
        DecAad,
        DecData,
        DecFinal,
    }

    private final AsconParameters asconParameters;
    private State m_state = State.Uninitialized;
    private byte[] mac;
    private byte[] initialAssociatedText;
    private final String algorithmName;
    private final int CRYPTO_KEYBYTES;
    private final int CRYPTO_ABYTES;
    private final int ASCON_AEAD_RATE;
    private final int nr;
    private long K0;
    private long K1;
    private long K2;
    private long N0;
    private long N1;
    private final long ASCON_IV;
    private long x0;
    private long x1;
    private long x2;
    private long x3;
    private long x4;
    private final int m_bufferSizeDecrypt;
    private final byte[] m_buf;
    private int m_bufPos = 0;

    public AsconEngine(AsconParameters asconParameters)
    {
        this.asconParameters = asconParameters;
        switch (asconParameters)
        {
        case ascon80pq:
            CRYPTO_KEYBYTES = 20;
            CRYPTO_ABYTES = 16;
            ASCON_AEAD_RATE = 8;
            ASCON_IV = 0xa0400c0600000000L;
            algorithmName = "Ascon-80pq AEAD";
            break;
        case ascon128a:
            CRYPTO_KEYBYTES = 16;
            CRYPTO_ABYTES = 16;
            ASCON_AEAD_RATE = 16;
            ASCON_IV = 0x80800c0800000000L;
            algorithmName = "Ascon-128a AEAD";
            break;
        case ascon128:
            CRYPTO_KEYBYTES = 16;
            CRYPTO_ABYTES = 16;
            ASCON_AEAD_RATE = 8;
            ASCON_IV = 0x80400c0600000000L;
            algorithmName = "Ascon-128 AEAD";
            break;
        default:
            throw new IllegalArgumentException("invalid parameter setting for ASCON AEAD");
        }
        nr = (ASCON_AEAD_RATE == 8) ? 6 : 8;
        m_bufferSizeDecrypt = ASCON_AEAD_RATE + CRYPTO_ABYTES;
        m_buf = new byte[m_bufferSizeDecrypt];
    }

    private long PAD(int i)
    {
        return 0x80L << (56 - (i << 3));
    }

    private void ROUND(long C)
    {
        long t0 = x0 ^ x1 ^ x2 ^ x3 ^ C ^ (x1 & (x0 ^ x2 ^ x4 ^ C));
        long t1 = x0 ^ x2 ^ x3 ^ x4 ^ C ^ ((x1 ^ x2 ^ C) & (x1 ^ x3));
        long t2 = x1 ^ x2 ^ x4 ^ C ^ (x3 & x4);
        long t3 = x0 ^ x1 ^ x2 ^ C ^ ((~x0) & (x3 ^ x4));
        long t4 = x1 ^ x3 ^ x4 ^ ((x0 ^ x4) & x1);
        x0 = t0 ^ Longs.rotateRight(t0, 19) ^ Longs.rotateRight(t0, 28);
        x1 = t1 ^ Longs.rotateRight(t1, 39) ^ Longs.rotateRight(t1, 61);
        x2 = ~(t2 ^ Longs.rotateRight(t2, 1) ^ Longs.rotateRight(t2, 6));
        x3 = t3 ^ Longs.rotateRight(t3, 10) ^ Longs.rotateRight(t3, 17);
        x4 = t4 ^ Longs.rotateRight(t4, 7) ^ Longs.rotateRight(t4, 41);
    }

    private void P(int nr)
    {
        if (nr >= 8)
        {
            if (nr == 12)
            {
                ROUND(0xf0L);
                ROUND(0xe1L);
                ROUND(0xd2L);
                ROUND(0xc3L);
            }
            ROUND(0xb4L);
            ROUND(0xa5L);
        }
        ROUND(0x96L);
        ROUND(0x87L);
        ROUND(0x78L);
        ROUND(0x69L);
        ROUND(0x5aL);
        ROUND(0x4bL);
    }

    private void ascon_aeadinit()
    {
        /* initialize */
        x0 = ASCON_IV;
        if (CRYPTO_KEYBYTES == 20)
        {
            x0 ^= K0;
        }
        x1 = K1;
        x2 = K2;
        x3 = N0;
        x4 = N1;
        P(12);
        if (CRYPTO_KEYBYTES == 20)
        {
            x2 ^= K0;
        }
        x3 ^= K1;
        x4 ^= K2;
    }

    private void checkAAD()
    {
        switch (m_state)
        {
        case DecInit:
            m_state = State.DecAad;
            break;
        case EncInit:
            m_state = State.EncAad;
            break;
        case DecAad:
        case EncAad:
            break;
        case EncFinal:
            throw new IllegalStateException(getAlgorithmName() + " cannot be reused for encryption");
        default:
            throw new IllegalStateException(getAlgorithmName() + " needs to be initialized");
        }
    }

    private boolean checkData()
    {
        switch (m_state)
        {
        case DecInit:
        case DecAad:
            finishAAD(State.DecData);
            return false;
        case EncInit:
        case EncAad:
            finishAAD(State.EncData);
            return true;
        case DecData:
            return false;
        case EncData:
            return true;
        case EncFinal:
            throw new IllegalStateException(getAlgorithmName() + " cannot be reused for encryption");
        default:
            throw new IllegalStateException(getAlgorithmName() + " needs to be initialized");
        }
    }

    private void processBufferAAD(byte[] buffer, int inOff)
    {
        x0 ^= Pack.bigEndianToLong(buffer, inOff);
        if (ASCON_AEAD_RATE == 16)
        {
            x1 ^= Pack.bigEndianToLong(buffer, 8 + inOff);
        }
        P(nr);
    }

    private void finishAAD(State nextState)
    {
        // State indicates whether we ever received AAD
        switch (m_state)
        {
        case DecAad:
        case EncAad:
            m_buf[m_bufPos] = (byte)0x80;
            if (m_bufPos >= 8) // ASCON_AEAD_RATE == 16 is implied
            {
                x0 ^= Pack.bigEndianToLong(m_buf, 0);
                x1 ^= Pack.bigEndianToLong(m_buf, 8) & (-1L << (56 - ((m_bufPos - 8) << 3)));
            }
            else
            {
                x0 ^= Pack.bigEndianToLong(m_buf, 0) & (-1L << (56 - (m_bufPos << 3)));
            }
            P(nr);
            break;
        default:
            break;
        }
        // domain separation
        x4 ^= 1L;
        m_bufPos = 0;
        m_state = nextState;
    }

    private void processBufferDecrypt(byte[] buffer, int bufOff, byte[] output, int outOff)
    {
        if (outOff + ASCON_AEAD_RATE > output.length)
        {
            throw new OutputLengthException("output buffer too short");
        }
        long t0 = Pack.bigEndianToLong(buffer, bufOff);
        Pack.longToBigEndian(x0 ^ t0, output, outOff);
        x0 = t0;

        if (ASCON_AEAD_RATE == 16)
        {
            long t1 = Pack.bigEndianToLong(buffer, bufOff + 8);
            Pack.longToBigEndian(x1 ^ t1, output, outOff + 8);
            x1 = t1;
        }
        P(nr);
    }

    private void processBufferEncrypt(byte[] buffer, int bufOff, byte[] output, int outOff)
    {
        if (outOff + ASCON_AEAD_RATE > output.length)
        {
            throw new OutputLengthException("output buffer too short");
        }
        x0 ^= Pack.bigEndianToLong(buffer, bufOff);
        Pack.longToBigEndian(x0, output, outOff);

        if (ASCON_AEAD_RATE == 16)
        {
            x1 ^= Pack.bigEndianToLong(buffer, bufOff + 8);
            Pack.longToBigEndian(x1, output, outOff + 8);
        }

        P(nr);
    }

    private void processFinalDecrypt(byte[] input, int inOff, int inLen, byte[] output, int outOff)
    {
        if (inLen >= 8) // ASCON_AEAD_RATE == 16 is implied
        {
            long c0 = Pack.bigEndianToLong(input, inOff);
            x0 ^= c0;
            Pack.longToBigEndian(x0, output, outOff);
            x0 = c0;
            inOff += 8;
            outOff += 8;
            inLen -= 8;
            x1 ^= PAD(inLen);
            if (inLen != 0)
            {
                long c1 = Pack.littleEndianToLong_High(input, inOff, inLen);
                x1 ^= c1;
                Pack.longToLittleEndian_High(x1, output, outOff, inLen);
                x1 &= -1L >>> (inLen << 3);
                x1 ^= c1;
            }
        }
        else
        {
            x0 ^= PAD(inLen);
            if (inLen != 0)
            {
                long c0 = Pack.littleEndianToLong_High(input, inOff, inLen);
                x0 ^= c0;
                Pack.longToLittleEndian_High(x0, output, outOff, inLen);
                x0 &= -1L >>> (inLen << 3);
                x0 ^= c0;
            }
        }

        finishData(State.DecFinal);
    }

    private void processFinalEncrypt(byte[] input, int inOff, int inLen, byte[] output, int outOff)
    {
        if (inLen >= 8) // ASCON_AEAD_RATE == 16 is implied
        {
            x0 ^= Pack.bigEndianToLong(input, inOff);
            Pack.longToBigEndian(x0, output, outOff);
            inOff += 8;
            outOff += 8;
            inLen -= 8;
            x1 ^= PAD(inLen);
            if (inLen != 0)
            {
                x1 ^= Pack.littleEndianToLong_High(input, inOff, inLen);
                Pack.longToLittleEndian_High(x1, output, outOff, inLen);
            }
        }
        else
        {
            x0 ^= PAD(inLen);
            if (inLen != 0)
            {
                x0 ^= Pack.littleEndianToLong_High(input, inOff, inLen);
                Pack.longToLittleEndian_High(x0, output, outOff, inLen);
            }
        }
        finishData(State.EncFinal);
    }

    private void finishData(State nextState)
    {
        switch (asconParameters)
        {
        case ascon128:
            x1 ^= K1;
            x2 ^= K2;
            break;
        case ascon128a:
            x2 ^= K1;
            x3 ^= K2;
            break;
        case ascon80pq:
            x1 ^= (K0 << 32 | K1 >> 32);
            x2 ^= (K1 << 32 | K2 >> 32);
            x3 ^= K2 << 32;
            break;
        default:
            throw new IllegalStateException();
        }
        P(12);
        x3 ^= K1;
        x4 ^= K2;

        m_state = nextState;
    }

    public void init(boolean forEncryption, CipherParameters params)
        throws IllegalArgumentException
    {
        KeyParameter key;
        byte[] npub;
        if (params instanceof AEADParameters)
        {
            AEADParameters aeadParameters = (AEADParameters)params;
            key = aeadParameters.getKey();
            npub = aeadParameters.getNonce();
            initialAssociatedText = aeadParameters.getAssociatedText();

            int macSizeBits = aeadParameters.getMacSize();
            if (macSizeBits != CRYPTO_ABYTES * 8)
            {
                throw new IllegalArgumentException("Invalid value for MAC size: " + macSizeBits);
            }
        }
        else if (params instanceof ParametersWithIV)
        {
            ParametersWithIV withIV = (ParametersWithIV)params;
            key = (KeyParameter)withIV.getParameters();
            npub = withIV.getIV();
            initialAssociatedText = null;
        }
        else
        {
            throw new IllegalArgumentException("invalid parameters passed to Ascon");
        }

        if (key == null)
        {
            throw new IllegalArgumentException("Ascon Init parameters must include a key");
        }
        if (npub == null || npub.length != CRYPTO_ABYTES)
        {
            throw new IllegalArgumentException(asconParameters + " requires exactly " + CRYPTO_ABYTES + " bytes of IV");
        }

        byte[] k = key.getKey();
        if (k.length != CRYPTO_KEYBYTES)
        {
            throw new IllegalArgumentException(asconParameters + " key must be " + CRYPTO_KEYBYTES + " bytes long");
        }

        CryptoServicesRegistrar.checkConstraints(new DefaultServiceProperties(
            this.getAlgorithmName(), 128, params, Utils.getPurpose(forEncryption)));
        N0 = Pack.bigEndianToLong(npub, 0);
        N1 = Pack.bigEndianToLong(npub, 8);
        if (CRYPTO_KEYBYTES == 16)
        {
            K1 = Pack.bigEndianToLong(k, 0);
            K2 = Pack.bigEndianToLong(k, 8);
        }
        else if (CRYPTO_KEYBYTES == 20)
        {
            K0 = Pack.bigEndianToInt(k, 0);
            K1 = Pack.bigEndianToLong(k, 4);
            K2 = Pack.bigEndianToLong(k, 12);
        }
        else
        {
            throw new IllegalStateException();
        }

        m_state = forEncryption ? State.EncInit : State.DecInit;

        reset(true);
    }

    public String getAlgorithmName()
    {
        return algorithmName;
    }

    public String getAlgorithmVersion()
    {
        return "v1.2";
    }

    public void processAADByte(byte in)
    {
        checkAAD();
        m_buf[m_bufPos] = in;
        if (++m_bufPos == ASCON_AEAD_RATE)
        {
            processBufferAAD(m_buf, 0);
        }
    }

    public void processAADBytes(byte[] inBytes, int inOff, int len)
    {
        if ((inOff + len) > inBytes.length)
        {
            throw new DataLengthException("input buffer too short");
        }
        // Don't enter AAD state until we actually get input
        if (len <= 0)
        {
            return;
        }
        checkAAD();
        if (m_bufPos > 0)
        {
            int available = ASCON_AEAD_RATE - m_bufPos;
            if (len < available)
            {
                System.arraycopy(inBytes, inOff, m_buf, m_bufPos, len);
                m_bufPos += len;
                return;
            }
            System.arraycopy(inBytes, inOff, m_buf, m_bufPos, available);
            inOff += available;
            len -= available;
            processBufferAAD(m_buf, 0);
            //m_bufPos = 0;
        }
        while (len >= ASCON_AEAD_RATE)
        {
            processBufferAAD(inBytes, inOff);
            inOff += ASCON_AEAD_RATE;
            len -= ASCON_AEAD_RATE;
        }
        System.arraycopy(inBytes, inOff, m_buf, 0, len);
        m_bufPos = len;
    }

    public int processByte(byte in, byte[] out, int outOff)
        throws DataLengthException
    {
        return processBytes(new byte[]{in}, 0, 1, out, outOff);
    }

    public int processBytes(byte[] inBytes, int inOff, int len, byte[] outBytes, int outOff)
        throws DataLengthException
    {
        if ((inOff + len) > inBytes.length)
        {
            throw new DataLengthException("input buffer too short");
        }
        boolean forEncryption = checkData();
        int resultLength = 0;

        if (forEncryption)
        {
            if (m_bufPos > 0)
            {
                int available = ASCON_AEAD_RATE - m_bufPos;
                if (len < available)
                {
                    System.arraycopy(inBytes, inOff, m_buf, m_bufPos, len);
                    m_bufPos += len;
                    return 0;
                }

                System.arraycopy(inBytes, inOff, m_buf, m_bufPos, available);
                inOff += available;
                len -= available;

                processBufferEncrypt(m_buf, 0, outBytes, outOff);
                resultLength = ASCON_AEAD_RATE;
                //m_bufPos = 0;
            }

            while (len >= ASCON_AEAD_RATE)
            {
                processBufferEncrypt(inBytes, inOff, outBytes, outOff + resultLength);
                inOff += ASCON_AEAD_RATE;
                len -= ASCON_AEAD_RATE;
                resultLength += ASCON_AEAD_RATE;
            }
        }
        else
        {
            int available = m_bufferSizeDecrypt - m_bufPos;
            if (len < available)
            {
                System.arraycopy(inBytes, inOff, m_buf, m_bufPos, len);
                m_bufPos += len;
                return 0;
            }

            // NOTE: Need 'while' here because ASCON_AEAD_RATE < CRYPTO_ABYTES in some parameter sets
            while (m_bufPos >= ASCON_AEAD_RATE)
            {
                processBufferDecrypt(m_buf, 0, outBytes, outOff + resultLength);
                m_bufPos -= ASCON_AEAD_RATE;
                System.arraycopy(m_buf, ASCON_AEAD_RATE, m_buf, 0, m_bufPos);
                resultLength += ASCON_AEAD_RATE;

                available += ASCON_AEAD_RATE;
                if (len < available)
                {
                    System.arraycopy(inBytes, inOff, m_buf, m_bufPos, len);
                    m_bufPos += len;
                    return resultLength;
                }
            }

            available = ASCON_AEAD_RATE - m_bufPos;
            System.arraycopy(inBytes, inOff, m_buf, m_bufPos, available);
            inOff += available;
            len -= available;
            processBufferDecrypt(m_buf, 0, outBytes, outOff + resultLength);
            resultLength += ASCON_AEAD_RATE;
            //m_bufPos = 0;

            while (len >= m_bufferSizeDecrypt)
            {
                processBufferDecrypt(inBytes, inOff, outBytes, outOff + resultLength);
                inOff += ASCON_AEAD_RATE;
                len -= ASCON_AEAD_RATE;
                resultLength += ASCON_AEAD_RATE;
            }
        }

        System.arraycopy(inBytes, inOff, m_buf, 0, len);
        m_bufPos = len;

        return resultLength;
    }

    public int doFinal(byte[] outBytes, int outOff)
        throws IllegalStateException, InvalidCipherTextException, DataLengthException
    {
        boolean forEncryption = checkData();
        int resultLength;
        if (forEncryption)
        {
            resultLength = m_bufPos + CRYPTO_ABYTES;
            if (outOff + resultLength > outBytes.length)
            {
                throw new OutputLengthException("output buffer too short");
            }
            processFinalEncrypt(m_buf, 0, m_bufPos, outBytes, outOff);
            mac = new byte[CRYPTO_ABYTES];
            Pack.longToBigEndian(x3, mac, 0);
            Pack.longToBigEndian(x4, mac, 8);
            System.arraycopy(mac, 0, outBytes, outOff + m_bufPos, CRYPTO_ABYTES);
            reset(false);
        }
        else
        {
            if (m_bufPos < CRYPTO_ABYTES)
            {
                throw new InvalidCipherTextException("data too short");
            }
            m_bufPos -= CRYPTO_ABYTES;
            resultLength = m_bufPos;
            if (outOff + resultLength > outBytes.length)
            {
                throw new OutputLengthException("output buffer too short");
            }
            processFinalDecrypt(m_buf, 0, m_bufPos, outBytes, outOff);
            x3 ^= Pack.bigEndianToLong(m_buf, m_bufPos);
            x4 ^= Pack.bigEndianToLong(m_buf, m_bufPos + 8);
            if ((x3 | x4) != 0L)
            {
                throw new InvalidCipherTextException("mac check in " + getAlgorithmName() + " failed");
            }
            reset(true);
        }
        return resultLength;
    }

    public byte[] getMac()
    {
        return mac;
    }

    public int getUpdateOutputSize(int len)
    {
        int total = Math.max(0, len);
        switch (m_state)
        {
        case DecInit:
        case DecAad:
            total = Math.max(0, total - CRYPTO_ABYTES);
            break;
        case DecData:
        case DecFinal:
            total = Math.max(0, total + m_bufPos - CRYPTO_ABYTES);
            break;
        case EncData:
        case EncFinal:
            total += m_bufPos;
            break;
        default:
            break;
        }
        return total - total % ASCON_AEAD_RATE;
    }

    public int getOutputSize(int len)
    {
        int total = Math.max(0, len);

        switch (m_state)
        {
        case DecInit:
        case DecAad:
            return Math.max(0, total - CRYPTO_ABYTES);
        case DecData:
        case DecFinal:
            return Math.max(0, total + m_bufPos - CRYPTO_ABYTES);
        case EncData:
        case EncFinal:
            return total + m_bufPos + CRYPTO_ABYTES;
        default:
            return total + CRYPTO_ABYTES;
        }
    }

    public void reset()
    {
        reset(true);
    }

    private void reset(boolean clearMac)
    {
        if (clearMac)
        {
            mac = null;
        }
        Arrays.clear(m_buf);
        m_bufPos = 0;

        switch (m_state)
        {
        case DecInit:
        case EncInit:
            break;
        case DecAad:
        case DecData:
        case DecFinal:
            m_state = State.DecInit;
            break;
        case EncAad:
        case EncData:
        case EncFinal:
            m_state = State.EncFinal;
            return;
        default:
            throw new IllegalStateException(getAlgorithmName() + " needs to be initialized");
        }
        ascon_aeadinit();
        if (initialAssociatedText != null)
        {
            processAADBytes(initialAssociatedText, 0, initialAssociatedText.length);
        }
    }

    public int getKeyBytesSize()
    {
        return CRYPTO_KEYBYTES;
    }

    public int getIVBytesSize()
    {
        return CRYPTO_ABYTES;
    }
}
