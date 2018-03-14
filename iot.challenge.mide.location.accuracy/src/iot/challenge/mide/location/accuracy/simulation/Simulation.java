package iot.challenge.mide.location.accuracy.simulation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.TreeMap;

import iot.challenge.mide.location.accuracy.installation.Installation;
import iot.challenge.mide.location.accuracy.installation.Point;
import iot.challenge.mide.location.accuracy.simulation.services.InstallationService;
import iot.challenge.mide.location.accuracy.simulation.services.LocationService;
import iot.challenge.mide.location.accuracy.simulation.services.PositioningService;
import iot.challenge.mide.location.accuracy.trilaterization.DistanceAlgorithm;
import iot.challenge.mide.location.accuracy.trilaterization.LeastSquaresAlgorithm;

public class Simulation {

	private static String BEACON = "TEST";

	private Installation installation;
	private Point point;
	private Recording recording;
	private long time;
	private long end;
	private double error;

	private InstallationService installationService;
	private PositioningService positioningService;
	private LocationService locationService;

	private Simulation() {
		super();
		installationService = InstallationService.getInstance();
	}

	public Simulation(Installation installation, Point point, Recording recording, DistanceAlgorithm distanceAlgorithm,
			LeastSquaresAlgorithm leastSquaresAlgorithm) {
		this();
		this.installation = installation;
		this.point = point;
		this.recording = recording;
		positioningService = new PositioningService(distanceAlgorithm, leastSquaresAlgorithm);
		locationService = new LocationService(positioningService);
	}

	public void execute() {
		saveEvents();
		List<Point> points = locationService.localization(time, end);
		error = (points.size() > 0)
				? (points.stream().mapToDouble(this::euclideanDistance).sum() / (double) points.size()) / 1000d
				: 0;
	}

	private double euclideanDistance(Point other) {
		return Math.sqrt(Math.pow(other.getX() - point.getX(), 2) + Math.pow(other.getY() - point.getY(), 2));
	}

	private void saveEvents() {
		installationService.addInstallation(installation);
		installationService.getEvents().put(BEACON, new TreeMap<Long, Map<String, BeaconEvent>>());

		time = end = System.currentTimeMillis();

		recording.getEvents().forEach(this::save);
	}

	private void save(String scanner, Queue<BeaconEvent> events) {
		NavigableMap<Long, Map<String, BeaconEvent>> beaconEvents = installationService.getEvents().get(BEACON);

		for (BeaconEvent event : events) {
			event.setTime(event.getTime() + this.time);
			Long time = event.getTime();
			Map<String, BeaconEvent> instantEvents = beaconEvents.get(time);
			if (instantEvents == null) {
				instantEvents = new HashMap<String, BeaconEvent>();
				beaconEvents.put(time, instantEvents);
			}
			instantEvents.put(scanner, event);
			end = (time > end) ? time : end;
		}
	}

	public double getError() {
		return error;
	}

}
