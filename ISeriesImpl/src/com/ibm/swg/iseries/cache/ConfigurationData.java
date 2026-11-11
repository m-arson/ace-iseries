package com.ibm.swg.iseries.cache;

import java.util.Arrays;
import com.ibm.as400.access.AS400ConnectionPool;

public final class ConfigurationData {
	private final String address;
	private final String user;
	private final char[] password;
	private final int maxConnections;
	private final int minConnections;
	private final int maxUseTimeout;
	private final String ccsid;
	private final AS400ConnectionPool pool;

	ConfigurationData(String address, String user, char[] password, int maxConnections,
			int minConnections, int maxUseTimeout, String ccsid, AS400ConnectionPool pool) {
		this.address = address;
		this.user = user;
		this.password = password.clone();
		this.maxConnections = maxConnections;
		this.minConnections = minConnections;
		this.maxUseTimeout = maxUseTimeout;
		this.ccsid = ccsid;
		this.pool = pool;
	}

	public int get_maxConnections() { return maxConnections; }
	public int get_minConnections() { return minConnections; }
	public int get_maxUseTimeout() { return maxUseTimeout; }
	public String get_iSeriesAddress() { return address; }
	public String get_iSeriesUser() { return user; }
	public synchronized String get_iSeriesPassword() { return new String(password); }
	public String get_CCSID() { return ccsid; }
	public AS400ConnectionPool get_iSeriesConnectionPool() { return pool; }
	synchronized char[] copyPassword() { return password.clone(); }
	synchronized void clearPassword() { Arrays.fill(password, '\0'); }
}
