package iot.challenge.mide.util;

/**
 * Utilities to process MQTT topics
 */
public class MqttProcessor {

	/**
	 * Check if the MQTT topic matches the filter
	 * 
	 * Adapted from
	 * https://github.com/eclipse/paho.mqtt.java/blob/f85680da7e37f4cacfc07bf7ac93c92f31aeb520/org.eclipse.paho.client.mqttv3/src/main/java/org/eclipse/paho/client/mqttv3/MqttTopic.java#L248
	 * 
	 * @param filter
	 *            Topic filter
	 * @param topic
	 *            Topic name
	 * 
	 * @return true if the topic matches the filter
	 */
	public static boolean matches(String filter, String topic) {
		int tp = 0;
		int fp = 0;

		char[] ta = topic.toCharArray();
		char[] fa = filter.toCharArray();

		int tl = ta.length;
		int fl = fa.length;

		if (filter.equals(topic))
			return true;

		while (tp < tl && fp < fl) {
			if (ta[tp] == '/' && fa[fp] != '/')
				return false;

			if (fa[fp] != '+' && fa[fp] != '#' && fa[fp] != ta[tp])
				return false;

			if (fa[fp] == '+') {
				while ((tp + 1) < tl && ta[tp + 1] != '/') {
					tp++;
				}

			} else if (fa[fp] == '#') {
				tp = tl - 1;
			}

			tp++;
			fp++;
		}

		return (tp == tl) && (fp == fl);
	}
}
