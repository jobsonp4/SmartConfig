package com.jobson.smartconfig.protocol;

import java.net.InetAddress;

import com.jobson.smartconfig.task.ISmartConfigGenerator;
import com.jobson.smartconfig.util.ByteUtil;

public class SmartConfigGenerator implements ISmartConfigGenerator {

	private final byte[][] mGcBytes2;
	private final byte[][] mDcBytes2;

	/**
	 * Constructor of SmartConfigGenerator, it will cost some time(maybe a bit
	 * much)
	 * 
	 * @param apSsid
	 *            the Ap's ssid
	 * @param apBssid
	 *            the Ap's bssid
	 * @param apPassword
	 *            the Ap's password
	 * @param inetAddress
	 *            the phone's or pad's local ip address allocated by Ap
	 * @param isSsidHidden
	 *            whether the Ap's ssid is hidden
	 */
	public SmartConfigGenerator(String apSsid, String apBssid, String apPassword,
								InetAddress inetAddress, boolean isSsidHiden) {
		// generate guide code
		GuideCode gc = new GuideCode();
		char[] gcU81 = gc.getU8s();
		mGcBytes2 = new byte[gcU81.length][];

		for (int i = 0; i < mGcBytes2.length; i++) {
			mGcBytes2[i] = ByteUtil.genSpecBytes(gcU81[i]);
		}

		// generate data code
		DatumCode dc = new DatumCode(apSsid, apBssid, apPassword, inetAddress,
				isSsidHiden);
		char[] dcU81 = dc.getU8s();
		mDcBytes2 = new byte[dcU81.length][];

		for (int i = 0; i < mDcBytes2.length; i++) {
			mDcBytes2[i] = ByteUtil.genSpecBytes(dcU81[i]);
		}
	}

	@Override
	public byte[][] getGCBytes2() {
		return mGcBytes2;
	}

	@Override
	public byte[][] getDCBytes2() {
		return mDcBytes2;
	}

}
