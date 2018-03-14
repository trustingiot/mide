package iot.challenge.mide.location.accuracy.trilaterization;

import iot.challenge.mide.location.accuracy.simulation.BeaconEvent;
import iot.challenge.mide.location.accuracy.simulation.BluetoothLeBeacon;

/**
 * Algorithm used to compute BLE beacon distance
 */
public enum DistanceAlgorithm {
	Linear("Linear") {
		@Override
		protected double distance(double rssi, double txpower) {
			double ratioDB = txpower - rssi;
			double ratioLinear = Math.pow(10, (double) ratioDB / 10);
			return Math.sqrt(ratioLinear);
		}
	},
	Accuracy("Accuracy") {
		@Override
		protected double distance(double rssi, double txpower) {
			double distance = 0d;

			double ratio = rssi / txpower;
			if (ratio < 1d) {
				distance = Math.pow(ratio, 10);
			} else {
				distance = 0.89976 * Math.pow(ratio, 7.7095) + 0.111;
			}

			return distance;
		}
	};

	private String name;

	private DistanceAlgorithm(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public double computeDistance(BeaconEvent event) {
		BluetoothLeBeacon beacon = event.getBeacon();
		return distance(beacon.getRssi(), beacon.getTxpower());
	}

	public double computeDistance(BeaconEvent event, DistanceUnits units) {
		return DistanceUnits.convert(
				computeDistance(event),
				DistanceUnits.METERS,
				DistanceUnits.MILIMETERS);
	}

	abstract double distance(double rssi, double txpower);
}
