package iot.challenge.mide.location.accuracy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import iot.challenge.mide.location.accuracy.installation.Installation;
import iot.challenge.mide.location.accuracy.installation.Point;
import iot.challenge.mide.location.accuracy.simulation.DataSet;
import iot.challenge.mide.location.accuracy.simulation.Simulation;
import iot.challenge.mide.location.accuracy.trilaterization.DistanceAlgorithm;
import iot.challenge.mide.location.accuracy.trilaterization.LeastSquaresAlgorithm;

public class Main {

	public static final String CONFIG = "config.properties";

	private static final String DATASET = "dataset";
	private static final String DISTANCE_ALGORITHM = "distance.algorithm";
	private static final String LEAST_SQUARES_ALGORITHM = "least.squares.algorithm";

	public static void main(String[] args) throws IOException {
		FileInputStream input = null;
		try {
			Properties properties = new Properties();
			input = new FileInputStream(CONFIG);
			properties.load(input);

			DataSet dataset = loadDataSet(properties.getProperty(DATASET));
			DistanceAlgorithm distanceAlgorithm = DistanceAlgorithm.valueOf(properties.getProperty(DISTANCE_ALGORITHM));
			LeastSquaresAlgorithm leastSquaresAlgorithm = LeastSquaresAlgorithm
					.valueOf(properties.getProperty(LEAST_SQUARES_ALGORITHM));

			execute(dataset, distanceAlgorithm, leastSquaresAlgorithm);

		} catch (Exception e) {
			System.err.println("Execution aborted");
			e.printStackTrace();
		} finally {
			if (input != null)
				input.close();
		}
	}

	private static DataSet loadDataSet(String folder) throws Exception {
		File dataSetFolder = new File(folder);
		return new DataSet(dataSetFolder);
	}

	private static void execute(
			DataSet dataSet,
			DistanceAlgorithm distanceAlgorithm,
			LeastSquaresAlgorithm leastSquaresAlgorithm) {

		Map<Point, Simulation> simulations = new HashMap<>();

		Installation installation = dataSet.getInstallation();
		dataSet.getRecordings().forEach((p, r) -> {
			simulations.put(p, new Simulation(installation, p, r, distanceAlgorithm, leastSquaresAlgorithm));
			simulations.get(p).execute();
		});

		displayResults(simulations);
	}

	private static void displayResults(Map<Point, Simulation> simulations) {
		simulations.forEach((p, s) -> System.out.format(p + ": %.3f m.%n", s.getError()));
		double am = simulations.values().stream().mapToDouble(Simulation::getError).sum() / simulations.values().size();
		System.out.format("Mean error: %.3f m.%n", am);
	}

}
