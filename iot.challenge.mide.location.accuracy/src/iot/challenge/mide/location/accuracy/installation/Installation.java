package iot.challenge.mide.location.accuracy.installation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * Represents a installation. It is composed of:
 * <ul>
 * <li>An id</li>
 * <li>A list of points that describe its plane</li>
 * <li>A list of scanners</li>
 * </ul>
 */
public class Installation {

	public final static String ID = "id";
	public final static String POINTS = "points";
	public final static String SCANNERS = "scanners";

	private String id;

	private List<Point> points;

	private List<Scanner> scanners;

	public Installation() {
		super();
		id = null;
		points = null;
		scanners = null;
	}

	public Installation(String id) {
		this();
		this.id = id;
		this.points = new ArrayList<>();
		this.scanners = new ArrayList<>();
	}

	public Installation(String id, List<Point> points) {
		this();
		this.id = id;
		this.points = points;
		this.scanners = new ArrayList<>();
	}

	public Installation(String id, List<Point> points, List<Scanner> scanners) {
		this();
		this.id = id;
		this.points = points;
		this.scanners = scanners;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<Point> getPoints() {
		return points;
	}

	public void setPoints(List<Point> points) {
		this.points = points;
	}

	public List<Scanner> getScanners() {
		return scanners;
	}

	public void setScanners(List<Scanner> scanners) {
		this.scanners = scanners;
	}

	public void addPoint(Point point) {
		points.add(point);
	}

	public void insertPoint(int index, Point point) {
		points.add(index, point);
	}

	public void removePoint(int index) {
		points.remove(index);
	}

	public void addScanner(Scanner scanner) {
		scanners.add(scanner);
	}

	public void removeScanner(int index) {
		scanners.remove(index);
	}

	public void removeScanner(Scanner scanner) {
		scanners.remove(scanner);
	}

	public Installation copy() {
		Installation installation = new Installation();
		installation.id = new String(id);
		installation.points = copy(points, Point::copy);
		installation.scanners = copy(scanners, Scanner::copy);
		return installation;
	}

	private static <T> List<T> copy(List<T> list, Function<T, T> copyElement) {
		return list.stream()
				.map(copyElement::apply)
				.collect(Collectors.toList());
	}

	@Override
	public String toString() {
		return toJson().toString();
	}

	public JsonObject toJson() {
		return new JsonObject()
				.add(ID, id)
				.add(POINTS, toJson(points, Point::toJson))
				.add(SCANNERS, toJson(scanners, Scanner::toJson));
	}

	private static <T> JsonArray toJson(List<T> list, Function<T, JsonObject> elementToJson) {
		JsonArray json = new JsonArray();
		list.stream()
				.map(elementToJson::apply)
				.forEach(json::add);
		return json;
	}

	public static Installation fromString(String message) {
		return Installation.fromJson(Json.parse(message));
	}

	public static Installation fromJson(JsonValue jsonValue) {
		JsonObject json = jsonValue.asObject();
		Installation installation = new Installation();
		installation.id = json.get(ID).asString();
		installation.points = read(json.get(POINTS), Point::fromJson);
		installation.scanners = read(json.get(SCANNERS), Scanner::fromJson);
		return installation;
	}

	private static <T> List<T> read(JsonValue json, Function<JsonValue, T> jsonToElement) {
		List<T> result = new ArrayList<>();
		json.asArray().forEach(entry -> result.add(jsonToElement.apply(entry)));
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((points == null) ? 0 : points.hashCode());
		result = prime * result + ((scanners == null) ? 0 : scanners.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Installation other = (Installation) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (points == null) {
			if (other.points != null)
				return false;
		} else if (!points.equals(other.points))
			return false;
		if (scanners == null) {
			if (other.scanners != null)
				return false;
		} else if (!scanners.equals(other.scanners))
			return false;
		return true;
	}

}
