/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.org.bouncycastle.asn1;

import java.io.IOException;
import java.io.InputStream;

import com.android.org.bouncycastle.util.io.Streams;

/**
 * A parser for indefinite-length BIT STRINGs.
 * 
 * @deprecated Check for 'ASN1BitStringParser' instead 
 * @hide This class is not part of the Android public SDK API
 */
public class BERBitStringParser
    implements ASN1BitStringParser
{
    private ASN1StreamParser _parser;

    private ConstructedBitStream _bitStream;

    BERBitStringParser(
        ASN1StreamParser parser)
    {
        _parser = parser;
    }

    public InputStream getOctetStream() throws IOException
    {
        return _bitStream = new ConstructedBitStream(_parser, true);
    }

    public InputStream getBitStream() throws IOException
    {
        return _bitStream = new ConstructedBitStream(_parser, false);
    }

    public int getPadBits()
    {
        return _bitStream.getPadBits();
    }

    public ASN1Primitive getLoadedObject()
        throws IOException
    {
        return parse(_parser);
    }

    public ASN1Primitive toASN1Primitive()
    {
        try
        {
            return getLoadedObject();
        }
        catch (IOException e)
        {
            throw new ASN1ParsingException("IOException converting stream to byte array: " + e.getMessage(), e);
        }
    }

    static BERBitString parse(ASN1StreamParser sp) throws IOException
    {
        ConstructedBitStream bitStream = new ConstructedBitStream(sp, false);
        byte[] data = Streams.readAll(bitStream);
        int padBits = bitStream.getPadBits();
        return new BERBitString(data, padBits);
    }
}
