package iot.challenge.mide.location.accuracy.trilaterization;

/**
 * Distance unit
 */
public enum DistanceUnits {
	MILIMETERS(1000), METERS(1);

	private int scale;

	private DistanceUnits(int scale) {
		this.scale = scale;
	}

	public int getScale() {
		return scale;
	}

	public static double convert(double distance, DistanceUnits from, DistanceUnits to) {
		double factor = to.scale / from.scale;
		return distance * factor;
	}
}
