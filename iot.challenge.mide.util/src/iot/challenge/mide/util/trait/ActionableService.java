package iot.challenge.mide.util.trait;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.message.KuraPayload;

import iot.challenge.mide.util.Options;

/**
 * Allows the classes that implements it to deploy an service actionable using
 * MQTT messages.
 * 
 * Extends the CloudClientAdapter
 */
public interface ActionableService extends CloudClientAdapter {

	/**
	 * Cancel action
	 */
	public static final String CANCEL = "cancel";

	/**
	 * Creates cancel payload
	 * 
	 * @return Cancel payload
	 */
	public static KuraPayload createCancelPayload() {
		KuraPayload result = new KuraPayload();
		result.addMetric(CANCEL, true);
		return result;
	}

	/**
	 * Creates a cloud client for the ActionableService
	 * 
	 * @param cloudService
	 *            Cloud service
	 * @param appId
	 *            Application id
	 */
	default void connectToCloud(CloudService cloudService, String appId) {
		CloudClient cloudClient = null;
		try {
			cloudClient = cloudService.newCloudClient(appId);
			cloudClient.addCloudClientListener(this);
			setCloudClient(cloudClient);
			subscribeToControlTopic();
		} catch (KuraException e) {
			error("Unable to get CloudClient", e);
			setCloudClient(null);
		}
	}

	/**
	 * Subscribes to control topic
	 * 
	 * @throws KuraException
	 */
	default void subscribeToControlTopic() throws KuraException {
		CloudClient cloudClient = getCloudClient();
		if (cloudClient != null) {
			try {
				cloudClient.subscribe(getControlTopic(), 1);
			} catch (KuraException e) {
				error("Unable to subscribe to CloudClient", e);
				cloudClient.removeCloudClientListener(this);
				throw e;
			}
		}
	}

	/**
	 * Release ActionableService cloud client
	 */
	default void releaseCloudConnection() {
		CloudClient cloudClient = getCloudClient();
		if (cloudClient != null) {
			cloudClient.removeCloudClientListener(this);
			try {
				cloudClient.unsubscribe(getControlTopic());
			} catch (KuraException e) {
				error("Unable to unsubscribe from CloudClient", e);
			}
			cloudClient.release();
			setCloudClient(null);
		}
	}

	/**
	 * Clean control topic message
	 */
	default void cleanControlTopic() {
		CloudClient cloudClient = getCloudClient();
		if (cloudClient != null) {
			try {
				cloudClient.publish(getControlTopic(), null, 1, true);
			} catch (KuraException e) {
				error("Unable to publish to CloudClient", e);
			}
		}
	}

	@Override
	default void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
		if (appTopic.equals(getControlTopic())) {
			if (msg.metrics().containsKey(CANCEL)) {
				cancelService();
				cleanControlTopic();
			} else {
				configureService(msg);
			}
		} else {
			CloudClientAdapter.super.onMessageArrived(deviceId, appTopic, msg, qos, retain);
		}
	}

	/**
	 * Configures the service using the payload's metrics
	 * 
	 * @param payload
	 *            Payload
	 */
	default void configureService(KuraPayload payload) {
		if (!payload.metrics().isEmpty()) {
			Map<String, Object> options = readOptions(payload);
			setOptions(createOptions(options));
		}
	}

	/**
	 * Reads payload's metrics
	 * 
	 * @param payload
	 *            Payload
	 * 
	 * @return Payload's metrics
	 */
	default Map<String, Object> readOptions(KuraPayload payload) {
		Map<String, Object> configuration = new HashMap<>();
		payload.metrics().forEach(configuration::put);
		return configuration;
	}

	/**
	 * Returns service cloud client
	 * 
	 * @return Service cloud client
	 */
	CloudClient getCloudClient();

	/**
	 * Sets service cloud client
	 * 
	 * @param cloudClient
	 *            Service cloud client
	 */
	void setCloudClient(CloudClient cloudClient);

	/**
	 * Returns service options
	 * 
	 * @return Service options
	 */
	Options getOptions();

	/**
	 * Sets service options
	 * 
	 * @param options
	 *            Service options
	 */
	void setOptions(Options options);

	/**
	 * Returns service control topic
	 * 
	 * @return Service control topic
	 */
	String getControlTopic();

	/**
	 * Creates service options
	 * 
	 * @param configuration
	 *            Configuration
	 * 
	 * @return Service options
	 */
	Options createOptions(Map<String, Object> configuration);

	/**
	 * Cancel service execution
	 */
	void cancelService();

}
