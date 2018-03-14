package iot.challenge.mide.location.accuracy.simulation;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a BeaconEvent (Dated BluetoothLeBeacon)
 */
public class BeaconEvent implements Comparable<BeaconEvent> {

	public static final String TIME = "time";
	public static final String BEACON = "beacon";

	private Long time;

	private BluetoothLeBeacon beacon;

	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}

	public BluetoothLeBeacon getBeacon() {
		return beacon;
	}

	private BeaconEvent() {
		super();
	}

	public BeaconEvent(BluetoothLeBeacon beacon) {
		this();
		this.beacon = beacon;
		this.time = System.currentTimeMillis();
	}

	public BeaconEvent(BluetoothLeBeacon beacon, long time) {
		this();
		this.beacon = beacon;
		this.time = time;
	}

	@Override
	public String toString() {
		return toJson().toString();
	}

	public JsonObject toJson() {
		return new JsonObject()
				.add(TIME, time)
				.add(BEACON, beacon.toString());
	}

	public static BeaconEvent fromString(String event) {
		return fromJson(Json.parse(event));
	}

	public static BeaconEvent fromJson(JsonValue value) {
		JsonObject json = value.asObject();
		BeaconEvent result = new BeaconEvent();
		result.time = json.get(TIME).asLong();
		result.beacon = BluetoothLeBeacon.fromString(json.get(BEACON).asString());
		return result;
	}

	/**
	 * Converts a list of events to an array of bytes. It's symmetric to readBeacons
	 * 
	 * @param beacons
	 *            List of events
	 * @return Array of bytes
	 */
	public static byte[] toByteArray(List<BeaconEvent> beacons) {
		JsonArray array = new JsonArray();
		beacons.stream()
				.map(BeaconEvent::toJson)
				.forEach(array::add);
		return array.toString().getBytes();
	}

	/**
	 * Converts an array of bytes to a list of events. It's symmetric to toByteArray
	 * 
	 * @param bytes
	 *            Array of bytes
	 * @return List of events
	 */
	public static List<BeaconEvent> readBeacons(byte[] bytes) {
		if (bytes != null && bytes.length > 0) {
			String message = new String(bytes);
			JsonArray array = Json.parse(message).asArray();

			return array.values().stream()
					.map(BeaconEvent::fromJson)
					.collect(Collectors.toList());

		} else {
			return new ArrayList<>();
		}
	}

	public BeaconEvent copy() {
		BeaconEvent event = new BeaconEvent();
		event.time = new Long(time);
		event.beacon = beacon.copy();
		return event;
	}

	@Override
	public int compareTo(BeaconEvent other) {
		if (other == null)
			return 1;
		return time.compareTo(other.time);
	}
}
