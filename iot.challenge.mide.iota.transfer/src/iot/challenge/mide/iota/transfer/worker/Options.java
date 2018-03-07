package iot.challenge.mide.iota.transfer.worker;

import java.util.Map;

/**
 * Worker's options
 */
public class Options extends iot.challenge.mide.util.Options {

	public static final String PROPERTY_IOTA_WALLET_SEED = "iota.wallet.seed";
	public static final String PROPERTY_ID = "id";

	public static final String PROPERTY_IOTA_WALLET_SEED_DEFAULT = "IHDEENZYITYVYSPKAURUZAQKGVJEREFDJMYTANNXXGPZ9GJWTEOJJ9IPMXOGZNQLSNMFDSQOTZAEFTUEB";
	public static final String PROPERTY_ID_DEFAULT = "worker";

	protected final String iotaSeed;

	protected final String id;

	public Options(Map<String, Object> properties) {
		super(properties);
		iotaSeed = read(PROPERTY_IOTA_WALLET_SEED, PROPERTY_IOTA_WALLET_SEED_DEFAULT);
		id = read(PROPERTY_ID, PROPERTY_ID_DEFAULT);
	}

	public String getIotaSeed() {
		return iotaSeed;
	}

	public String getID() {
		return id;
	}
}