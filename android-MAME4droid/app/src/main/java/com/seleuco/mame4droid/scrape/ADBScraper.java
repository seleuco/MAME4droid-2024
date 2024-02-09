/*
 * This file is part of MAME4droid.
 *
 * Copyright (C) 2023 David Valdeita (Seleuco)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Linking MAME4droid statically or dynamically with other modules is
 * making a combined work based on MAME4droid. Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * In addition, as a special exception, the copyright holders of MAME4droid
 * give you permission to combine MAME4droid with free software programs
 * or libraries that are released under the GNU LGPL and with code included
 * in the standard release of MAME under the MAME License (or modified
 * versions of such code, with unchanged license). You may copy and
 * distribute such a system following the terms of the GNU GPL for MAME4droid
 * and the licenses of the other code concerned, provided that you include
 * the source code of that other code when and as the GNU GPL requires
 * distribution of source code.
 *
 * Note that people who make modified versions of MAME4idroid are not
 * obligated to grant this special exception for their modified versions; it
 * is their choice whether to do so. The GNU General Public License
 * gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version
 * which carries forward this exception.
 *
 * MAME4droid is dual-licensed: Alternatively, you can license MAME4droid
 * under a MAME license, as set out in http://mamedev.org/
 */

package com.seleuco.mame4droid.scrape;

import android.util.Log;

import com.seleuco.mame4droid.MAME4droid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Properties;

public class ADBScraper implements IScraper {

	private static final String TAG = "SCRAPPER-ADB";

	private static final int TIMEOUT = 5000;

	private static final String USER_AGENT = "MAME4droid/1.0";
	private static final String ADB_QUERY_MAME_URL = "http://adb.arcadeitalia.net/service_scraper.php?ajax=query_mame";
	private static final String FILE_TYPE_PNG = "png";
	private static final String FILE_TYPE_ICO = "ico";

	private static final String PROPERTIE_INGAME = "ingame";
	private static final String PROPERTIE_ICON = "icon";
	private static final String PROPERTIE_FLYER = "flyer";
	private static final String PROPERTIE_MARQUEE = "marquee";
	private static final String PROPERTIE_TITLE = "title";
	private static final String PROPERTIE_CABINET = "cabinet";

	private static final String DIR_INGAME = "snap";
	private static final String DIR_ICON = "icons";
	private static final String DIR_FLYER = "flyers";
	private static final String DIR_MARQUEE = "marquees";
	private static final String DIR_TITLE = "titles";
	private static final String DIR_CABINET = "cabinets";
	private static final String DIR_PROPERTIES = "scrape";
	private String scrapeDir;

	private MAME4droid mm = null;

	public ADBScraper(String dir, MAME4droid mm){
		scrapeDir = dir;
		this.mm = mm;
	}

	@Override
	public void setMAME4droid(MAME4droid mm) {
		this.mm = mm;
	}

	protected boolean needsDownload(Properties p, String k){

		if(PROPERTIE_ICON.equals(k) && !mm.getPrefsHelper().isScrapingIcons())
			return false;
		if(PROPERTIE_INGAME.equals(k) && !mm.getPrefsHelper().isScrapingSnapshots())
			return false;
		if(!PROPERTIE_ICON.equals(k) && !PROPERTIE_INGAME.equals(k) && !mm.getPrefsHelper().isScrapingAll())
			return false;

		if(p.get(k)==null) return true;
		return !Boolean.parseBoolean((String) p.get(k));
	}

	@Override
	public boolean scrape(String rom_name, int current) {
		try {

			Properties props = new Properties();
			String propsDir = scrapeDir + DIR_PROPERTIES;

			Files.createDirectories(Paths.get(propsDir));

			File propsFile = new File(propsDir + File.separator + rom_name + ".properties");

			Log.d(TAG, "processing: " + rom_name+ " num:"+current);

			boolean skip = false;

			if (propsFile.exists()) {
				FileInputStream in = new FileInputStream(propsFile);
				props.load(in);
				in.close();
				skip = true;
				Enumeration<Object> e = props.elements();
				while(e.hasMoreElements() && skip){
					skip = Boolean.parseBoolean((String)e.nextElement());
				}
			}

			if (skip) {
				Log.d(TAG, "skipping: " + rom_name);
				return false;
			}

			Log.d(TAG, "scraping: " + rom_name);

			String json = getJSON(ADB_QUERY_MAME_URL + "&game_name=" + rom_name, TIMEOUT);
			Log.d(TAG, "json: " + json);

			if (json == null) {
				Log.d(TAG, "Not json: " + rom_name);
				return true;
			}

			if (!isEmpty(json)) {

				String url_image_ingame = getImageURL("url_image_ingame", json);
				String url_image_title = getImageURL("url_image_title", json);
				String url_image_marquee = getImageURL("url_image_marquee", json);
				String url_image_cabinet = getImageURL("url_image_cabinet", json);
				String url_image_flyer = getImageURL("url_image_flyer", json);
				String url_icon = getImageURL("url_icon", json);

				if (url_image_ingame != null && needsDownload(props, PROPERTIE_INGAME))
					props.put(PROPERTIE_INGAME, download(rom_name, FILE_TYPE_PNG, url_image_ingame, TIMEOUT, DIR_INGAME) + "");
				if (url_icon != null && needsDownload(props, PROPERTIE_ICON))
					props.put(PROPERTIE_ICON, download(rom_name, FILE_TYPE_ICO, url_icon, TIMEOUT, DIR_ICON) + "");
				if (url_image_title != null && needsDownload(props, PROPERTIE_TITLE))
					props.put(PROPERTIE_TITLE, download(rom_name, FILE_TYPE_PNG, url_image_title, TIMEOUT, DIR_TITLE) + "");
				if (url_image_marquee != null && needsDownload(props, PROPERTIE_MARQUEE))
					props.put(PROPERTIE_MARQUEE, download(rom_name, FILE_TYPE_PNG, url_image_marquee, TIMEOUT, DIR_MARQUEE) + "");
				if (url_image_cabinet != null && needsDownload(props, PROPERTIE_CABINET))
					props.put(PROPERTIE_CABINET, download(rom_name, FILE_TYPE_PNG, url_image_cabinet, TIMEOUT, DIR_CABINET) + "");
				if (url_image_flyer != null && needsDownload(props, PROPERTIE_FLYER))
					props.put(PROPERTIE_FLYER, download(rom_name, FILE_TYPE_PNG, url_image_flyer, TIMEOUT, DIR_FLYER) + "");
			} else {
				Log.d(TAG, "Not data: " + rom_name);
			}

			FileOutputStream out = new FileOutputStream(propsFile);
			props.store(out, null);
			out.close();

		} catch (IOException ex) {
			Log.e(TAG,ex.toString());
		}

		return true;
	}

	@Override
	public boolean reset() {
		Log.d(TAG, "Deleting properties...");
		String propsDir = scrapeDir + DIR_PROPERTIES;
		File f = new File(propsDir);
		File[] contents = f.listFiles();
		for (File f2 : contents) {
			f2.delete();
		}
		return true;
	}

	protected String getImageURL(String imageType, String json) {
		String url = null;
		String token = "\"" + imageType + "\":";
		int i = json.indexOf(token);
		if (i == -1) return url;
		i += token.length();
		i = json.indexOf("\"", i);
		int j = json.indexOf("\"", i + 1);
		if (j == -1) return url;
		url = json.substring(i + 1, j);
		if(url.trim().length()==0)
			return null;
		url = url.replace("\\", "");
		if(mm.getPrefsHelper().isScrapingResize()) {
			url = url.replace("resize=0", "resize=300");
		}
		return url;
	}

	protected boolean isEmpty(String json) {
		String data = null;
		String token = "\"result\":";
		int i = json.indexOf(token);
		if (i == -1) return false;
		i += token.length();
		i = json.indexOf("[", i);
		int j = json.indexOf("]", i + 1);
		if (j == -1) return false;
		data = json.substring(i + 1, j);
		return data.isEmpty();
	}

	protected String getJSON(String url, int timeout) {
		HttpURLConnection c = null;
		try {
			URL u = new URL(url);
			c = (HttpURLConnection) u.openConnection();
			c.setRequestProperty("User-Agent", USER_AGENT);
			c.setRequestMethod("GET");
			c.setRequestProperty("Content-length", "0");
			c.setUseCaches(false);
			c.setAllowUserInteraction(false);
			c.setConnectTimeout(timeout);
			c.setReadTimeout(timeout);
			c.connect();
			int status = c.getResponseCode();

			switch (status) {
				//if error with ADB HTTP/1.0 503 Service Unavailable message
				case 200:
				case 201:
					BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = br.readLine()) != null) {
						sb.append(line + "\n");
					}
					br.close();
					return sb.toString();
			}
		} catch (IOException ex) {
			Log.e(TAG,ex.toString());
		} finally {
			if (c != null) {
				try {
					c.disconnect();
				} catch (Exception ignored) {}
			}
		}
		return null;
	}

	protected boolean download(String name, String fileType, String url, int timeout, String folder) {
		String dir = scrapeDir + folder;
		String fileName = name + "." + fileType;
		File file = new File(dir + File.separator + fileName);
		if(file.exists()){
			Log.d(TAG, "Already exists: " + fileName);
			return true;
		}
		Log.d(TAG, "Downloading: " + url);
		HttpURLConnection c = null;
		try {
			URL u = new URL(url);
			c = (HttpURLConnection) u.openConnection();
			c.setRequestProperty("User-Agent", USER_AGENT);
			c.setRequestMethod("GET");
			c.setRequestProperty("Content-length", "0");
			c.setUseCaches(false);
			c.setAllowUserInteraction(false);
			c.setConnectTimeout(timeout);
			c.setReadTimeout(timeout);
			c.connect();
			int status = c.getResponseCode();

			switch (status) {
				case 200:
				case 201:
                   /*
                   	String raw = c.getHeaderField("Content-Disposition");
                    if(raw != null && raw.indexOf("=") != -1) {
                        fileName = raw.split("=")[1]; //getting value after '='
                        if(fileName.startsWith("\"") && fileName.endsWith("\""))
                            fileName = fileName.substring(1,fileName.length()-1);
                    }
                    if(fileName==null)
                    */

					InputStream input = c.getInputStream();
					byte[] buffer = new byte[4096];
					int n;
					Files.createDirectories(Paths.get(dir));
					OutputStream output = new FileOutputStream(file);
					while ((n = input.read(buffer)) != -1) {
						output.write(buffer, 0, n);
					}
					output.close();
					return true;
			}

		} catch (IOException ex) {
			Log.e(TAG,ex.toString());
		} finally {
			if (c != null) {
				try {
					c.disconnect();
				} catch (Exception ignored) {}
			}
		}
		return false;
	}
}
