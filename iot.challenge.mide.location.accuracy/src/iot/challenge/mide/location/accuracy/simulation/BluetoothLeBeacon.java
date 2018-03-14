package iot.challenge.mide.location.accuracy.simulation;

public class BluetoothLeBeacon {

	public static final String RSSI = "rssi";
	public static final String TXPOWER = "txpower";

	private int rssi;
	private int txpower;

	public int getRssi() {
		return rssi;
	}

	public void setRssi(int rssi) {
		this.rssi = rssi;
	}

	public int getTxpower() {
		return txpower;
	}

	public void setTxpower(int txpower) {
		this.txpower = txpower;
	}

	public static BluetoothLeBeacon fromString(String event) {
		BluetoothLeBeacon result = new BluetoothLeBeacon();
		for (String token : event.split("; ")) {
			if (token.startsWith("txpower")) {
				result.txpower = Integer.parseInt(token.split(": ")[1]);
			} else if (token.startsWith("rssi")) {
				result.rssi = Integer.parseInt(token.split(": ")[1]);
			}
		}
		return result;
	}

	public BluetoothLeBeacon copy() {
		BluetoothLeBeacon result = new BluetoothLeBeacon();
		result.rssi = rssi;
		result.txpower = txpower;
		return result;
	}
}