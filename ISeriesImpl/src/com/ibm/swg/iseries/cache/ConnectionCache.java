package com.ibm.swg.iseries.cache;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400ConnectionPool;
import com.ibm.as400.access.ConnectionPoolException;
import com.ibm.as400.access.SocketProperties;
import com.ibm.broker.plugin.MbUserException;
import com.ibm.swg.iseries.RuntimeFailure;

public final class ConnectionCache {
	private record PolicyKey(String type, String name) {
		PolicyKey {
			if (type == null || type.isBlank() || name == null || name.isBlank()) {
				throw RuntimeFailure.invalidArgument("Policy type and name are required.");
			}
		}
	}

	private static final class Entry {
		final ConfigurationData configuration;
		final Object acquisitionGate = new Object();
		int owners;
		int leases;
		boolean retired;

		Entry(ConfigurationData configuration) {
			this.configuration = configuration;
		}
	}

	private final Map<PolicyKey, Entry> pools = new HashMap<>();
	private final Map<AS400, Entry> leases = new IdentityHashMap<>();

	private ConnectionCache() { }

	public static synchronized void setISeriesPool(String type, String name, Properties properties) throws MbUserException {
		try {
			PolicyKey key = new PolicyKey(type, name);
			Entry existing = Holder.INSTANCE.pools.get(key);
			ConfigurationData configuration = createConfiguration(properties,
					existing == null ? null : existing.configuration);
			if (existing == null) {
				Holder.INSTANCE.pools.put(key, new Entry(configuration));
			}
		} catch (IllegalArgumentException e) {
			throw failure("setISeriesPool", "Policy configuration failed.", e);
		}
	}

	public static synchronized void acquirePolicy(String type, String name, Properties properties) throws MbUserException {
		setISeriesPool(type, name, properties);
		Holder.INSTANCE.pools.get(new PolicyKey(type, name)).owners++;
	}

	public static synchronized void releasePolicy(String type, String name) {
		PolicyKey key = new PolicyKey(type, name);
		Entry entry = Holder.INSTANCE.pools.get(key);
		if (entry != null && entry.owners > 0 && --entry.owners == 0) {
			Holder.INSTANCE.pools.remove(key);
			entry.retired = true;
			closeIfUnused(entry);
		}
	}

	private static ConfigurationData createConfiguration(Properties properties, ConfigurationData existing)
			throws MbUserException {
		if (properties == null) {
			throw RuntimeFailure.invalidArgument("Policy properties are required.");
		}
		String address = required(properties, "iSeriesAddress");
		String user = required(properties, "iSeriesUser");
		String secret = properties.getProperty("iSeriesPassword");
		if (secret == null || secret.isBlank()) {
			throw RuntimeFailure.invalidArgument("iSeriesPassword is required.");
		}
		int max = setting(properties, "maxConnections", 5, 1, 1024);
		int min = setting(properties, "minConnections", 1, 0, max);
		int maxUse = setting(properties, "maxUseTimeout", -1, -1, 86_400_000);
		if (maxUse == 0) {
			throw RuntimeFailure.invalidArgument("maxUseTimeout must be -1 or positive.");
		}
		int ccsid = setting(properties, "CCSID", 1208, 0, 65535);
		char[] password = secret.toCharArray();
		if (existing != null) {
			char[] currentPassword = existing.copyPassword();
			try {
				if (!address.equals(existing.get_iSeriesAddress()) || !user.equals(existing.get_iSeriesUser())
						|| !Arrays.equals(password, currentPassword) || max != existing.get_maxConnections()
						|| min != existing.get_minConnections() || maxUse != existing.get_maxUseTimeout()
						|| !Integer.toString(ccsid).equals(existing.get_CCSID())) {
					throw RuntimeFailure.invalidArgument("Policy changed while in use; redeploy all sharing flows or use a new policy name.");
				}
				return existing;
			} finally {
				Arrays.fill(password, '\0');
				Arrays.fill(currentPassword, '\0');
			}
		}
		AS400ConnectionPool pool = new AS400ConnectionPool();
		boolean initialized = false;
		try {
			pool.setRunMaintenance(false);
			pool.setThreadUsed(false);
			pool.setMaxConnections(max);
			pool.setMaxUseTime(maxUse);
			SocketProperties sockets = new SocketProperties();
			sockets.setLoginTimeout(30_000);
			sockets.setSoTimeout(120_000);
			pool.setSocketProperties(sockets);
			if (min > 0) {
				pool.fill(address, user, password, AS400.COMMAND, min);
			}
			ConfigurationData configuration = new ConfigurationData(address, user, password, max, min,
					maxUse, Integer.toString(ccsid), pool);
			initialized = true;
			return configuration;
		} catch (ConnectionPoolException e) {
			throw failure("setISeriesPool", "Pool initialization failed.", e);
		} finally {
			Arrays.fill(password, '\0');
			if (!initialized) {
				pool.close();
			}
		}
	}

	private static String required(Properties properties, String key) {
		String value = properties.getProperty(key);
		if (value == null || value.isBlank()) {
			throw RuntimeFailure.invalidArgument(key + " is required.");
		}
		return value.trim();
	}

	static int setting(Properties properties, String key, int fallback, int min, int max) {
		String value = properties.getProperty(key);
		int result;
		try {
			result = value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			throw RuntimeFailure.invalidArgument(key + " must be an integer.");
		}
		if (result < min || result > max) {
			throw RuntimeFailure.invalidArgument(key + " is out of range.");
		}
		return result;
	}

	public static AS400 getConnection(String type, String name) throws MbUserException {
		Entry entry;
		synchronized (ConnectionCache.class) {
			try {
				entry = Holder.INSTANCE.pools.get(new PolicyKey(type, name));
			} catch (IllegalArgumentException e) {
				throw failure("getConnection", "Invalid policy key.", e);
			}
			if (entry == null) {
				throw failure("getConnection", "No connection pool configured for policy.", null);
			}
			entry.leases++;
		}
		ConfigurationData configuration = entry.configuration;
		char[] password = configuration.copyPassword();
		boolean acquired = false;
		try {
			AS400 connection;
			synchronized (entry.acquisitionGate) {
				connection = configuration.get_iSeriesConnectionPool().getConnection(
						configuration.get_iSeriesAddress(), configuration.get_iSeriesUser(), password, AS400.COMMAND);
			}
			Objects.requireNonNull(connection, "Pool returned no connection.");
			synchronized (ConnectionCache.class) {
				Holder.INSTANCE.leases.put(connection, entry);
			}
			acquired = true;
			return connection;
		} catch (ConnectionPoolException | RuntimeException e) {
			throw failure("getConnection", "Getting connection from pool failed.", e);
		} finally {
			Arrays.fill(password, '\0');
			if (!acquired) {
				synchronized (ConnectionCache.class) {
					entry.leases--;
					closeIfUnused(entry);
				}
			}
		}
	}

	public static synchronized ConfigurationData getConfigurationData(String type, String name) {
		Entry entry = Holder.INSTANCE.pools.get(new PolicyKey(type, name));
		return entry == null ? null : entry.configuration;
	}

	public static synchronized void releaseConnection(String type, String name, AS400 connection) {
		Objects.requireNonNull(type, "Policy type is required");
		Objects.requireNonNull(name, "Policy name is required");
		if (connection == null) {
			return;
		}
		Entry entry = Holder.INSTANCE.leases.remove(connection);
		if (entry != null) {
			try {
				entry.configuration.get_iSeriesConnectionPool().returnConnectionToPool(connection);
			} finally {
				entry.leases--;
				closeIfUnused(entry);
			}
		}
	}

	private static void closeIfUnused(Entry entry) {
		if (entry.retired && entry.leases == 0) {
			try {
				entry.configuration.get_iSeriesConnectionPool().close();
			} finally {
				entry.configuration.clearPassword();
			}
		}
	}

	private static MbUserException failure(String method, String message, Exception cause) {
		return RuntimeFailure.create("ConnectionCache", method, message, cause);
	}

	private static final class Holder {
		static final ConnectionCache INSTANCE = new ConnectionCache();
	}
}
