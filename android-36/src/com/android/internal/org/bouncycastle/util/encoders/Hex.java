/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.util.encoders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.android.internal.org.bouncycastle.util.Strings;

/**
 * Utility class for converting hex data to bytes and back again.
 * @hide This class is not part of the Android public SDK API
 */
public class Hex
{
    private static final HexEncoder encoder = new HexEncoder();

    public static String toHexString(
        byte[] data)
    {
        return toHexString(data, 0, data.length);
    }

    public static String toHexString(
        byte[] data,
        int    off,
        int    length)
    {
        byte[] encoded = encode(data, off, length);
        return Strings.fromByteArray(encoded);
    }

    /**
     * encode the input data producing a Hex encoded byte array.
     *
     * @return a byte array containing the Hex encoded data.
     */
    public static byte[] encode(
        byte[]    data)
    {
        return encode(data, 0, data.length);
    }

    /**
     * encode the input data producing a Hex encoded byte array.
     *
     * @return a byte array containing the Hex encoded data.
     */
    public static byte[] encode(
        byte[]    data,
        int       off,
        int       length)
    {
        ByteArrayOutputStream    bOut = new ByteArrayOutputStream();

        try
        {
            encoder.encode(data, off, length, bOut);
        }
        catch (Exception e)
        {
            throw new EncoderException("exception encoding Hex string: " + e.getMessage(), e);
        }

        return bOut.toByteArray();
    }

    /**
     * Hex encode the byte data writing it to the given output stream.
     *
     * @return the number of bytes produced.
     */
    public static int encode(
        byte[]         data,
        OutputStream   out)
        throws IOException
    {
        return encoder.encode(data, 0, data.length, out);
    }

    /**
     * Hex encode the byte data writing it to the given output stream.
     *
     * @return the number of bytes produced.
     */
    public static int encode(
        byte[]         data,
        int            off,
        int            length,
        OutputStream   out)
        throws IOException
    {
        return encoder.encode(data, off, length, out);
    }

    /**
     * decode the Hex encoded input data. It is assumed the input data is valid.
     *
     * @return a byte array representing the decoded data.
     */
    public static byte[] decode(
        byte[]    data)
    {
        ByteArrayOutputStream    bOut = new ByteArrayOutputStream();

        try
        {
            encoder.decode(data, 0, data.length, bOut);
        }
        catch (Exception e)
        {
            throw new DecoderException("exception decoding Hex data: " + e.getMessage(), e);
        }
        
        return bOut.toByteArray();
    }

    /**
     * decode the Hex encoded String data - whitespace will be ignored.
     *
     * @return a byte array representing the decoded data.
     */
    public static byte[] decode(
        String    data)
    {
        ByteArrayOutputStream    bOut = new ByteArrayOutputStream();

        try
        {
            encoder.decode(data, bOut);
        }
        catch (Exception e)
        {
            throw new DecoderException("exception decoding Hex string: " + e.getMessage(), e);
        }

        return bOut.toByteArray();
    }

    /**
     * decode the Hex encoded String data writing it to the given output stream,
     * whitespace characters will be ignored.
     *
     * @return the number of bytes produced.
     */
    public static int decode(
        String          data,
        OutputStream    out)
        throws IOException
    {
        return encoder.decode(data, out);
    }

    /**
     * Decode the hexadecimal-encoded string strictly i.e. any non-hexadecimal characters will be
     * considered an error.
     *
     * @return a byte array representing the decoded data.
     */
    public static byte[] decodeStrict(String str)
    {
        try
        {
            return encoder.decodeStrict(str, 0, str.length());
        }
        catch (Exception e)
        {
            throw new DecoderException("exception decoding Hex string: " + e.getMessage(), e);
        }
    }

    /**
     * Decode the hexadecimal-encoded string strictly i.e. any non-hexadecimal characters will be
     * considered an error.
     *
     * @return a byte array representing the decoded data.
     */
    public static byte[] decodeStrict(String str, int off, int len)
    {
        try
        {
            return encoder.decodeStrict(str, off, len);
        }
        catch (Exception e)
        {
            throw new DecoderException("exception decoding Hex string: " + e.getMessage(), e);
        }
    }
}
