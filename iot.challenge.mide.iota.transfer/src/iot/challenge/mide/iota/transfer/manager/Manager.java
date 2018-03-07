package iot.challenge.mide.iota.transfer.manager;

import iot.challenge.mide.util.MqttProcessor;
import iot.challenge.mide.util.trait.ActionRecorder;
import iot.challenge.mide.util.trait.DataServiceAdapter;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudPayloadProtoBufDecoder;
import org.eclipse.kura.cloud.CloudPayloadProtoBufEncoder;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;

/**
 * Group manager
 */
public class Manager implements ActionRecorder, ConfigurableComponent, DataServiceAdapter {

	public static final String MODE_COMPETITIVE = "competitive";
	public static final String MODE_COLLABORATIVE = "collaborative";

	public static final String SUB(String topic, String sub) {
		return topic + "/" + sub;
	};

	public static final String ALL(String topic) {
		return SUB(topic, "#");
	};

	public static final String BASE_TOPIC = "/mide/iota";

	public static final String CONFIG_TOPIC = SUB(BASE_TOPIC, "configuration");
	public static final String WORKER_TOPIC = SUB(BASE_TOPIC, "worker");

	public static final String TODO_TOPIC = SUB(BASE_TOPIC, "todo");
	public static final String DONE_TOPIC = SUB(BASE_TOPIC, "done");

	////
	//
	// Action recorder
	//
	//
	public static final String ID = "iot.challenge.mide.iota.transfer.efficiency.group";

	@Override
	public String getID() {
		return ID;
	}

	////
	//
	// Parameters
	//
	//
	protected Options options;
	protected String address;
	protected Long id;
	protected int index;
	protected int received;
	protected int transmitted;

	protected boolean execute = false;
	protected boolean subscribed = false;

	protected Map<Integer, Long> queued;
	protected Map<String, Boolean> workers;

	////
	//
	// Registered services
	//
	//
	protected ConfigurationService configurationService;
	protected DataService dataService;
	protected CloudPayloadProtoBufDecoder decoder;
	protected CloudPayloadProtoBufEncoder encoder;

	protected void setConfigurationService(ConfigurationService service) {
		configurationService = service;
	}

	protected void unsetConfigurationService(ConfigurationService service) {
		configurationService = null;
	}

	protected void setDataService(DataService service) {
		dataService = service;
	}

	protected void unsetDataService(DataService service) {
		dataService = null;
	}

	protected void setCloudPayloadProtoBufDecoder(CloudPayloadProtoBufDecoder decoder) {
		this.decoder = decoder;
	}

	protected void unsetCloudPayloadProtoBufDecoder(CloudPayloadProtoBufDecoder decoder) {
		this.decoder = null;
	}

	protected void setCloudPayloadProtoBufEncoder(CloudPayloadProtoBufEncoder encoder) {
		this.encoder = encoder;
	}

	protected void unsetCloudPayloadProtoBufEncoder(CloudPayloadProtoBufEncoder encoder) {
		this.encoder = null;
	}

	////
	//
	// Service methods
	//
	//
	protected void activate(ComponentContext context, Map<String, Object> properties) {
		performRegisteredAction("Activating", this::activate, properties);
	}

	protected void updated(ComponentContext context, Map<String, Object> properties) {
		performRegisteredAction("Updating", this::update, properties);
	}

	protected void deactivate(ComponentContext context, Map<String, Object> properties) {
		performRegisteredAction("Deactivating", this::deactivate);
	}

	////
	//
	// Functionality
	//
	//
	protected void activate(Map<String, Object> properties) {
		queued = new HashMap<>();
		workers = new HashMap<>();
		startSubscription();
		createOptions(properties);
	}

	protected void startSubscription() {
		try {
			dataService.addDataServiceListener(this);
			dataService.subscribe(ALL(WORKER_TOPIC), 0);
			dataService.subscribe(ALL(DONE_TOPIC), 0);
			subscribed = true;
		} catch (KuraException e) {
			error("Unable to subscribe to topics", e);
			dataService.removeDataServiceListener(this);
			subscribed = false;
		}
	}

	protected void createOptions(Map<String, Object> properties) {
		stopExecution();
		options = new Options(properties);
	}

	protected void stopExecution() {
		synchronized (this) {
			cancelAllWork();
			for (String id : workers.keySet())
				workers.put(id, true);
		}
	}

	protected void update(Map<String, Object> properties) {
		createOptions(properties);
		if (options.isEnable()) {
			execute = true;
			disable(properties);
		} else {
			if (execute) {
				execute = false;
				execute();
			}
		}
	}

	protected void deactivate() {
		stopExecution();
		if (subscribed) {
			dataService.removeDataServiceListener(this);
		}
	}

	private void disable(Map<String, Object> properties) {
		Map<String, Object> map = new HashMap<>(properties);
		map.put(Options.PROPERTY_ENABLE, false);
		try {
			configurationService.updateConfiguration(ID, map);
		} catch (KuraException e) {
			error("Unable to update configuartion", e);
		}
	}

	protected void execute() {
		updateConfiguration();
		this.address = options.getIotaAddress();

		while (address.length() < 81)
			address = '9' + address;

		this.id = System.currentTimeMillis();
		this.index = 0;
		this.transmitted = 0;
		this.received = 0;
		this.queued.clear();

		info("Execution started");
		handleNext();
	}

	protected void updateConfiguration() {
		KuraPayload payload = new KuraPayload();
		payload.addMetric(Options.PROPERTY_IOTA_NODE_HOST, options.getIotaNodeHost());
		payload.addMetric(Options.PROPERTY_IOTA_NODE_PORT, options.getIotaNodePort());
		payload.addMetric(Options.PROPERTY_IOTA_NODE_PROTOCOL, options.getIotaNodeProtocol());
		payload.addMetric(Options.PROPERTY_MODE, options.getMode());
		try {
			dataService.publish(CONFIG_TOPIC, encoder.getBytes(payload, false), 2, true, 1);
		} catch (Exception e) {
			error("Unable to update configuration");
		}
	}

	protected void handleNext() {
		if (options.getMode().equals(MODE_COMPETITIVE)) {
			cancelAllWork();
		}

		if (transmitted < options.getTransactions()) {
			if (index < options.getTransactions()) {
				submitWork();
			}
		} else {
			showResults();
		}
	}

	protected void submitWork() {
		if (options.getMode().equals(MODE_COMPETITIVE)) {
			startCompetitiveWork();

		} else {
			String worker = findFirstFreeWorker();
			if (worker != null) {
				transfer(worker);
			}
			if (findFirstFreeWorker() != null && index < options.getTransactions()) {
				handleNext();
			}
		}
	}

	protected String findFirstFreeWorker() {
		return workers.keySet().stream().filter(workers::get).findFirst().orElse(null);
	}

	protected void showResults() {
		int loss = options.getTransactions() - received;
		long duration = (System.currentTimeMillis() - id);
		double efficiency = (double) duration / (double) received;

		info("Test results:\nTransmitted: {}\nReceived: {}\nLoss: {}\nDuration: {} milliseconds\nEfficiency: {} milliseconds each transaction or {} transactions per hour",
				options.getTransactions(), received, loss, duration, efficiency, 3600000d / efficiency);

		id = null;
	}

	protected void cancelAllWork() {
		publishAll(new byte[0]);
	}

	protected void publishAll(byte[] payload) {
		try {
			Map<String, Boolean> aux = new HashMap<>();
			for (String id : workers.keySet()) {
				dataService.publish(SUB(TODO_TOPIC, id), payload, 2, true, 0);
				aux.put(id, payload.length == 0);
			}
			workers = aux;
		} catch (KuraException e) {
			error("Send todo failed", e);
		}
	}

	protected void startCompetitiveWork() {
		try {
			queued.put(index + 1, System.currentTimeMillis());
			publishAll(encoder.getBytes(generatePayload(), false));
		} catch (KuraException e) {
			error("Unable to generate payload", e);
		}
	}

	protected KuraPayload generatePayload() {
		KuraPayload payload = new KuraPayload();
		payload.addMetric("address", address);
		payload.addMetric("app", "mide");
		payload.addMetric("id", id);
		payload.addMetric("n", options.getTransactions());
		payload.addMetric("index", ++index);
		return payload;
	}

	protected void transfer(String worker) {
		try {
			dataService.publish(SUB(TODO_TOPIC, worker), encoder.getBytes(generatePayload(), false), 2, true, 0);
			queued.put(index, System.currentTimeMillis());
			workers.put(worker, false);
		} catch (KuraException e) {
			error("Unable to transfer message");
		}
	}

	@Override
	public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
		if (MqttProcessor.matches(ALL(WORKER_TOPIC), topic)) {
			assignWorker(topic, payload);

		} else if (MqttProcessor.matches(ALL(DONE_TOPIC), topic)) {
			done(topic, payload);
		}
	}

	protected void assignWorker(String topic, byte[] payload) {
		if (MqttProcessor.matches(ALL(WORKER_TOPIC), topic)) {
			String[] tokens = topic.split("/");
			String id = tokens[tokens.length - 1];

			if (payload.length > 0) {
				workers.put(id, true);
			} else {
				workers.remove(id);
			}
		}
	}

	protected void done(String topic, byte[] payload) {
		try {
			if (id != null) {
				KuraPayload kuraPayload = decoder.buildFromByteArray(payload);
				int index = Integer.parseInt((String) kuraPayload.getMetric("index"));

				if (queued.containsKey(index)) {
					transmitted++;
					String worker = (String) kuraPayload.getMetric("worker");
					String hash = (String) kuraPayload.getMetric("hash");
					if (!hash.isEmpty()) {
						received++;
						logTransfer(queued.remove(index), index, worker, hash);
					}
					workers.put(worker, true);
					handleNext();
				}
			}

		} catch (KuraException e) {
			error("Unable to decode payload", e);
		}
	}

	protected void logTransfer(Long time, int index, String worker, String hash) {
		info("{} of {}: Worker: {} -> https://thetangle.org/transaction/{} ({} seconds)",
				index,
				options.getTransactions(),
				worker,
				hash,
				(System.currentTimeMillis() - time) / 1000);
	}
}
