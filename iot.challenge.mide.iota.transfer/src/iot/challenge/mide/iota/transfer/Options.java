package iot.challenge.mide.iota.transfer;

import java.util.Map;

/**
 * IOTATransferEfficiencyService's options
 */
public class Options extends iot.challenge.mide.util.Options {

	public static final String PROPERTY_ENABLE = "enable";
	public static final String PROPERTY_IOTA_WALLET_SEED = "iota.wallet.seed";
	public static final String PROPERTY_IOTA_NODE_PROTOCOL = "iota.node.protocol";
	public static final String PROPERTY_IOTA_NODE_HOST = "iota.node.host";
	public static final String PROPERTY_IOTA_NODE_PORT = "iota.node.port";
	public static final String PROPERTY_IOTA_ADDRESS = "iota.address";
	public static final String PROPERTY_TRANSACTIONS = "transactions";

	public static final boolean PROPERTY_ENABLE_DEFAULT = false;
	public static final String PROPERTY_IOTA_WALLET_SEED_DEFAULT = "IHDEENZYITYVYSPKAURUZAQKGVJEREFDJMYTANNXXGPZ9GJWTEOJJ9IPMXOGZNQLSNMFDSQOTZAEFTUEB";
	public static final String PROPERTY_IOTA_NODE_PROTOCOL_DEFAULT = "http";
	public static final String PROPERTY_IOTA_NODE_HOST_DEFAULT = "iota-tangle.io";
	public static final String PROPERTY_IOTA_NODE_PORT_DEFAULT = "14265"; // IotaAPI.Builder().port(String port)
	public static final String PROPERTY_IOTA_ADDRESS_DEFAULT = "JURA";
	public static final int PROPERTY_TRANSACTIONS_DEFAULT = 10;

	protected final boolean enable;

	protected final String iotaSeed;

	protected final String iotaNodeProtocol;

	protected final String iotaNodeHost;

	protected final String iotaNodePort;

	protected final String iotaAddress;

	protected final int transactions;

	public Options(Map<String, Object> properties) {
		super(properties);
		enable = read(PROPERTY_ENABLE, PROPERTY_ENABLE_DEFAULT);
		iotaSeed = read(PROPERTY_IOTA_WALLET_SEED, PROPERTY_IOTA_WALLET_SEED_DEFAULT);
		iotaNodeProtocol = read(PROPERTY_IOTA_NODE_PROTOCOL, PROPERTY_IOTA_NODE_PROTOCOL_DEFAULT);
		iotaNodeHost = read(PROPERTY_IOTA_NODE_HOST, PROPERTY_IOTA_NODE_HOST_DEFAULT);
		iotaNodePort = read(PROPERTY_IOTA_NODE_PORT, PROPERTY_IOTA_NODE_PORT_DEFAULT);
		iotaAddress = read(PROPERTY_IOTA_ADDRESS, PROPERTY_IOTA_ADDRESS_DEFAULT);
		transactions = read(PROPERTY_TRANSACTIONS, PROPERTY_TRANSACTIONS_DEFAULT);
	}

	public boolean isEnable() {
		return enable;
	}

	public String getIotaSeed() {
		return iotaSeed;
	}

	public String getIotaNodeProtocol() {
		return iotaNodeProtocol;
	}

	public String getIotaNodeHost() {
		return iotaNodeHost;
	}

	public String getIotaNodePort() {
		return iotaNodePort;
	}

	public String getIotaAddress() {
		return iotaAddress;
	}

	public int getTransactions() {
		return transactions;
	}
}
