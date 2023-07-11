/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony.uicc;

/**
 * {@hide}
 */
public interface IccConstants {
    // GSM SIM file ids from TS 51.011
    static final int EF_ADN = 0x6F3A;
    static final int EF_FDN = 0x6F3B;
    static final int EF_GID1 = 0x6F3E;
    static final int EF_GID2 = 0x6F3F;
    static final int EF_SDN = 0x6F49;
    static final int EF_EXT1 = 0x6F4A;
    static final int EF_EXT2 = 0x6F4B;
    static final int EF_EXT3 = 0x6F4C;
    static final int EF_EXT5 = 0x6F4E;
    static final int EF_EXT6 = 0x6FC8;   // Ext record for EF[MBDN]
    static final int EF_MWIS = 0x6FCA;
    static final int EF_MBDN = 0x6FC7;
    static final int EF_PNN = 0x6FC5;
    static final int EF_OPL = 0x6FC6;
    static final int EF_SPN = 0x6F46;
    static final int EF_SMS = 0x6F3C;
    static final int EF_ICCID = 0x2FE2;
    static final int EF_AD = 0x6FAD;
    static final int EF_MBI = 0x6FC9;
    static final int EF_MSISDN = 0x6F40;
    static final int EF_SPDI = 0x6FCD;
    static final int EF_SST = 0x6F38;
    static final int EF_CFIS = 0x6FCB;
    static final int EF_IMG = 0x4F20;

    // USIM SIM file ids from TS 131.102
    public static final int EF_PBR = 0x4F30;
    public static final int EF_LI = 0x6F05;

    // GSM SIM file ids from CPHS (phase 2, version 4.2) CPHS4_2.WW6
    static final int EF_MAILBOX_CPHS = 0x6F17;
    static final int EF_VOICE_MAIL_INDICATOR_CPHS = 0x6F11;
    static final int EF_CFF_CPHS = 0x6F13;
    static final int EF_SPN_CPHS = 0x6F14;
    static final int EF_SPN_SHORT_CPHS = 0x6F18;
    static final int EF_INFO_CPHS = 0x6F16;
    static final int EF_CSP_CPHS = 0x6F15;

    // CDMA RUIM file ids from 3GPP2 C.S0023-0
    static final int EF_CST = 0x6F32;
    static final int EF_RUIM_SPN =0x6F41;

    // ETSI TS.102.221
    static final int EF_PL = 0x2F05;
    // 3GPP2 C.S0065
    static final int EF_CSIM_LI = 0x6F3A;
    static final int EF_CSIM_SPN =0x6F41;
    static final int EF_CSIM_MDN = 0x6F44;
    static final int EF_CSIM_IMSIM = 0x6F22;
    static final int EF_CSIM_CDMAHOME = 0x6F28;
    static final int EF_CSIM_EPRL = 0x6F5A;
    static final int EF_CSIM_PRL = 0x6F30;
    // C.S0074-Av1.0 Section 4
    static final int EF_CSIM_MLPL = 0x4F20;
    static final int EF_CSIM_MSPL = 0x4F21;
    static final int EF_CSIM_MIPUPP = 0x6F4D;

    //ISIM access
    static final int EF_IMPU = 0x6F04;
    static final int EF_IMPI = 0x6F02;
    static final int EF_DOMAIN = 0x6F03;
    static final int EF_IST = 0x6F07;
    static final int EF_PCSCF = 0x6F09;
    static final int EF_PSI = 0x6FE5;

    //PLMN Selection Information w/ Access Technology TS 131.102
    static final int EF_PLMN_W_ACT = 0x6F60;
    static final int EF_OPLMN_W_ACT = 0x6F61;
    static final int EF_HPLMN_W_ACT = 0x6F62;

    //Equivalent Home and Forbidden PLMN Lists TS 131.102
    static final int EF_EHPLMN = 0x6FD9;
    static final int EF_FPLMN = 0x6F7B;

    // Last Roaming Selection Indicator
    static final int EF_LRPLMNSI = 0x6FDC;

    //Search interval for higher priority PLMNs
    static final int EF_HPPLMN = 0x6F31;

    static final String MF_SIM = "3F00";
    static final String DF_TELECOM = "7F10";
    static final String DF_PHONEBOOK = "5F3A";
    static final String DF_GRAPHICS = "5F50";
    static final String DF_GSM = "7F20";
    static final String DF_CDMA = "7F25";
    static final String DF_MMSS = "5F3C";

    //UICC access
    static final String DF_ADF = "7FFF";
}
