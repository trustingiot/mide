package iot.challenge.mide.location.accuracy.simulation.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import iot.challenge.mide.location.accuracy.installation.Installation;
import iot.challenge.mide.location.accuracy.simulation.BeaconEvent;

/**
 * InstallationService
 */
public class InstallationService {

	private static InstallationService instance = new InstallationService();

	private Map<String, Installation> installations;
	private Map<String, NavigableMap<Long, Map<String, BeaconEvent>>> events;

	private InstallationService() {
		super();
		installations = new HashMap<>();
		events = new HashMap<>();
	}

	public static InstallationService getInstance() {
		return instance;
	}

	public Map<String, Installation> getInstallations() {
		return installations;
	}

	public Map<String, NavigableMap<Long, Map<String, BeaconEvent>>> getEvents() {
		return events;
	}

	public Map<String, Map<String, Map<String, List<BeaconEvent>>>> getEventWindow(long start, long end) {
		// Beacon -> Scanner -> List<BeaconEvent>
		Map<String, Map<String, List<BeaconEvent>>> events = getAllEventsInWindow(start, end);
		return mapEventsByInstallations(events);
	}

	// Beacon -> Scanner -> List<BeaconEvent>
	protected Map<String, Map<String, List<BeaconEvent>>> getAllEventsInWindow(long start, long end) {

		// Beacon -> Scanner -> List<BeaconEvent>
		Map<String, Map<String, List<BeaconEvent>>> result = new HashMap<>();

		// Iterate over events
		events.forEach((beacon, beaconEvents) -> {

			// Events in window
			Collection<Map<String, BeaconEvent>> window = beaconEvents.subMap(start, true, end, true).values();

			if (!window.isEmpty()) {

				// Beacon detection in window
				// Scanner -> Queue<BeaconEvent>
				Map<String, List<BeaconEvent>> byBeacon = new HashMap<>();
				result.put(beacon, byBeacon);

				// Window is a collection of maps (each map contains all detections of a beacon
				// in an instant)
				window.stream().forEach(map -> {
					map.forEach((scanner, beaconEvent) -> {
						List<BeaconEvent> byScanner = byBeacon.get(scanner);
						if (byScanner == null) {
							byScanner = new ArrayList<>();
							byBeacon.put(scanner, byScanner);
						}
						byScanner.add(beaconEvent);
					});
				});
			}
		});

		return result;
	}

	// Installation -> Beacon -> Scanner -> List<BeaconEvent>
	protected Map<String, Map<String, Map<String, List<BeaconEvent>>>> mapEventsByInstallations(
			Map<String, Map<String, List<BeaconEvent>>> events) {

		Map<String, Map<String, Map<String, List<BeaconEvent>>>> result = new HashMap<>();

		installations.keySet().forEach(installation -> {
			Map<String, Map<String, List<BeaconEvent>>> eventsByInstallation = new HashMap<>();

			events.forEach((beacon, detections) -> {
				Map<String, List<BeaconEvent>> byInstallation = detections.entrySet().stream()
						.filter(entry -> hasScanner(installation, entry.getKey()))
						.collect(Collectors.toMap(Entry::getKey, it -> new ArrayList<>(it.getValue())));

				if (!byInstallation.isEmpty())
					eventsByInstallation.put(beacon, byInstallation);

			});

			if (!eventsByInstallation.isEmpty())
				result.put(installation, eventsByInstallation);

		});

		return result;
	}

	public Installation getInstallation(String id) {
		return installations.get(id);
	}

	public void addInstallation(Installation installation) {
		if (installation != null)
			installations.put(installation.getId(), installation);
	}

	public void removeInstallation(Installation installation) {
		if (installation != null)
			installations.remove(installation.getId());
	}

	public void modifyInstallation(Installation installation) {
		addInstallation(installation);
	}

	public void saveEvents(String scanner, String beacon, List<BeaconEvent> events) {
		synchronized (this) {
			NavigableMap<Long, Map<String, BeaconEvent>> beaconEvents = this.events.get(beacon);
			if (beaconEvents == null) {
				beaconEvents = new TreeMap<Long, Map<String, BeaconEvent>>();
				this.events.put(beacon, beaconEvents);
			}

			for (BeaconEvent event : events) {
				Long time = event.getTime();
				Map<String, BeaconEvent> instantEvents = beaconEvents.get(time);
				if (instantEvents == null) {
					instantEvents = new HashMap<String, BeaconEvent>();
					beaconEvents.put(time, instantEvents);
				}

				instantEvents.put(scanner, event);
			}
		}
	}

	public boolean hasScanner(String installation, String scanner) {
		return installations.get(installation)
				.getScanners()
				.stream()
				.filter(it -> it.getAddr().equals(scanner))
				.findFirst()
				.isPresent();
	}
}
