package iot.challenge.mide.location.accuracy.trilaterization;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.RealVector;

import iot.challenge.mide.location.accuracy.installation.Point;

public enum LeastSquaresAlgorithm {
	Linear("Linear") {
		@Override
		protected RealVector solve(TrilaterationFunction function) {
			LinearLeastSquaresSolver solver = new LinearLeastSquaresSolver(function);
			return solver.solve();
		}
	},
	NonLinear("NonLinear") {
		@Override
		protected RealVector solve(TrilaterationFunction function) {
			NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(function,
					new LevenbergMarquardtOptimizer());
			Optimum optimum = solver.solve();
			return optimum.getPoint();
		}
	};

	private String name;

	private LeastSquaresAlgorithm(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	// Points uses integer coordinates -> loss of precision
	// For minimum loss it's recommended to use mm. instead of m.
	public Point computePosition(double[][] positions, double[] distances) {
		TrilaterationFunction trilaterationFunction = new TrilaterationFunction(positions, distances);
		RealVector vector = solve(trilaterationFunction);
		double[] coordinates = vector.toArray();
		return new Point((int) coordinates[0], (int) coordinates[1]);
	}

	abstract protected RealVector solve(TrilaterationFunction function);
}
