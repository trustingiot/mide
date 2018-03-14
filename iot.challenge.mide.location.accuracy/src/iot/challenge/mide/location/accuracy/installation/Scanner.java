package iot.challenge.mide.location.accuracy.installation;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * Represents a scanner. It is composed of:
 * <ul>
 * <li>An address</li>
 * <li>A position</li>
 * </ul>
 */
public class Scanner {

	public final static String ADDR = "addr";
	public final static String POSITION = "position";

	private String addr;

	private Point position;

	public Scanner() {
		super();
		addr = null;
		position = null;
	}

	public Scanner(String addr, Point position) {
		this();
		this.addr = addr;
		this.position = position;
	}

	public String getAddr() {
		return addr;
	}

	public void setAddr(String addr) {
		this.addr = addr;
	}

	public Point getPosition() {
		return position;
	}

	public void setPosition(Point position) {
		this.position = position;
	}

	public Scanner copy() {
		Scanner scanner = new Scanner();
		scanner.addr = new String(addr);
		scanner.position = position.copy();
		return scanner;
	}

	@Override
	public String toString() {
		return toJson().toString();
	}

	public JsonObject toJson() {
		return new JsonObject()
				.add(ADDR, addr)
				.add(POSITION, position.toJson());
	}

	public static Scanner fromString(String message) {
		return Scanner.fromJson(Json.parse(message));
	}

	public static Scanner fromJson(JsonValue jsonValue) {
		Scanner scanner = new Scanner();
		JsonObject json = jsonValue.asObject();
		scanner.addr = json.get(ADDR).asString();
		scanner.position = Point.fromJson(json.get(POSITION));
		return scanner;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((addr == null) ? 0 : addr.hashCode());
		result = prime * result + ((position == null) ? 0 : position.hashCode());
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
		Scanner other = (Scanner) obj;
		if (addr == null) {
			if (other.addr != null)
				return false;
		} else if (!addr.equals(other.addr))
			return false;
		if (position == null) {
			if (other.position != null)
				return false;
		} else if (!position.equals(other.position))
			return false;
		return true;
	}

}
