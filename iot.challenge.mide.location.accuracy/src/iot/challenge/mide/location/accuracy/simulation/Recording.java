package iot.challenge.mide.location.accuracy.simulation;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * Represents a recording of beacon events
 */
public class Recording {

	public static final String RECORDING = "recording";

	public static final String TOPIC = "topic";
	public static final String START_TIME = "startTime";
	public static final String DURATION = "duration";
	public static final String EVENTS = "events";
	public static final String SCANNER = "scanner";
	public static final String SCANNER_EVENTS = "scannerEvents";

	private final String topic;

	private final Long startTime;

	private final Long duration;

	private Map<String, Queue<BeaconEvent>> events;

	public Recording(String topic, long startTime, long duration) {
		this.topic = topic;
		this.startTime = startTime;
		this.duration = duration;
		this.events = new HashMap<>();
	}

	public String getTopic() {
		return topic;
	}

	public Long getStartTime() {
		return startTime;
	}

	public Long getDuration() {
		return duration;
	}

	public Map<String, Queue<BeaconEvent>> getEvents() {
		return events;
	}

	/**
	 * Returns all beacon events detected by 'scanner'
	 *
	 * @param scanner
	 *            Scanner
	 * @return A priority queue of events.
	 *         {@link BeaconEvent#compareTo(BeaconEvent)}
	 */
	public Queue<BeaconEvent> getScannerEvents(String scanner) {
		Queue<BeaconEvent> scannerEvents = events.get(scanner);
		if (scannerEvents == null) {
			scannerEvents = new PriorityQueue<>();
			events.put(scanner, scannerEvents);
		}
		return scannerEvents;
	}

	@Override
	public String toString() {
		return toJson().toString();
	}

	public JsonObject toJson() {
		return new JsonObject()
				.add(TOPIC, topic)
				.add(START_TIME, startTime)
				.add(DURATION, duration)
				.add(EVENTS, toJson(events));
	}

	private static JsonArray toJson(Map<String, Queue<BeaconEvent>> events) {
		JsonArray json = new JsonArray();
		events.entrySet()
				.stream()
				.map(Recording::toJson)
				.forEach(json::add);
		return json;
	}

	private static JsonObject toJson(Map.Entry<String, Queue<BeaconEvent>> entry) {
		return new JsonObject()
				.add(SCANNER, entry.getKey())
				.add(SCANNER_EVENTS, toJson(entry.getValue()));
	}

	private static JsonArray toJson(Queue<BeaconEvent> scannerEvents) {
		JsonArray json = new JsonArray();
		scannerEvents.stream()
				.map(BeaconEvent::toJson)
				.forEach(json::add);
		return json;
	}

	public static Recording fromString(String message) {
		JsonObject json = Json.parse(message).asObject();
		Recording recording = new Recording(
				json.get(TOPIC).asString(),
				json.get(START_TIME).asLong(),
				json.get(DURATION).asLong());
		recording.events = readEvents(json.get(EVENTS).asArray());
		return recording;
	}

	private static Map<String, Queue<BeaconEvent>> readEvents(JsonArray events) {
		return events.values().stream()
				.map(JsonValue::asObject)
				.collect(Collectors.toMap(
						json -> json.get(SCANNER).asString(),
						json -> readScannerEvents(json.get(SCANNER_EVENTS).asArray())));
	}

	private static Queue<BeaconEvent> readScannerEvents(JsonArray scannerEvents) {
		return scannerEvents.values().stream()
				.map(BeaconEvent::fromJson)
				.collect(Collectors.toCollection(PriorityQueue::new));
	}
}
