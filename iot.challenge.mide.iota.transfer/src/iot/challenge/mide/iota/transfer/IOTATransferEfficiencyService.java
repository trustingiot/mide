package iot.challenge.mide.iota.transfer;

import iot.challenge.mide.util.trait.ActionRecorder;
import jota.IotaAPI;
import jota.IotaLocalPoW;
import jota.dto.response.SendTransferResponse;
import jota.error.ArgumentException;
import jota.model.Transfer;
import jota.utils.TrytesConverter;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.osgi.service.component.ComponentContext;

import cfb.pearldiver.PearlDiver;
import cfb.pearldiver.PearlDiverLocalPoW;
import com.eclipsesource.json.JsonObject;

/**
 * IOTAService provider
 */
public class IOTATransferEfficiencyService implements ActionRecorder, ConfigurableComponent {

	////
	//
	// Action recorder
	//
	//
	public static final String ID = "iot.challenge.mide.iota.transfer.efficiency";

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
	protected List<Transfer> transfers;
	protected String address;
	protected Long id;
	protected int index;
	protected int received;

	protected ExecutorService transferWorker;
	protected Future<?> transferHandle;

	////
	//
	// Registered services
	//
	//
	protected ConfigurationService configurationService;

	protected void setConfigurationService(ConfigurationService service) {
		configurationService = service;
	}

	protected void unsetConfigurationService(ConfigurationService service) {
		configurationService = null;
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
		createPoW();
		createOptions(properties);
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

	protected void createOptions(Map<String, Object> properties) {
		options = new Options(properties);
		updateAPI();
	}

	protected void updateAPI() {
		if (shouldUpdateAPI()) {
			api = new IotaAPI.Builder()
					.localPoW(pow)
					.protocol(options.getIotaNodeProtocol())
					.host(options.getIotaNodeHost())
					.port(options.getIotaNodePort())
					.build();
		}
	}

	protected boolean shouldUpdateAPI() {
		if (api == null)
			return true;

		if (!api.getProtocol().equals(options.getIotaNodeProtocol()))
			return true;

		if (!api.getHost().equals(options.getIotaNodeHost()))
			return true;

		if (!api.getPort().equals(options.getIotaNodePort()))
			return true;

		return false;
	}

	protected void update(Map<String, Object> properties) {
		shutdownWorker();
		createOptions(properties);
		if (options.isEnable()) {
			execute();
			disable(properties);
		}
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

	protected void deactivate() {
		shutdownWorker();
	}

	protected void interrupt() {
		if (id != null) {
			diver.cancel();
			api.interruptAttachingToTangle();
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
		this.address = options.getIotaAddress();

		while (address.length() < 81)
			address = '9' + address;

		this.id = System.currentTimeMillis();
		this.index = 0;
		this.received = 0;

		info("Execution started");
		next();
	}

	protected void next() {
		if (index < options.getTransactions()) {
			JsonObject message = new JsonObject();
			message.add("app", "mide");
			message.add("id", id);
			message.add("n", options.getTransactions());
			message.add("i", ++index);
			transfer(address, message.toString());

		} else {
			int loss = options.getTransactions() - received;
			long duration = (System.currentTimeMillis() - id);
			double efficiency = (double) duration / (double) received;

			info("Test results:\nTransmitted: {}\nReceived: {}\nLoss: {}\nDuration: {} milliseconds\nEfficiency: {} milliseconds each transaction or {} transactions per hour",
					options.getTransactions(), received, loss, duration, efficiency, 3600000d / efficiency);
			
			id = null;
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

		try {
			long time = System.currentTimeMillis();
			response = sendToIOTA(transfers);
			logTransfer(time, response);
			received++;
		} catch (Exception exception) {
			error("Transfer fail");
		}

		transferHandle = null;
		next();
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

	protected void logTransfer(long time, SendTransferResponse transfer) {
		info("{} of {} -> https://thetangle.org/transaction/{} ({} seconds)",
				index,
				options.getTransactions(),
				transfer.getTransactions().get(0).getHash(),
				(System.currentTimeMillis() - time) / 1000);
	}
}
