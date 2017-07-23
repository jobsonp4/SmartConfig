package com.jobson.smartconfig.task;

import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.jobson.smartconfig.ISmartConfigResult;
import com.jobson.smartconfig.SmartConfigResult;
import com.jobson.smartconfig.ISmartConfigListener;
import com.jobson.smartconfig.protocol.SmartConfigGenerator;
import com.jobson.smartconfig.udp.UDPSocketClient;
import com.jobson.smartconfig.udp.UDPSocketServer;
import com.jobson.smartconfig.util.ByteUtil;
import com.jobson.smartconfig.util.EspNetUtil;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class __SmartConfigTask implements __ISmartConfigTask {

	/**
	 * one indivisible data contain 3 9bits info
	 */
	private static final int ONE_DATA_LEN = 3;

	private static final String TAG = "SmartConfigTask";

	private volatile List<ISmartConfigResult> mEsptouchResultList;
	private volatile boolean mIsSuc = false;
	private volatile boolean mIsInterrupt = false;
	private volatile boolean mIsExecuted = false;
	private final UDPSocketClient mSocketClient;
	private final UDPSocketServer mSocketServer;
	private final String mApSsid;
	private final String mApBssid;
	private final boolean mIsSsidHidden;
	private final String mApPassword;
	private final Context mContext;
	private AtomicBoolean mIsCancelled;
	private ISmartConfigTaskParameter mParameter;
	private volatile Map<String, Integer> mBssidTaskSucCountMap;
	private ISmartConfigListener mEsptouchListener;

	public __SmartConfigTask(String apSsid, String apBssid, String apPassword,
							 Context context, ISmartConfigTaskParameter parameter,
							 boolean isSsidHidden) {
		if (TextUtils.isEmpty(apSsid)) {
			throw new IllegalArgumentException(
					"the apSsid should be null or empty");
		}
		if (apPassword == null) {
			apPassword = "";
		}
		mContext = context;
		mApSsid = apSsid;
		mApBssid = apBssid;
		mApPassword = apPassword;
		mIsCancelled = new AtomicBoolean(false);
		mSocketClient = new UDPSocketClient();
		mParameter = parameter;
		mSocketServer = new UDPSocketServer(mParameter.getPortListening(),
				mParameter.getWaitUdpTotalMillisecond(), context);
		mIsSsidHidden = isSsidHidden;
		mEsptouchResultList = new ArrayList<ISmartConfigResult>();
		mBssidTaskSucCountMap = new HashMap<String, Integer>();
	}

	private void __putEsptouchResult(boolean isSuc, String bssid,
			InetAddress inetAddress) {
		synchronized (mEsptouchResultList) {
			// check whether the result receive enough UDP response
			boolean isTaskSucCountEnough = false;
			Integer count = mBssidTaskSucCountMap.get(bssid);
			if (count == null) {
				count = 0;
			}
			++count;
			if (__ISmartConfigTask.DEBUG) {
				Log.d(TAG, "__putEsptouchResult(): count = " + count);
			}
			mBssidTaskSucCountMap.put(bssid, count);
			isTaskSucCountEnough = count >= mParameter
					.getThresholdSucBroadcastCount();
			if (!isTaskSucCountEnough) {
				if (__ISmartConfigTask.DEBUG) {
					Log.d(TAG, "__putEsptouchResult(): count = " + count
							+ ", isn't enough");
				}
				return;
			}
			// check whether the result is in the mEsptouchResultList already
			boolean isExist = false;
			for (ISmartConfigResult esptouchResultInList : mEsptouchResultList) {
				if (esptouchResultInList.getBssid().equals(bssid)) {
					isExist = true;
					break;
				}
			}
			// only add the result who isn't in the mEsptouchResultList
			if (!isExist) {
				if (__ISmartConfigTask.DEBUG) {
					Log.d(TAG, "__putEsptouchResult(): put one more result");
				}
				final ISmartConfigResult esptouchResult = new SmartConfigResult(isSuc,
						bssid, inetAddress);
				mEsptouchResultList.add(esptouchResult);
				if (mEsptouchListener != null) {
					mEsptouchListener.onEsptouchResultAdded(esptouchResult);
				}
			}
		}
	}

	private List<ISmartConfigResult> __getEsptouchResultList() {
		synchronized (mEsptouchResultList) {
			if (mEsptouchResultList.isEmpty()) {
				SmartConfigResult esptouchResultFail = new SmartConfigResult(false,
						null, null);
				esptouchResultFail.setIsCancelled(mIsCancelled.get());
				mEsptouchResultList.add(esptouchResultFail);
			}
			
			return mEsptouchResultList;
		}
	}

	private synchronized void __interrupt() {
		if (!mIsInterrupt) {
			mIsInterrupt = true;
			mSocketClient.interrupt();
			mSocketServer.interrupt();
			// interrupt the current Thread which is used to wait for udp response
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void interrupt() {
		if (__ISmartConfigTask.DEBUG) {
			Log.d(TAG, "interrupt()");
		}
		mIsCancelled.set(true);
		__interrupt();
	}

	private void __listenAsyn(final int expectDataLen) {
		new Thread() {
			public void run() {
				if (__ISmartConfigTask.DEBUG) {
					Log.d(TAG, "__listenAsyn() start");
				}
				long startTimestamp = System.currentTimeMillis();
				byte[] apSsidAndPassword = ByteUtil.getBytesByString(mApSsid
						+ mApPassword);
				byte expectOneByte = (byte) (apSsidAndPassword.length + 9);
				if (__ISmartConfigTask.DEBUG) {
					Log.i(TAG, "expectOneByte: " + (0 + expectOneByte));
				}
				byte receiveOneByte = -1;
				byte[] receiveBytes = null;
				while (mEsptouchResultList.size() < mParameter
						.getExpectTaskResultCount() && !mIsInterrupt) {
					receiveBytes = mSocketServer
							.receiveSpecLenBytes(expectDataLen);
					if (receiveBytes != null) {
						receiveOneByte = receiveBytes[0];
					} else {
						receiveOneByte = -1;
					}
					if (receiveOneByte == expectOneByte) {
						if (__ISmartConfigTask.DEBUG) {
							Log.i(TAG, "receive correct broadcast");
						}
						// change the socket's timeout
						long consume = System.currentTimeMillis()
								- startTimestamp;
						int timeout = (int) (mParameter
								.getWaitUdpTotalMillisecond() - consume);
						if (timeout < 0) {
							if (__ISmartConfigTask.DEBUG) {
								Log.i(TAG, "esptouch timeout");
							}
							break;
						} else {
							if (__ISmartConfigTask.DEBUG) {
								Log.i(TAG, "mSocketServer's new timeout is "
										+ timeout + " milliseconds");
							}
							mSocketServer.setSoTimeout(timeout);
							if (__ISmartConfigTask.DEBUG) {
								Log.i(TAG, "receive correct broadcast");
							}
							if (receiveBytes != null) {
								String bssid = ByteUtil.parseBssid(
										receiveBytes,
										mParameter.getEsptouchResultOneLen(),
										mParameter.getEsptouchResultMacLen());
								InetAddress inetAddress = EspNetUtil
										.parseInetAddr(
												receiveBytes,
												mParameter.getEsptouchResultOneLen()
												+ mParameter.getEsptouchResultMacLen(),
												mParameter.getEsptouchResultIpLen());
								__putEsptouchResult(true, bssid, inetAddress);
							}
						}
					} else {
						if (__ISmartConfigTask.DEBUG) {
							Log.i(TAG, "receive rubbish message, just ignore");
						}
					}
				}
				mIsSuc = mEsptouchResultList.size() >= mParameter
						.getExpectTaskResultCount();
				__SmartConfigTask.this.__interrupt();
				if (__ISmartConfigTask.DEBUG) {
					Log.d(TAG, "__listenAsyn() finish");
				}
			}
		}.start();
	}

	private boolean __execute(ISmartConfigGenerator generator) {

		long startTime = System.currentTimeMillis();
		long currentTime = startTime;
		long lastTime = currentTime - mParameter.getTimeoutTotalCodeMillisecond();

		byte[][] gcBytes2 = generator.getGCBytes2();
		byte[][] dcBytes2 = generator.getDCBytes2();

		int index = 0;
		while (!mIsInterrupt) {
			if (currentTime - lastTime >= mParameter.getTimeoutTotalCodeMillisecond()) {
				if (__ISmartConfigTask.DEBUG) {
					Log.d(TAG, "send gc code ");
				}
				// send guide code
				while (!mIsInterrupt
						&& System.currentTimeMillis() - currentTime < mParameter
								.getTimeoutGuideCodeMillisecond()) {
					mSocketClient.sendData(gcBytes2,
							mParameter.getTargetHostname(),
							mParameter.getTargetPort(),
							mParameter.getIntervalGuideCodeMillisecond());
					// check whether the udp is send enough time
					if (System.currentTimeMillis() - startTime > mParameter.getWaitUdpSendingMillisecond()) {
						break;
					}
				}
				lastTime = currentTime;
			} else {
				mSocketClient.sendData(dcBytes2, index, ONE_DATA_LEN,
						mParameter.getTargetHostname(),
						mParameter.getTargetPort(),
						mParameter.getIntervalDataCodeMillisecond());
				index = (index + ONE_DATA_LEN) % dcBytes2.length;
			}
			currentTime = System.currentTimeMillis();
			// check whether the udp is send enough time
			if (currentTime - startTime > mParameter.getWaitUdpSendingMillisecond()) {
				break;
			}
		}

		return mIsSuc;
	}

	private void __checkTaskValid() {
		// !!!NOTE: the esptouch task could be executed only once
		if (this.mIsExecuted) {
			throw new IllegalStateException(
					"the Esptouch task could be executed only once");
		}
		this.mIsExecuted = true;
	}

	@Override
	public ISmartConfigResult executeForResult() throws RuntimeException {
		return executeForResults(1).get(0);
	}

	@Override
	public boolean isCancelled() {
		return this.mIsCancelled.get();
	}

	@Override
	public List<ISmartConfigResult> executeForResults(int expectTaskResultCount)
			throws RuntimeException {
		__checkTaskValid();

		mParameter.setExpectTaskResultCount(expectTaskResultCount);

		if (__ISmartConfigTask.DEBUG) {
			Log.d(TAG, "execute()");
		}
		if (Looper.myLooper() == Looper.getMainLooper()) {
			throw new RuntimeException(
					"Don't call the esptouch Task at Main(UI) thread directly.");
		}
		InetAddress localInetAddress = EspNetUtil.getLocalInetAddress(mContext);
		if (__ISmartConfigTask.DEBUG) {
			Log.i(TAG, "localInetAddress: " + localInetAddress);
		}
		// generator the esptouch byte[][] to be transformed, which will cost
		// some time(maybe a bit much)
		ISmartConfigGenerator generator = new SmartConfigGenerator(mApSsid, mApBssid,
				mApPassword, localInetAddress, mIsSsidHidden);
		// listen the esptouch result asyn
		__listenAsyn(mParameter.getEsptouchResultTotalLen());
		boolean isSuc = false;
		for (int i = 0; i < mParameter.getTotalRepeatTime(); i++) {
			isSuc = __execute(generator);
			if (isSuc) {
				return __getEsptouchResultList();
			}
		}

		if (!mIsInterrupt) {
			// wait the udp response without sending udp broadcast
			try {
				Thread.sleep(mParameter.getWaitUdpReceivingMillisecond());
			} catch (InterruptedException e) {
				// receive the udp broadcast or the user interrupt the task
				if (this.mIsSuc) {
					return __getEsptouchResultList();
				} else {
					this.__interrupt();
					return __getEsptouchResultList();
				}
			}
			this.__interrupt();
		}
		
		return __getEsptouchResultList();
	}

	@Override
	public void setEsptouchListener(ISmartConfigListener esptouchListener) {
		mEsptouchListener = esptouchListener;
	}

}
