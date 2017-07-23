package com.jobson.smartconfig.task;

public interface ISmartConfigGenerator {
	/**
	 * Get guide code by the format of byte[][]
	 * 
	 * @return guide code by the format of byte[][]
	 */
	byte[][] getGCBytes2();

	/**
	 * Get data code by the format of byte[][]
	 * 
	 * @return data code by the format of byte[][]
	 */
	byte[][] getDCBytes2();
}
