package iot.challenge.mide.iota.transfer.manager;

import java.util.Map;

/**
 * Manager's options
 */
public class Options extends iot.challenge.mide.iota.transfer.Options {

	public static final String PROPERTY_MODE = "mode";

	public static final String PROPERTY_MODE_DEFAULT = Manager.MODE_COLLABORATIVE;

	protected final String mode;

	public Options(Map<String, Object> properties) {
		super(properties);
		mode = read(PROPERTY_MODE, PROPERTY_MODE_DEFAULT);
	}

	public String getMode() {
		return mode;
	}
}
