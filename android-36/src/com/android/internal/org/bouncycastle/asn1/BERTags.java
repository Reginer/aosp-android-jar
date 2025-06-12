/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.asn1;

/**
 * @hide This class is not part of the Android public SDK API
 */
public interface BERTags
{
    // 0x00: Reserved for use by the encoding rules
    public static final int BOOLEAN             = 0x01;
    public static final int INTEGER             = 0x02;
    public static final int BIT_STRING          = 0x03;
    public static final int OCTET_STRING        = 0x04;
    public static final int NULL                = 0x05;
    public static final int OBJECT_IDENTIFIER   = 0x06;
    public static final int OBJECT_DESCRIPTOR   = 0x07;
    public static final int EXTERNAL            = 0x08;
    public static final int REAL                = 0x09;
    public static final int ENUMERATED          = 0x0a; // decimal 10
    public static final int EMBEDDED_PDV        = 0x0b; // decimal 11
    public static final int UTF8_STRING         = 0x0c; // decimal 12
    public static final int RELATIVE_OID        = 0x0d; // decimal 13
    public static final int TIME                = 0x0e;
    // 0x0f: Reserved for future editions of this Recommendation | International Standard
    public static final int SEQUENCE            = 0x10; // decimal 16
    public static final int SEQUENCE_OF         = 0x10; // for completeness - used to model a SEQUENCE of the same type.
    public static final int SET                 = 0x11; // decimal 17
    public static final int SET_OF              = 0x11; // for completeness - used to model a SET of the same type.
    public static final int NUMERIC_STRING      = 0x12; // decimal 18
    public static final int PRINTABLE_STRING    = 0x13; // decimal 19
    public static final int T61_STRING          = 0x14; // decimal 20
    public static final int VIDEOTEX_STRING     = 0x15; // decimal 21
    public static final int IA5_STRING          = 0x16; // decimal 22
    public static final int UTC_TIME            = 0x17; // decimal 23
    public static final int GENERALIZED_TIME    = 0x18; // decimal 24
    public static final int GRAPHIC_STRING      = 0x19; // decimal 25
    public static final int VISIBLE_STRING      = 0x1a; // decimal 26
    public static final int GENERAL_STRING      = 0x1b; // decimal 27
    public static final int UNIVERSAL_STRING    = 0x1c; // decimal 28
    public static final int UNRESTRICTED_STRING = 0x1d; // decimal 29
    public static final int BMP_STRING          = 0x1e; // decimal 30
    public static final int DATE                = 0x1f;
    public static final int TIME_OF_DAY         = 0x20;
    public static final int DATE_TIME           = 0x21;
    public static final int DURATION            = 0x22;
    public static final int OBJECT_IDENTIFIER_IRI = 0x23;
    public static final int RELATIVE_OID_IRI    = 0x24;
    // 0x25..: Reserved for addenda to this Recommendation | International Standard

    public static final int CONSTRUCTED         = 0x20; // decimal 32

    public static final int UNIVERSAL           = 0x00; // decimal 32
    public static final int APPLICATION         = 0x40; // decimal 64
    public static final int TAGGED              = 0x80; // decimal 128 - maybe should deprecate this.
    public static final int CONTEXT_SPECIFIC    = 0x80; // decimal 128
    public static final int PRIVATE             = 0xC0; // decimal 192

    public static final int FLAGS               = 0xE0;
}
