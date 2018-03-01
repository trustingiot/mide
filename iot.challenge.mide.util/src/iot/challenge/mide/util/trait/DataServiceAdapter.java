package iot.challenge.mide.util.trait;

import org.eclipse.kura.data.listener.DataServiceListener;

/**
 * Adapter for DataServiceListener. By default all methods log a debug message.
 * 
 * Implements the ActionRecorder trait and DataServiceListener interface
 */
public interface DataServiceAdapter extends ActionRecorder, DataServiceListener {

	@Override
	default void onConnectionEstablished() {
		log(getLogLevel(), "{}.onConnectionEstablished()", getID());
	}

	@Override
	default void onDisconnecting() {
		log(getLogLevel(), "{}.onDisconnecting()", getID());
	}

	@Override
	default void onDisconnected() {
		log(getLogLevel(), "{}.onDisconnected()", getID());
	}

	@Override
	default void onConnectionLost(Throwable cause) {
		log(getLogLevel(), "{}.onConnectionLost(cause: {})", getID(), cause);
	}

	@Override
	default void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
		log(getLogLevel(), "{}.onMessageArrived(topic: {}, payload: {}, qos: {}, retained: {})", getID(),
				topic, (payload != null) ? payload.length : payload, qos, retained);
	}

	@Override
	default void onMessagePublished(int messageId, String topic) {
		log(getLogLevel(), "{}.onMessagePublished(messageId: {}, topic: {})", getID(), messageId, topic);
	}

	@Override
	default void onMessageConfirmed(int messageId, String topic) {
		log(getLogLevel(), "{}.onMessageConfirmed(messageId: {}, topic: {})", getID(), messageId, topic);
	}

	/**
	 * Return log level (debug by default)
	 * 
	 * @return Log level
	 */
	default LogLevel getLogLevel() {
		return LogLevel.debug;
	}

}
