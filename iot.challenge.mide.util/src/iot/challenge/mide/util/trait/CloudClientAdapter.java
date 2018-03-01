package iot.challenge.mide.util.trait;

import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.message.KuraPayload;

/**
 * Adapter for CloudClientListener. By default all methods log a debug message.
 * 
 * Implements the ActionRecorder trait and CloudClientListener interface
 */
public interface CloudClientAdapter extends CloudClientListener, ActionRecorder {

	@Override
	default void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
		log(getLogLevel(), "{}.onControlMessageArrived(deviceId: {}, appTopic: {}, msg: {}, qos: {}, retain: {})",
				getID(), deviceId, appTopic, msg, qos, retain);
	}

	@Override
	default void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
		log(getLogLevel(), "{}.onMessageArrived(deviceId: {}, appTopic: {}, msg: {}, qos: {}, retain: {})", getID(),
				deviceId, appTopic, msg, qos, retain);
	}

	@Override
	default void onConnectionLost() {
		log(getLogLevel(), "{}.onConnectionLost()", getID());
	}

	@Override
	default void onConnectionEstablished() {
		log(getLogLevel(), "{}.onConnectionEstablished()", getID());
	}

	@Override
	default void onMessageConfirmed(int messageId, String appTopic) {
		log(getLogLevel(), "{}.onMessageConfirmed(messageId: {}, appTopic: {})", getID(), messageId, appTopic);
	}

	@Override
	default void onMessagePublished(int messageId, String appTopic) {
		log(getLogLevel(), "{}.onMessagePublished(messageId: {}, appTopic: {})", getID(), messageId, appTopic);
	}

	/**
	 * Returns log level (debug by default)
	 * 
	 * @return Log level
	 */
	default LogLevel getLogLevel() {
		return LogLevel.debug;
	}

}
