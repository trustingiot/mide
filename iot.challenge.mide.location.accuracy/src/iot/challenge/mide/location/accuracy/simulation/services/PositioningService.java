package iot.challenge.mide.location.accuracy.simulation.services;

import java.util.List;

import iot.challenge.mide.location.accuracy.installation.Installation;
import iot.challenge.mide.location.accuracy.installation.Point;
import iot.challenge.mide.location.accuracy.installation.Scanner;
import iot.challenge.mide.location.accuracy.simulation.BeaconEvent;
import iot.challenge.mide.location.accuracy.trilaterization.DistanceAlgorithm;
import iot.challenge.mide.location.accuracy.trilaterization.DistanceUnits;
import iot.challenge.mide.location.accuracy.trilaterization.LeastSquaresAlgorithm;

/**
 * PositioningService
 */
public class PositioningService {

	private static final int MIN_SCANNERS = 4;

	private DistanceAlgorithm distanceAlgorithm;
	private LeastSquaresAlgorithm leastSquaresAlgorithm;

	private InstallationService installationService;

	private PositioningService() {
		super();
		installationService = InstallationService.getInstance();
	}

	public PositioningService(DistanceAlgorithm distanceAlgorithm, LeastSquaresAlgorithm leastSquaresAlgorithm) {
		this();
		this.distanceAlgorithm = distanceAlgorithm;
		this.leastSquaresAlgorithm = leastSquaresAlgorithm;
	}

	public Point position(String installationId, List<String> scannersAddr, List<BeaconEvent> events) {
		if (scannersAddr.size() < MIN_SCANNERS)
			return null;

		return leastSquaresAlgorithm.computePosition(
				obtainPositions(
						installationService.getInstallation(installationId),
						scannersAddr),
				obtainDistances(events));
	}

	protected static double[][] obtainPositions(Installation installation, List<String> scannersAddr) {
		return scannersAddr.stream()
				.map(it -> findScannerByAddr(installation.getScanners(), it))
				.map(Scanner::getPosition)
				.map(point -> new double[] { point.getX(), point.getY() })
				.toArray(double[][]::new);
	}

	protected static Scanner findScannerByAddr(List<Scanner> scanners, String addr) {
		return scanners.stream()
				.filter(scanner -> addr.equals(scanner.getAddr()))
				.findFirst()
				.orElse(null);
	}

	protected double[] obtainDistances(List<BeaconEvent> events) {
		return events.stream()
				.mapToDouble(event -> distanceAlgorithm.computeDistance(event, DistanceUnits.MILIMETERS))
				.toArray();
	}
}
