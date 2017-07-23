package com.jobson.smartconfig.task;

import com.jobson.smartconfig.ISmartConfigListener;
import com.jobson.smartconfig.ISmartConfigResult;

import java.util.List;

/**
 * ISmartConfigTask defined the task of esptouch should offer. INTERVAL here means
 * the milliseconds of interval of the step. REPEAT here means the repeat times
 * of the step.
 * 
 * @author afunx
 * 
 */
public interface __ISmartConfigTask {

	/**
	 * set the esptouch listener, when one device is connected to the Ap, it will be called back
	 * @param esptouchListener when one device is connected to the Ap, it will be called back
	 */
	void setEsptouchListener(ISmartConfigListener esptouchListener);
	
	/**
	 * Interrupt the Esptouch Task when User tap back or close the Application.
	 */
	void interrupt();

	/**
	 * Note: !!!Don't call the task at UI Main Thread or RuntimeException will
	 * be thrown Execute the Esptouch Task and return the result
	 * 
	 * @return the ISmartConfigResult
	 * @throws RuntimeException
	 */
	ISmartConfigResult executeForResult() throws RuntimeException;

	/**
	 * Note: !!!Don't call the task at UI Main Thread or RuntimeException will
	 * be thrown Execute the Esptouch Task and return the result
	 * 
	 * @param expectTaskResultCount
	 *            the expect result count(if expectTaskResultCount <= 0,
	 *            expectTaskResultCount = Integer.MAX_VALUE)
	 * @return the list of ISmartConfigResult
	 * @throws RuntimeException
	 */
	List<ISmartConfigResult> executeForResults(int expectTaskResultCount) throws RuntimeException;
	
	/**
	 * Turn on or off the log.
	 */
	static final boolean DEBUG = true;

	boolean isCancelled();
}
