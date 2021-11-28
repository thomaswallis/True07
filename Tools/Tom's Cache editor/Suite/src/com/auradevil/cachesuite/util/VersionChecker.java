package com.auradevil.cachesuite.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;

/**
 * @author tom
 */
public class VersionChecker {
	public static float getLatestVersion() throws IOException {
		URL u = new URL("http://www.nuke-net.com/suitever.txt");
		DataInputStream page = new DataInputStream(u.openStream());
		return Float.parseFloat(page.readLine());
	}
}
