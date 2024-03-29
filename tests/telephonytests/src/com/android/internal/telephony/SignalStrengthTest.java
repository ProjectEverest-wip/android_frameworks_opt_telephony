/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.internal.telephony;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.os.Parcel;
import android.os.PersistableBundle;
import android.platform.test.flag.junit.SetFlagsRule;
import android.telephony.CarrierConfigManager;
import android.telephony.CellInfo;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.CellSignalStrengthTdscdma;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;

import androidx.test.filters.SmallTest;

import com.android.internal.telephony.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Unit tests for {@link IpSecConfig}. */
@SmallTest
@RunWith(JUnit4.class)
public class SignalStrengthTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private static final int[] DEFAULT_LTE_RSRP_THRESHOLDS = {
            -128,  // SIGNAL_STRENGTH_POOR
            -118,  // SIGNAL_STRENGTH_MODERATE
            -108,  // SIGNAL_STRENGTH_GOOD
            -98 }; // SIGNAL_STRENGTH_GREAT

    private static final int[] DEFAULT_LTE_RSRQ_THRESHOLDS = {
            -19,   // SIGNAL_STRENGTH_POOR
            -17,   // SIGNAL_STRENGTH_MODERATE
            -14,   // SIGNAL_STRENGTH_GOOD
            -12 }; // SIGNAL_STRENGTH_GREAT

    private static final int[] DEFAULT_LTE_RSSNR_THRESHOLDS = {
            -3,   // SIGNAL_STRENGTH_POOR
            1,    // SIGNAL_STRENGTH_MODERATE
            5,    // SIGNAL_STRENGTH_GOOD
            13 }; // SIGNAL_STRENGTH_GREAT

    private static final int[] DEFAULT_5G_NR_SSRSRP_THRESHOLDS = {
            -125,  // SIGNAL_STRENGTH_POOR
            -115,  // SIGNAL_STRENGTH_MODERATE
            -105,  // SIGNAL_STRENGTH_GOOD
            -95 }; // SIGNAL_STRENGTH_GREAT

    private static final int[] DEFAULT_NTN_LTE_RSRP_THRESHOLDS = {
            -118,  // SIGNAL_STRENGTH_POOR
            -108,  // SIGNAL_STRENGTH_MODERATE
            -98,  // SIGNAL_STRENGTH_GOOD
            -88 }; // SIGNAL_STRENGTH_GREAT

    private static final int[] DEFAULT_NTN_LTE_RSRQ_THRESHOLDS = {
            -17,   // SIGNAL_STRENGTH_POOR
            -14,   // SIGNAL_STRENGTH_MODERATE
            -12,   // SIGNAL_STRENGTH_GOOD
            -10 }; // SIGNAL_STRENGTH_GREAT

    private static final int[] DEFAULT_NTN_LTE_RSSNR_THRESHOLDS = {
            1,   // SIGNAL_STRENGTH_POOR
            5,    // SIGNAL_STRENGTH_MODERATE
            13,    // SIGNAL_STRENGTH_GOOD
            17 }; // SIGNAL_STRENGTH_GREAT

    // RSRP, RSSNR thresholds boundaries
    private static final int MIN_RSRP = -140;
    private static final int MIN_RSRQ = -34;
    private static final int MAX_RSRQ = 3;
    private static final int MIN_RSSNR = -20;
    private static final int MAX_RSSNR = 30;

    // Default NTN & TN LTE thresholds's index
    private static final int INDEX_SIGNAL_STRENGTH_POOR = 0;
    private static final int INDEX_SIGNAL_STRENGTH_MODERATE = 1;
    private static final int INDEX_SIGNAL_STRENGTH_GOOD = 2;
    private static final int INDEX_SIGNAL_STRENGTH_GREAT = 3;

    @Before
    public void setUp() {
        mSetFlagsRule.enableFlags(Flags.FLAG_CARRIER_ENABLED_SATELLITE_FLAG);
    }

    @Test
    public void testDefaults() throws Exception {
        SignalStrength s = new SignalStrength();
        assertEquals(CellInfo.UNAVAILABLE, s.getCdmaDbm());
        assertEquals(CellInfo.UNAVAILABLE, s.getCdmaEcio());
        assertEquals(CellInfo.UNAVAILABLE, s.getEvdoDbm());
        assertEquals(CellInfo.UNAVAILABLE, s.getEvdoEcio());
        assertEquals(CellInfo.UNAVAILABLE, s.getEvdoSnr());
        assertEquals(CellInfo.UNAVAILABLE, s.getGsmBitErrorRate());
        // getGsmSignalStrength is an oddball because internally it actually returns an AsuLevel
        // rather than a dBm value. For backwards compatibility reasons, this is left as the
        // RSSI ASU value [0-31], 99.
        assertEquals(99, s.getGsmSignalStrength());
        assertEquals(true, s.isGsm());
    }

    @Test
    public void testParcelUnparcel() throws Exception {
        assertParcelingIsLossless(new SignalStrength());

        ArrayList<Byte> NrCqiReport = new ArrayList<>(
                Arrays.asList((byte) 3, (byte) 2 , (byte) 1));
        SignalStrength s = new SignalStrength(
                new CellSignalStrengthCdma(-93, -132, -89, -125, 5),
                new CellSignalStrengthGsm(-79, 2, 5),
                new CellSignalStrengthWcdma(-94, 4, -102, -5),
                new CellSignalStrengthTdscdma(-95, 2, -103),
                new CellSignalStrengthLte(-85, -91, -6, -10, 1, 12, 1),
                new CellSignalStrengthNr(-91, -6, 3, 1, NrCqiReport, -80, -7, 4, 1));
        assertParcelingIsLossless(s);

        PersistableBundle bundle = new PersistableBundle();
        bundle.putIntArray(
                CarrierConfigManager.KEY_LTE_RSRQ_THRESHOLDS_INT_ARRAY,
                DEFAULT_LTE_RSRQ_THRESHOLDS);
        bundle.putIntArray(
                CarrierConfigManager.KEY_LTE_RSRP_THRESHOLDS_INT_ARRAY,
                DEFAULT_LTE_RSRP_THRESHOLDS);
        bundle.putIntArray(
                CarrierConfigManager.KEY_LTE_RSSNR_THRESHOLDS_INT_ARRAY,
                DEFAULT_LTE_RSSNR_THRESHOLDS);
        bundle.putIntArray(
                CarrierConfigManager.KEY_5G_NR_SSRSRP_THRESHOLDS_INT_ARRAY,
                DEFAULT_5G_NR_SSRSRP_THRESHOLDS);

        s.updateLevel(bundle, null);
        assertParcelingIsLossless(s);
    }

    private void assertParcelingIsLossless(SignalStrength ssi) throws Exception {
        Parcel p = Parcel.obtain();
        ssi.writeToParcel(p, 0);
        p.setDataPosition(0);
        SignalStrength sso = SignalStrength.CREATOR.createFromParcel(p);
        assertTrue(sso.equals(ssi));
    }

    @Test
    public void testGetCellSignalStrengths() throws Exception {
        CellSignalStrengthLte lte = new CellSignalStrengthLte(-85, -91, -6, -10, 1, 12, 1);
        CellSignalStrengthGsm gsm = new CellSignalStrengthGsm(-79, 2, 5);
        CellSignalStrengthCdma cdma = new CellSignalStrengthCdma(-93, -132, -89, -125, 5);
        CellSignalStrengthWcdma wcdma = new CellSignalStrengthWcdma(-94, 4, -102, -5);
        CellSignalStrengthTdscdma tdscdma = new CellSignalStrengthTdscdma(-95, 2, -103);

        // Test that a single object is properly stored and returned by getCellSignalStrengths()
        SignalStrength s = new SignalStrength(
                new CellSignalStrengthCdma(),
                gsm,
                new CellSignalStrengthWcdma(),
                new CellSignalStrengthTdscdma(),
                new CellSignalStrengthLte(),
                new CellSignalStrengthNr());

        List<CellSignalStrength> css = s.getCellSignalStrengths();
        assertEquals(1, css.size());
        assertTrue(gsm.equals(css.get(0)));

        // Test that a multiple objects are properly stored and returned by getCellSignalStrengths()
        s = new SignalStrength(
                cdma,
                new CellSignalStrengthGsm(),
                new CellSignalStrengthWcdma(),
                new CellSignalStrengthTdscdma(),
                lte,
                new CellSignalStrengthNr());

        css = s.getCellSignalStrengths();
        assertEquals(2, css.size());
        assertTrue(css.contains(cdma));
        assertTrue(css.contains(lte));
    }

    private static SignalStrength createSignalStrengthLteReportRsrq(int lteRsrp, int lteRsrq) {

        CellSignalStrengthLte lte = new CellSignalStrengthLte(
                -89,                   // rssi
                lteRsrp,               // rsrp
                lteRsrq,               // rsrq
                -25,                   // rssnr
                CellInfo.UNAVAILABLE,  // cqiTableIndex
                CellInfo.UNAVAILABLE,  // cqi
                CellInfo.UNAVAILABLE); // timingAdvance

        SignalStrength signalStrength = new SignalStrength(
                new CellSignalStrengthCdma(),
                new CellSignalStrengthGsm(),
                new CellSignalStrengthWcdma(),
                new CellSignalStrengthTdscdma(),
                lte,
                new CellSignalStrengthNr());

        PersistableBundle bundle = new PersistableBundle();
        bundle.putInt(
                CarrierConfigManager.KEY_PARAMETERS_USED_FOR_LTE_SIGNAL_BAR_INT,
                CellSignalStrengthLte.USE_RSRP | CellSignalStrengthLte.USE_RSRQ);
        bundle.putIntArray(
                CarrierConfigManager.KEY_LTE_RSRQ_THRESHOLDS_INT_ARRAY,
                DEFAULT_LTE_RSRQ_THRESHOLDS);
        bundle.putIntArray(
                CarrierConfigManager.KEY_LTE_RSRP_THRESHOLDS_INT_ARRAY,
                DEFAULT_LTE_RSRP_THRESHOLDS);
        signalStrength.updateLevel(bundle, null);
        return signalStrength;
    }

    private static SignalStrength createSignalStrengthLteReportRssnr(int lteRsrp, int lteRssnr) {

        CellSignalStrengthLte lte = new CellSignalStrengthLte(
                -89,                   // rssi
                lteRsrp,               // rsrp
                15,                    // rsrq
                lteRssnr,              // rssnr
                CellInfo.UNAVAILABLE,  // cqiTableIndex
                CellInfo.UNAVAILABLE,  // cqi
                CellInfo.UNAVAILABLE); // timingAdvance

        SignalStrength signalStrength = new SignalStrength(
                new CellSignalStrengthCdma(),
                new CellSignalStrengthGsm(),
                new CellSignalStrengthWcdma(),
                new CellSignalStrengthTdscdma(),
                lte,
                new CellSignalStrengthNr());

        PersistableBundle bundle = new PersistableBundle();
        bundle.putInt(
                CarrierConfigManager.KEY_PARAMETERS_USED_FOR_LTE_SIGNAL_BAR_INT,
                CellSignalStrengthLte.USE_RSRP | CellSignalStrengthLte.USE_RSSNR);
        bundle.putIntArray(
                CarrierConfigManager.KEY_LTE_RSRP_THRESHOLDS_INT_ARRAY,
                DEFAULT_LTE_RSRP_THRESHOLDS);
        bundle.putIntArray(
                CarrierConfigManager.KEY_LTE_RSSNR_THRESHOLDS_INT_ARRAY,
                DEFAULT_LTE_RSSNR_THRESHOLDS);
        signalStrength.updateLevel(bundle, null);
        return signalStrength;
    }

    private static SignalStrength createSignalStrengthLteReport(int lteRsrp, int lteRsrq,
            int lteRssnr, boolean isNTN) {
        CellSignalStrengthLte lte = new CellSignalStrengthLte(
                -89,               // rssi
                lteRsrp,               // rsrp
                lteRsrq,               // rsrq
                lteRssnr,              // rssnr
                CellInfo.UNAVAILABLE,  // cqiTableIndex
                CellInfo.UNAVAILABLE,  // cqi
                CellInfo.UNAVAILABLE); // timingAdvance

        SignalStrength signalStrength = new SignalStrength(
                new CellSignalStrengthCdma(),
                new CellSignalStrengthGsm(),
                new CellSignalStrengthWcdma(),
                new CellSignalStrengthTdscdma(),
                lte,
                new CellSignalStrengthNr());

        PersistableBundle bundle = new PersistableBundle();
        bundle.putInt(
                CarrierConfigManager.KEY_PARAMETERS_USED_FOR_LTE_SIGNAL_BAR_INT,
                CellSignalStrengthLte.USE_RSRP | CellSignalStrengthLte.USE_RSRQ
                        | CellSignalStrengthLte.USE_RSSNR);
        bundle.putIntArray(CarrierConfigManager.KEY_LTE_RSRP_THRESHOLDS_INT_ARRAY,
                DEFAULT_LTE_RSRP_THRESHOLDS);
        bundle.putIntArray(CarrierConfigManager.KEY_LTE_RSRQ_THRESHOLDS_INT_ARRAY,
                DEFAULT_LTE_RSRQ_THRESHOLDS);
        bundle.putIntArray(CarrierConfigManager.KEY_LTE_RSSNR_THRESHOLDS_INT_ARRAY,
                DEFAULT_LTE_RSSNR_THRESHOLDS);
        bundle.putInt(
                CarrierConfigManager.KEY_PARAMETERS_USED_FOR_NTN_LTE_SIGNAL_BAR_INT,
                CellSignalStrengthLte.USE_RSRP | CellSignalStrengthLte.USE_RSRQ
                        | CellSignalStrengthLte.USE_RSSNR);
        bundle.putIntArray(
                CarrierConfigManager.KEY_NTN_LTE_RSRP_THRESHOLDS_INT_ARRAY,
                DEFAULT_NTN_LTE_RSRP_THRESHOLDS);
        bundle.putIntArray(
                CarrierConfigManager.KEY_NTN_LTE_RSRQ_THRESHOLDS_INT_ARRAY,
                DEFAULT_NTN_LTE_RSRQ_THRESHOLDS);
        bundle.putIntArray(
                CarrierConfigManager.KEY_NTN_LTE_RSSNR_THRESHOLDS_INT_ARRAY,
                DEFAULT_NTN_LTE_RSSNR_THRESHOLDS);
        ServiceState serviceState = new ServiceState();
        NetworkRegistrationInfo nri = new NetworkRegistrationInfo.Builder()
                .setIsNonTerrestrialNetwork(isNTN)
                .build();
        serviceState.addNetworkRegistrationInfo(nri);
        signalStrength.updateLevel(bundle, serviceState);
        return signalStrength;
    }

    @Test
    public void testValidateInput() throws Exception {

        SignalStrength ss;

        // Input value of RSRQ: 4[dB]
        ss = createSignalStrengthLteReportRsrq(-60, 4);
        assertEquals(SignalStrength.INVALID, ss.getLteRsrq());

        // Input value of RSRQ: 3[dB]
        ss = createSignalStrengthLteReportRsrq(-60, 3);
        assertEquals(3, ss.getLteRsrq());

        // Input value of RSRQ: -34[dB]
        ss = createSignalStrengthLteReportRsrq(-60, -34);
        assertEquals(-34, ss.getLteRsrq());

        // Input value of RSRQ: -35[dB]
        ss = createSignalStrengthLteReportRsrq(-60, -35);
        assertEquals(SignalStrength.INVALID, ss.getLteRsrq());

        // Input value of RSSNR: 31[dB]
        ss = createSignalStrengthLteReportRssnr(-60, 31);
        assertEquals(SignalStrength.INVALID, ss.getLteRssnr());

        // Input value of RSSNR: 30[dB]
        ss = createSignalStrengthLteReportRssnr(-60, 30);
        assertEquals(30, ss.getLteRssnr());

        // Input value of RSSNR: -20[dB]
        ss = createSignalStrengthLteReportRssnr(60, -20);
        assertEquals(-20, ss.getLteRssnr());

        // Input value of RSSNR: -21[dB]
        ss = createSignalStrengthLteReportRssnr(60, -21);
        assertEquals(SignalStrength.INVALID, ss.getLteRssnr());

        // Test for NTN LTE RSRQ Thresholds based on Boundaries [-34 dB, 3 dB]
        boolean isNTN = true;
        int rsrp = DEFAULT_NTN_LTE_RSRP_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        int rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        int rsrq = MAX_RSRQ + 1;
        ss = createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN);
        assertEquals(SignalStrength.INVALID, ss.getLteRsrq());

        rsrq = MAX_RSRQ;
        ss = createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN);
        assertEquals(3, ss.getLteRsrq());

        rsrq = MIN_RSRQ - 1;
        ss = createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN);
        assertEquals(SignalStrength.INVALID, ss.getLteRsrq());

        rsrq = MIN_RSRQ;
        ss = createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN);
        assertEquals(-34, ss.getLteRsrq());

        // Test for NTN LTE RSSNR Thresholds based on Boundaries [-20 dBm, 30 dBm]
        rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT]; // or 3 ?
        rssnr = MAX_RSSNR + 1;
        ss = createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN);
        assertEquals(SignalStrength.INVALID, ss.getLteRssnr());

        rssnr = MAX_RSSNR;
        ss = createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN);
        assertEquals(30, ss.getLteRssnr());

        rssnr = MIN_RSSNR - 1;
        ss = createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN);
        assertEquals(SignalStrength.INVALID, ss.getLteRssnr());

        rssnr = MIN_RSSNR;
        ss = createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN);
        assertEquals(-20, ss.getLteRssnr());
    }

    @Test
    public void testRsrqThresholds_rsrp_great() throws Exception {
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReportRsrq(-98, -34).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReportRsrq(-98, -19).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReportRsrq(-98, -17).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GOOD,
                createSignalStrengthLteReportRsrq(-98, -14).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GREAT,
                createSignalStrengthLteReportRsrq(-98, -12).getLteLevel());

        // When NTN is connected, check the signal strength
        boolean isNTN = true;
        int rsrp = DEFAULT_NTN_LTE_RSRP_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        int rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        int rsrq = MIN_RSRQ;
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GOOD,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GREAT,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());

        // When NTN is disconnected, check the signal strength
        isNTN = false;
        rsrp = DEFAULT_LTE_RSRP_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        rsrq = MIN_RSRQ;
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GOOD,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GREAT,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
    }

    @Test
    public void testRsrqThresholds_rsrp_good() throws Exception {
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReportRsrq(-108, -34).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReportRsrq(-108, -19).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReportRsrq(-108, -17).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GOOD,
                createSignalStrengthLteReportRsrq(-108, -14).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GOOD,
                createSignalStrengthLteReportRsrq(-108, -12).getLteLevel());

        // When NTN is connected, check the signal strength
        boolean isNTN = true;
        int rsrp = DEFAULT_NTN_LTE_RSRP_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        int rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        int rsrq = MIN_RSRQ;
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GOOD,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GOOD,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());

        // When NTN is disconnected, check the signal strength
        isNTN = false;
        rsrp = DEFAULT_LTE_RSRP_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        rsrq = MIN_RSRQ;
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GOOD,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GOOD,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
    }

    @Test
    public void testRsrqThresholds_rsrp_moderate() throws Exception {
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReportRsrq(-118, -34).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReportRsrq(-118, -19).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReportRsrq(-118, -17).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReportRsrq(-118, -14).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReportRsrq(-118, -12).getLteLevel());

        // When NTN is connected, check the signal strength
        boolean isNTN = true;
        int rsrp = DEFAULT_NTN_LTE_RSRP_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        int rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        int rsrq = MIN_RSRQ;
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());

        // When NTN is disconnected, check the signal strength
        isNTN = false;
        rsrp = DEFAULT_LTE_RSRP_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        rsrq = MIN_RSRQ;
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
    }

    @Test
    public void testRsrqThresholds_rsrp_poor() throws Exception {
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReportRsrq(-128, -34).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReportRsrq(-128, -19).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReportRsrq(-128, -17).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReportRsrq(-128, -14).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReportRsrq(-128, -12).getLteLevel());

        // When NTN is connected, check the signal strength
        boolean isNTN = true;
        int rsrp = DEFAULT_NTN_LTE_RSRP_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        int rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        int rsrq = MIN_RSRQ;
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());

        // When NTN is disconnected, check the signal strength
        isNTN = false;
        rsrp = DEFAULT_LTE_RSRP_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        rsrq = MIN_RSRQ;
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
    }

    @Test
    public void testRsrqThresholds_rsrp_unknown() throws Exception {
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReportRsrq(-138, -34).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReportRsrq(-138, -19).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReportRsrq(-138, -17).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReportRsrq(-138, -14).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReportRsrq(-138, -12).getLteLevel());

        // When NTN is connected, check the signal strength
        boolean isNTN = true;
        int rsrp = MIN_RSRP;
        int rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        int rsrq = MIN_RSRQ;
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());

        // When NTN is disconnected, check the signal strength
        isNTN = false;
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        rsrq = MIN_RSRQ;
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
    }

    @Test
    public void testRssnrThresholds_rsrp_great() throws Exception {
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReportRssnr(-98, -20).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReportRssnr(-98, -3).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReportRssnr(-98, 1).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GOOD,
                createSignalStrengthLteReportRssnr(-98, 5).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GREAT,
                createSignalStrengthLteReportRssnr(-98, 13).getLteLevel());

        // When NTN is connected, check the signal strength
        boolean isNTN = true;
        int rsrp = DEFAULT_NTN_LTE_RSRP_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        int rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        int rssnr = MIN_RSSNR;
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GOOD,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GREAT,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());

        // When NTN is disconnected, check the signal strength
        isNTN = false;
        rsrp = DEFAULT_LTE_RSRP_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        rssnr = MIN_RSSNR;
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GOOD,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GREAT,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
    }

    @Test
    public void testRssnrThresholds_rsrp_good() throws Exception {
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReportRssnr(-108, -20).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReportRssnr(-108, -3).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReportRssnr(-108, 1).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GOOD,
                createSignalStrengthLteReportRssnr(-108, 5).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GOOD,
                createSignalStrengthLteReportRssnr(-108, 13).getLteLevel());

        // When NTN is connected, check the signal strength
        boolean isNTN = true;
        int rsrp = DEFAULT_NTN_LTE_RSRP_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        int rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        int rssnr = MIN_RSSNR;
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GOOD,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GOOD,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());

        // When NTN is disconnected, check the signal strength
        isNTN = false;
        rsrp = DEFAULT_LTE_RSRP_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        rssnr = MIN_RSSNR;
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GOOD,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_GOOD,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
    }

    @Test
    public void testRssnrThresholds_rsrp_moderate() throws Exception {
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReportRssnr(-118, -20).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReportRssnr(-118, -3).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReportRssnr(-118, 1).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReportRssnr(-118, 5).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReportRssnr(-118, 13).getLteLevel());

        // When NTN is connected, check the signal strength
        boolean isNTN = true;
        int rsrp = DEFAULT_NTN_LTE_RSRP_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        int rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        int rssnr = MIN_RSSNR;
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());

        // When NTN is disconnected, check the signal strength
        isNTN = false;
        rsrp = DEFAULT_LTE_RSRP_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        rssnr = MIN_RSSNR;
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_MODERATE,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
    }

    @Test
    public void testRssnrThresholds_rsrp_poor() throws Exception {
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReportRssnr(-128, -20).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReportRssnr(-128, -3).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReportRssnr(-128, 1).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReportRssnr(-128, 5).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReportRssnr(-128, 13).getLteLevel());

        // When NTN is connected, check the signal strength
        boolean isNTN = true;
        int rsrp = DEFAULT_NTN_LTE_RSRP_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        int rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        int rssnr = MIN_RSSNR;
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());

        // When NTN is disconnected, check the signal strength
        isNTN = false;
        rsrp = DEFAULT_LTE_RSRP_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        rssnr = MIN_RSSNR;
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_POOR,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
    }

    @Test
    public void testRssnrThresholds_rsrp_unknown() throws Exception {
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReportRssnr(-138, -20).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReportRssnr(-138, -3).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReportRssnr(-138, 1).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReportRssnr(-138, 5).getLteLevel());
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReportRssnr(-138, 13).getLteLevel());

        // When NTN is connected, check the signal strength
        boolean isNTN = true;
        int rsrp = MIN_RSRP;
        int rsrq = DEFAULT_NTN_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        int rssnr = MIN_RSSNR;
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_NTN_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());

        // When NTN is disconnected, check the signal strength
        isNTN = false;
        rsrq = DEFAULT_LTE_RSRQ_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        rssnr = MIN_RSSNR;
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_POOR];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_MODERATE];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GOOD];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
        rssnr = DEFAULT_LTE_RSSNR_THRESHOLDS[INDEX_SIGNAL_STRENGTH_GREAT];
        assertEquals(SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                createSignalStrengthLteReport(rsrp, rsrq, rssnr, isNTN).getLteLevel());
    }
}

