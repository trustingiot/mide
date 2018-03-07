package iot.challenge.mide.iota.transfer.worker;

import iot.challenge.mide.iota.transfer.manager.Manager;
import iot.challenge.mide.util.MqttProcessor;
import iot.challenge.mide.util.trait.ActionRecorder;
import iot.challenge.mide.util.trait.DataServiceAdapter;
import jota.IotaAPI;
import jota.IotaLocalPoW;
import jota.dto.response.SendTransferResponse;
import jota.error.ArgumentException;
import jota.model.Transfer;
import jota.utils.TrytesConverter;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudPayloadProtoBufDecoder;
import org.eclipse.kura.cloud.CloudPayloadProtoBufEncoder;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;

import com.eclipsesource.json.JsonObject;

import cfb.pearldiver.PearlDiver;
import cfb.pearldiver.PearlDiverLocalPoW;

/**
 * Worker
 */
public class Worker implements ActionRecorder, ConfigurableComponent, DataServiceAdapter {

	////
	//
	// Action recorder
	//
	//
	public static final String ID = "iot.challenge.mide.iota.transfer.worker";

	@Override
	public String getID() {
		return ID;
	}

	////
	//
	// Parameters
	//
	//
	protected IotaAPI api;
	protected IotaLocalPoW pow;
	protected PearlDiver diver;

	protected Options options;
	protected String protocol;
	protected String host;
	protected String port;
	protected String mode;

	protected KuraPayload buffer = null;

	protected List<Transfer> transfers;
	protected String address;
	protected boolean wip;
	protected boolean interrupted = false;

	protected ExecutorService transferWorker;
	protected Future<?> transferHandle;

	////
	//
	// Registered services
	//
	//
	protected DataService dataService;
	protected CloudPayloadProtoBufDecoder decoder;
	protected CloudPayloadProtoBufEncoder encoder;

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
		createOptions(properties);
		createPoW();
		startSubscription();
	}

	protected void createOptions(Map<String, Object> properties) {
		options = new Options(properties);
		updateAPI();
	}

	protected void updateAPI() {
		if (options != null && host != null) {
			if (shouldUpdateAPI()) {
				shutdownWorker();
				api = new IotaAPI.Builder()
						.localPoW(pow)
						.protocol(protocol)
						.host(host)
						.port(port)
						.build();
			}
		}
	}

	protected boolean shouldUpdateAPI() {
		if (api == null)
			return true;

		if (!api.getProtocol().equals(protocol))
			return true;

		if (!api.getHost().equals(host))
			return true;

		if (!api.getPort().equals(port))
			return true;

		return false;
	}

	protected void createPoW() {
		try {
			pow = new PearlDiverLocalPoW();
			Field field = Arrays.asList(PearlDiverLocalPoW.class.getDeclaredFields())
					.stream()
					.filter(f -> f.getType().equals(PearlDiver.class))
					.findFirst()
					.orElse(null);

			if (field == null)
				throw new IllegalArgumentException();

			boolean flag = field.isAccessible();
			field.setAccessible(true);
			diver = (PearlDiver) field.get(pow);
			field.setAccessible(flag);

		} catch (Exception e) {
			error("Create IOTA local PoW failed");
			pow = null;
			diver = null;
		}
	}

	protected void startSubscription() {
		try {
			dataService.addDataServiceListener(this);
			dataService.subscribe(Manager.CONFIG_TOPIC, 0);
			dataService.subscribe(Manager.SUB(Manager.TODO_TOPIC, options.getID()), 0);
			notifiWorker();
		} catch (KuraException e) {
			error("Unable to subscribe to topics", e);
			dataService.removeDataServiceListener(this);
		}
	}

	protected void notifiWorker() throws KuraException {
		KuraPayload payload = new KuraPayload();
		payload.addMetric("free", true);
		dataService.publish(Manager.SUB(Manager.WORKER_TOPIC, options.getID()), encoder.getBytes(payload, false), 2,
				true, 0);
	}

	protected void update(Map<String, Object> properties) {
		shutdownWorker();
		createOptions(properties);
	}

	protected void shutdownWorker() {
		synchronized (this) {
			interrupt();

			if (transferHandle != null) {
				transferHandle.cancel(true);
				transferHandle = null;
			}

			if (transferWorker != null) {
				transferWorker.shutdown();
				transferWorker = null;
			}
		}
	}

	protected void interrupt() {
		if (wip) {
			interrupted = true;
			diver.cancel();
		}
		wip = false;
		buffer = null;
	}

	protected void deactivate() {
		shutdownWorker();
		dataService.removeDataServiceListener(this);
	}

	@Override
	public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
		if (MqttProcessor.matches(Manager.CONFIG_TOPIC, topic)) {
			configure(payload);

		} else if (MqttProcessor.matches(Manager.SUB(Manager.TODO_TOPIC, options.getID()), topic)) {
			if (payload.length == 0) {
				shutdownWorker();
			} else {
				todo(payload);
			}
		}
	}

	protected void configure(byte[] payload) {
		try {
			KuraPayload kuraPayload = decoder.buildFromByteArray(payload);
			host = (String) kuraPayload
					.getMetric(iot.challenge.mide.iota.transfer.manager.Options.PROPERTY_IOTA_NODE_HOST);
			port = (String) kuraPayload
					.getMetric(iot.challenge.mide.iota.transfer.manager.Options.PROPERTY_IOTA_NODE_PORT);
			protocol = (String) kuraPayload
					.getMetric(iot.challenge.mide.iota.transfer.manager.Options.PROPERTY_IOTA_NODE_PROTOCOL);
			mode = (String) kuraPayload.getMetric(iot.challenge.mide.iota.transfer.manager.Options.PROPERTY_MODE);
			updateAPI();
		} catch (KuraException e) {
			error("Unable to configure");
		}
	}

	protected void todo(byte[] payload) {
		try {
			KuraPayload kuraPayload = decoder.buildFromByteArray(payload);
			buffer = kuraPayload;
			transferBuffer();
		} catch (KuraException e) {
			error("Unable to decode");
		}
	}

	protected void transferBuffer() {
		if (buffer != null) {
			String address = (String) buffer.getMetric("address");
			JsonObject message = new JsonObject();
			message.add("app", (String) buffer.getMetric("app"));
			message.add("id", (long) buffer.getMetric("id"));
			message.add("n", (Integer) buffer.getMetric("n"));
			message.add("index", (Integer) buffer.getMetric("index"));
			transfer(address, message.toString());
		}
	}

	protected void transfer(String address, String message) {
		if (transferWorker == null)
			transferWorker = Executors.newScheduledThreadPool(1);

		Transfer transfer = new Transfer(address, 0, TrytesConverter.toTrytes(message), "");
		transfers = Arrays.asList(transfer);
		transferHandle = transferWorker.submit(this::sendTransfer);
	}

	protected void sendTransfer() {
		SendTransferResponse response = null;
		wip = true;
		try {
			int index = (int) buffer.getMetric("index");
			info("Started transmission {}", index);
			interrupted = false;
			response = sendToIOTA(transfers);
			String hash = response.getTransactions().get(0).getHash();
			done(hash);
		} catch (Exception e) {
			if (!interrupted) {
				error("Transfer fail");
			} else {
				done("");
			}
		}

		transferHandle = null;
		wip = false;
	}

	protected SendTransferResponse sendToIOTA(List<Transfer> transfers) throws ArgumentException {
		return api.sendTransfer(
				options.getIotaSeed(), // Seed
				2, // Security
				9, // Depth
				14, // Min weight
				transfers, // Transfers
				null, // Inputs
				null, // Remainders
				false); // Validate inputs
	}

	protected void done(String hash) {
		String workID = Integer.toString((int) buffer.getMetric("index"));
		String workerID = options.getID();

		KuraPayload payload = new KuraPayload();
		payload.addMetric("index", workID);
		payload.addMetric("worker", workerID);
		payload.addMetric("hash", hash);
		try {
			dataService.publish(Manager.SUB(Manager.DONE_TOPIC, workID), encoder.getBytes(payload, false), 2, true, 0);
		} catch (Exception e) {
			error("Unable to assign buffer");
		}
		buffer = null;
	}
}
