package com.jobson.smartconfig;

import android.content.Context;

import com.jobson.smartconfig.task.SmartConfigTaskParameter;
import com.jobson.smartconfig.task.ISmartConfigTaskParameter;
import com.jobson.smartconfig.task.__SmartConfigTask;

import java.util.List;

public class SmartConfigTask implements ISmartConfigTask {

	public __SmartConfigTask _mEsptouchTask;
	private ISmartConfigTaskParameter _mParameter;

	/**
	 * Constructor of SmartConfigTask
	 * 
	 * @param apSsid
	 *            the Ap's ssid
	 * @param apBssid
	 *            the Ap's bssid
	 * @param apPassword
	 *            the Ap's password
	 * @param isSsidHidden
	 *            whether the Ap's ssid is hidden
	 * @param context
	 *            the Context of the Application
	 */
	public SmartConfigTask(String apSsid, String apBssid, String apPassword,
						   boolean isSsidHidden, Context context) {
		_mParameter = new SmartConfigTaskParameter();
		_mEsptouchTask = new __SmartConfigTask(apSsid, apBssid, apPassword,
				context, _mParameter, isSsidHidden);
	}

	/**
	 * Constructor of SmartConfigTask
	 * 
	 * @param apSsid
	 *            the Ap's ssid
	 * @param apBssid
	 *            the Ap's bssid
	 * @param apPassword
	 *            the Ap's password
	 * @param isSsidHidden
	 *            whether the Ap's ssid is hidden
	 * @param timeoutMillisecond
	 *            (it should be >= 15000+6000) millisecond of total timeout
	 * @param context
	 *            the Context of the Application
	 */
	public SmartConfigTask(String apSsid, String apBssid, String apPassword,
						   boolean isSsidHidden, int timeoutMillisecond, Context context) {
		_mParameter = new SmartConfigTaskParameter();
		_mParameter.setWaitUdpTotalMillisecond(timeoutMillisecond);
		_mEsptouchTask = new __SmartConfigTask(apSsid, apBssid, apPassword,
				context, _mParameter, isSsidHidden);
	}

	@Override
	public void interrupt() {
		_mEsptouchTask.interrupt();
	}

	@Override
	public ISmartConfigResult executeForResult() throws RuntimeException {
		return _mEsptouchTask.executeForResult();
	}

	@Override
	public boolean isCancelled() {
		return _mEsptouchTask.isCancelled();
	}

	@Override
	public List<ISmartConfigResult> executeForResults(int expectTaskResultCount)
			throws RuntimeException {
		if (expectTaskResultCount <= 0) {
			expectTaskResultCount = Integer.MAX_VALUE;
		}
		return _mEsptouchTask.executeForResults(expectTaskResultCount);
	}

	@Override
	public void setEsptouchListener(ISmartConfigListener esptouchListener) {
		_mEsptouchTask.setEsptouchListener(esptouchListener);
	}
}
