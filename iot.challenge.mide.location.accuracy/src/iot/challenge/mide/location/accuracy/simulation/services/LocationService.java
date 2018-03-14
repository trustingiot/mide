package iot.challenge.mide.location.accuracy.simulation.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Properties;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import iot.challenge.mide.location.accuracy.Main;
import iot.challenge.mide.location.accuracy.installation.Point;
import iot.challenge.mide.location.accuracy.simulation.BeaconEvent;
import iot.challenge.mide.location.accuracy.simulation.BluetoothLeBeacon;

/**
 * LocationService provider
 */
public class LocationService {

	public static final String RETENTION_TIME = "retention.time";
	public static final String PUBLICATION_RATE = "publication.rate";
	public static final String DELAY = "delay";
	public static final String SCANNING_WINDOW = "scanning.window";
	public static final String ATTENUATION = "attenuation";
	public static final String CUTOFF_RATE = "cutoff.rate";

	protected int retentionTime;
	protected int publicationRate;
	protected int delay;
	protected int scanningWindow;
	protected double attenuation;
	protected double cutoffRate;

	protected PositioningService positioningService;

	protected Map<String, NavigableMap<Long, Map<String, Point>>> locations;

	private LocationService() {
		super();
		this.locations = new HashMap<>();
	}

	public LocationService(PositioningService positioningService) {
		this();
		this.positioningService = positioningService;
		readProperties();
	}

	private void readProperties() {

		FileInputStream input = null;
		try {
			Properties properties = new Properties();
			input = new FileInputStream(Main.CONFIG);
			properties.load(input);

			retentionTime = Integer.parseInt(properties.getProperty(RETENTION_TIME));
			publicationRate = Integer.parseInt(properties.getProperty(PUBLICATION_RATE));
			delay = Integer.parseInt(properties.getProperty(DELAY));
			scanningWindow = Integer.parseInt(properties.getProperty(SCANNING_WINDOW));
			attenuation = Double.parseDouble(properties.getProperty(ATTENUATION));
			cutoffRate = Double.parseDouble(properties.getProperty(CUTOFF_RATE));
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			if (input != null)
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	public List<Point> localization(long current, long finish) {
		List<Point> result = new ArrayList<>();
		localization(current, finish, result);
		return result;
	}

	public void localization(long current, long finish, List<Point> result) {
		long end = current - (delay * 1000l);
		long start = end - scanningWindow;

		Point point = computeLocation(start, end);
		if (point != null)
			result.add(point);

		current += publicationRate;

		if (current < finish)
			localization(current, finish, result);
	}

	// Installation -> Beacon -> Position
	protected Point computeLocation(long start, long end) {

		Point result = new Point();

		// Installation -> Beacon -> Scanner -> Queue<BeaconEvent>
		Map<String, Map<String, Map<String, List<BeaconEvent>>>> events = InstallationService.getInstance()
				.getEventWindow(start,
						end);

		// Detections by installations
		events.forEach((installation, detections) -> {

			// Detections by beacons in installation
			detections.forEach((beacon, beaconEvents) -> {

				// Beacon position in installation
				Point p = position(installation, beacon, beaconEvents, start, end);
				if (p != null) {
					result.setX(p.getX());
					result.setY(p.getY());
				}
			});

		});

		return (result.getX() == null) ? null : result;
	}

	protected Point position(String installation, String beacon, Map<String, List<BeaconEvent>> events, long start,
			long end) {
		List<String> scannerAddr = new ArrayList<>();
		List<BeaconEvent> scannerEvents = new ArrayList<>();
		events.forEach((scanner, detections) -> {
			if (!detections.isEmpty()) {
				BeaconEvent resume = resumeBeaconEvents(detections, start, end);
				if (resume != null) {
					scannerAddr.add(scanner);
					scannerEvents.add(resume);
				}
			}
		});

		return positioningService.position(installation, scannerAddr, scannerEvents);
	}

	protected BeaconEvent resumeBeaconEvents(List<BeaconEvent> events, long start, long end) {
		BeaconEvent result = null;

		List<BeaconEvent> validEvents = removeOutliersEvents(events);
		if (!validEvents.isEmpty()) {
			int rssi = aggregateRssis(validEvents, start, end);
			result = validEvents.get(0).copy();
			result.getBeacon().setRssi(rssi);
		}

		return result;
	}

	private List<BeaconEvent> removeOutliersEvents(List<BeaconEvent> events) {
		List<Short> rssis = extractRssis(events);
		removeOutliers(rssis);

		List<BeaconEvent> normal = new ArrayList<>();
		int size = events.size();
		for (int i = 0; i < size; i++) {
			if (rssis.get(i) != null)
				normal.add(events.get(i));
		}

		return normal;
	}

	private static List<Short> extractRssis(List<BeaconEvent> events) {
		return events.stream()
				.map(BeaconEvent::getBeacon)
				.map(BluetoothLeBeacon::getRssi)
				.map(v -> (short) v.intValue())
				.collect(Collectors.toList());
	}

	private void removeOutliers(List<Short> total) {
		List<Short> partition = total.stream().filter(Objects::nonNull).collect(Collectors.toList());

		int size = total.size();
		int partitionSize = partition.size();
		int outliers = size - partitionSize;

		if (outliers < size) {
			double[] limits = computeLimits(partition, cutoffRate);
			total.replaceAll(v -> (inRange(v, limits)) ? v : null);

			if (total.stream().filter(Objects::isNull).count() > outliers)
				removeOutliers(total);
		}
	}

	private static double[] computeLimits(List<Short> values, double cutoffRate) {
		double arithmeticAverage = arithmeticAverage(values);
		double typicalDeviation = typicalDeviation(values, arithmeticAverage);
		double factor = typicalDeviation * cutoffRate;
		return new double[] { arithmeticAverage - factor, arithmeticAverage + factor };
	}

	private static double arithmeticAverage(List<Short> values) {
		return averageSum(values, Short::shortValue);
	}

	private static double averageSum(List<Short> values, ToDoubleFunction<Short> f) {
		return sum(values, f) / (double) values.size();
	}

	private static double sum(List<Short> values, ToDoubleFunction<Short> f) {
		return values.stream().mapToDouble(f::applyAsDouble).sum();
	}

	private static double typicalDeviation(List<Short> values, double arithmeticAverage) {
		return Math.sqrt(averageSum(values, v -> Math.pow(v - arithmeticAverage, 2)));
	}

	private static boolean inRange(Short v, double[] limits) {
		return v != null && v >= limits[0] && v <= limits[1];
	}

	private int aggregateRssis(List<BeaconEvent> events, long start, long end) {
		List<Double> weights = computeWeights(events, start, end);
		List<Short> rssis = extractRssis(events);
		return (int) IntStream.range(0, rssis.size())
				.mapToDouble(i -> weights.get(i) * rssis.get(i))
				.sum();
	}

	private List<Double> computeWeights(List<BeaconEvent> events, long start, long end) {
		double window = end - start;
		List<Double> weights = events.stream()
				.map(BeaconEvent::getTime)
				.map(t -> end - t)
				.map(delay -> 1d - (((double) delay) / window))
				.map(w -> Math.pow(w, attenuation))
				.collect(Collectors.toList());
		normalize(weights);
		return weights;
	}

	protected static void normalize(List<Double> values) {
		double sum = values.stream().mapToDouble(Double::doubleValue).sum();
		values.replaceAll(v -> v / sum);
	}
}
