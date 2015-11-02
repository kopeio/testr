package io.kope.utils;

import com.google.gson.Gson;

public class DebugUtils {

	static final Gson gson = new Gson();

	public static String toJson(Object o) {
		if (o == null) {
			return null;
		}
		return gson.toJson(o);
	}

}
