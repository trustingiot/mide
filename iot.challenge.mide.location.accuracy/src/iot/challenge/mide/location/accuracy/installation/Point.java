package iot.challenge.mide.location.accuracy.installation;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * Represents a 2-dimensional point
 */
public class Point {

	public final static String X = "X";
	public final static String Y = "Y";

	private Integer x;

	private Integer y;

	public Point() {
		super();
		x = null;
		y = null;
	}

	public Point(Integer x, Integer y) {
		this();
		this.x = x;
		this.y = y;
	}

	public Integer getX() {
		return x;
	}

	public void setX(Integer x) {
		this.x = x;
	}

	public Integer getY() {
		return y;
	}

	public void setY(Integer y) {
		this.y = y;
	}

	public Point copy() {
		Point point = new Point();
		point.x = new Integer(x);
		point.y = new Integer(y);
		return point;
	}

	@Override
	public String toString() {
		return toJson().toString();
	}

	public JsonObject toJson() {
		return new JsonObject()
				.add(X, x)
				.add(Y, y);
	}

	public static Point fromString(String message) {
		return fromJson(Json.parse(message));
	}

	public static Point fromJson(JsonValue jsonValue) {
		Point point = new Point();
		JsonObject json = jsonValue.asObject();
		point.x = json.get(X).asInt();
		point.y = json.get(Y).asInt();
		return point;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((x == null) ? 0 : x.hashCode());
		result = prime * result + ((y == null) ? 0 : y.hashCode());
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
		Point other = (Point) obj;
		if (x == null) {
			if (other.x != null)
				return false;
		} else if (!x.equals(other.x))
			return false;
		if (y == null) {
			if (other.y != null)
				return false;
		} else if (!y.equals(other.y))
			return false;
		return true;
	}

}
