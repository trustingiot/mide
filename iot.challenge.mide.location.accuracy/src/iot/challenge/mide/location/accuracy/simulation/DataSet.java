package iot.challenge.mide.location.accuracy.simulation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

import iot.challenge.mide.location.accuracy.installation.Installation;
import iot.challenge.mide.location.accuracy.installation.Point;

public class DataSet {

	private static final String INSTALLATION = "installation.txt";
	private static final Pattern RECORDING = Pattern.compile("x\\d+y\\d+\\.txt");

	private Installation installation;
	private Map<Point, Recording> recordings;

	private DataSet() {
		super();
		recordings = new HashMap<>();
	}

	public DataSet(File folder) throws Exception {
		this();
		for (File file : folder.listFiles()) {
			if (isRecordingFile(file)) {
				String name = file.getName();
				int x = Integer.parseInt(name.substring(1, name.indexOf('y')));
				int y = Integer.parseInt(name.substring(name.indexOf('y') + 1, name.indexOf('.')));
				recordings.put(new Point(x, y), readRecording(readFile(file)));
			} else if (isInstallationFile(file)) {
				installation = Installation.fromString(readFile(file));
			} else {
				throw new IllegalArgumentException("Unknown file");
			}
		}
	}

	public Installation getInstallation() {
		return installation;
	}

	public Map<Point, Recording> getRecordings() {
		return recordings;
	}

	private static boolean isInstallationFile(File f) {
		return f.getName().equals(INSTALLATION);
	}

	private static boolean isRecordingFile(File f) {
		return RECORDING.matcher(f.getName()).matches();
	}

	private static String readFile(File f) throws IOException {
		return new String(Files.readAllBytes(f.toPath()));
	}

	private static Recording readRecording(String recording) {
		JsonValue json = Json.parse(recording);
		recording = json.asObject().get("recording").toString().replaceAll("\\\\\"", "\"");
		recording = recording.substring(1, recording.length() - 1);
		Recording.fromString(recording);
		return Recording.fromString(recording);
	}
}
