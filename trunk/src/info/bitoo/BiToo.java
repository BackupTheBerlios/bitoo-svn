/*
 * Copyright (c) 2004 Fabio Di Fabio
 * All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc., 675
 * Mass Ave, Cambridge, MA 02139, USA.
 */
package info.bitoo;

import info.bitoo.clientadapters.ClientAdapter;
import info.bitoo.clientadapters.ClientAdapterException;
import info.bitoo.downloaders.AlternativeDownloader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.gudy.azureus2.core3.util.BEncoder;

public class BiToo implements Runnable {
	private final static Logger logger = Logger
			.getLogger(BiToo.class.getName());

	private String remoteTorrentBaseURL;

	private String peerId;

	private InetAddress localPeerAddress;

	private int localPeerPort;

	private Map protocolDownloadersMap;

	private ClientAdapter clientAdapter;

	private String localTrackerBaseURL;

	private URL torrentURL;
	
	private Properties props;
	
	private boolean completed = false;
	
	public BiToo() {

	}


	/**
	 * @param props
	 * @throws UnknownHostException
	 */
	public BiToo(Properties props) throws UnknownHostException {
		init(props);
	}


	/**
	 * @param props
	 * @throws UnknownHostException
	 */
	public void init(Properties props) throws UnknownHostException {
		readConfig(props);
	}


	private List getAlternativeDownloaders(List locations, Properties props) {
		if (locations == null) {
			return null;
		}

		List downloaders = new ArrayList();

		Iterator iLocations = locations.iterator();
		while (iLocations.hasNext()) {
			String location = new String( (byte[])iLocations.next());
			if (location != null) {
				int idxColon = location.indexOf(':');
				String protocol = location.substring(0, idxColon)
						.toLowerCase();
				String downloaderClassName = (String) protocolDownloadersMap
						.get(protocol);
				if (downloaderClassName == null) {
					logger.warn("No downloader found for protocol: ["
							+ protocol + "], this URL is disabled: ["
							+ location + "]");
				} else {
					String errMsg = "Cannot create a instance for downloader class: ["
							+ downloaderClassName + "] and location: [" + location + "]";
					try {
						Class downloaderClass = Class
								.forName(downloaderClassName);
						Constructor downloaderConstructor = downloaderClass
								.getConstructor(new Class[] { URL.class });
						AlternativeDownloader downloader = (AlternativeDownloader) downloaderConstructor
								.newInstance(new Object[] { new URL(location) });

						downloader.init(props);
						downloaders.add(downloader);
					} catch (ClassNotFoundException e) {
						logger.error(errMsg, e);
					} catch (IllegalArgumentException e) {
						logger.error(errMsg, e);
					} catch (InstantiationException e) {
						logger.error(errMsg, e);
					} catch (IllegalAccessException e) {
						logger.error(errMsg, e);
					} catch (InvocationTargetException e) {
						logger.error(errMsg, e);
					} catch (SecurityException e) {
						logger.error(errMsg, e);
					} catch (NoSuchMethodException e) {
						logger.error(errMsg, e);
					} catch (MalformedURLException e) {
						logger.error(errMsg, e);
					}
				}
			}
		}
		return downloaders;
	}

	private Properties readConfig(Properties props) throws UnknownHostException {
		this.props = props;
		logger.info("BEGIN CONFIG");
		
		remoteTorrentBaseURL = props.getProperty("remoteTorrentBaseURL");
		logger.info("remoteTorrentBaseURL: [" + remoteTorrentBaseURL + "]");

		localTrackerBaseURL = props.getProperty("localTrackerBaseURL");
		logger.info("localTrackerBaseURL: [" + localTrackerBaseURL + "]");
		
		String peerIdBase = props.getProperty("peerIdBase",	"-0000178901234567890");
		long time = System.currentTimeMillis();
		String strTime = String.valueOf(time);
		int idx = strTime.length();
		peerId = peerIdBase.substring(0, 20 - idx) + strTime;
		logger.info("peerIdBase: [" + peerIdBase + "]");
		logger.info("peerId: [" + peerId + "]");

		try {
			localPeerAddress = InetAddress.getByName(props.getProperty(
					"localPeerAddress", "localhost"));
		} catch (UnknownHostException e1) {
			logger.fatal("Bad local peer address: ["
					+ props.getProperty("localPeerAddress", "localhost"));
			throw e1;
		}
		logger.info("localPeerAddress: [" + localPeerAddress.toString() + "]");
		

		try {
			localPeerPort = Integer.parseInt(props.getProperty("localPeerPort",
					"6881"));
		} catch (NumberFormatException nfe) {
			logger.fatal("Bad local peer port: ["
					+ props.getProperty("localPeerPort", "6881") + "]");
			throw nfe;
		}
		logger.info("localPeerPort: [" + localPeerPort + "]");
		
		
		String propProtocolDownloader = props.getProperty(
				"protocolDownloadersMap",
				"http=info.bitoo.HttpAlternativeDownloader");
		StringTokenizer stMap = new StringTokenizer(propProtocolDownloader, ",");
		protocolDownloadersMap = new HashMap();
		while (stMap.hasMoreTokens()) {
			String pair = stMap.nextToken();
			StringTokenizer stPair = new StringTokenizer(pair, "=");

			String protocol = stPair.nextToken().toLowerCase();
			String downloader = stPair.nextToken();
			logger.info("Downloader for protocol: [" + protocol + "] is: ["
					+ downloader + "]");
			protocolDownloadersMap.put(protocol, downloader);
		}

		String clientAdapterClassName = props.getProperty("realClientAdapter",
				"info.bitoo.clientadapters.AzureusWebUI");
		logger.info("realClientAdapter: [" + clientAdapterClassName + "]");
		
		clientAdapter = initClientAdapter(clientAdapterClassName, props);
		
		logger.info("END CONFIG");
		return props;
	}

	private ClientAdapter initClientAdapter(
			String clientAdapterClassName, Properties props) {
		
		String errMsg = "Cannot create a instance for client adapter class: ["
				+ clientAdapterClassName + "]";
		ClientAdapter adapter = null;

		try {
			Class clientAdapterClass = Class.forName(clientAdapterClassName);
			adapter = (ClientAdapter)clientAdapterClass.newInstance();
			try {
				adapter.init(props);
			} catch (ClientAdapterException e1) {
				logger.error("Fail to initialize client adapter: [" + clientAdapterClassName + "]", e1);
			}
		} catch (ClassNotFoundException e) {
			logger.error(errMsg, e);
		} catch (IllegalArgumentException e) {
			logger.error(errMsg, e);
		} catch (InstantiationException e) {
			logger.error(errMsg, e);
		} catch (IllegalAccessException e) {
			logger.error(errMsg, e);
		} catch (SecurityException e) {
			logger.error(errMsg, e);
		}
		return adapter;
	}


	/**
	 * @param torrentMap
	 * @param torrentURL
	 * @throws IOException
	 */
	private File updateTorrent(Map torrentMap, URL torrentURL, int localPeerPort) throws IOException {
		String torrentURLPath = torrentURL.getPath();
		int idx = torrentURLPath.lastIndexOf('/');
		String torrentName = torrentURLPath.substring(idx + 1);
		
		String proxyAnnounce = localTrackerBaseURL.replaceAll("\\$p", String.valueOf(localPeerPort));// + torrentName;
		torrentMap.put("announce", proxyAnnounce);
		
		byte[] torrentBytes = BEncoder.encode(torrentMap);
		
		File tmpTorrentFile = File.createTempFile("bitoo", ".torrent");
		tmpTorrentFile.deleteOnExit();
		OutputStream os = new BufferedOutputStream(new FileOutputStream(tmpTorrentFile));
		
		os.write(torrentBytes);
		os.flush();
		os.close();
		
		return tmpTorrentFile;
	}


	/**
	 * @param torrentName
	 * @throws MalformedURLException
	 */
	public void setTorrent(String torrentName) throws MalformedURLException {
		torrentURL = new URL(remoteTorrentBaseURL + torrentName);
	}


	/**
	 * @param torrentURL
	 */
	public void setTorrent(URL torrentURL) {
		this.torrentURL = torrentURL;
	}


	public void run() {
		if(props == null) {
			logger.fatal("BiToo not inited. Exit");
			return;
		}
		
		if(torrentURL == null) {
			logger.fatal("No torrent URL set, nothing to do. Exit");
			return;
		}
		
		
		
		FetchTorrent torrentFetcher = new FetchTorrent(torrentURL);

		Map torrentMap = null;
		try {
			torrentMap = torrentFetcher.getTorrent();
		} catch (IOException e) {
			logger.fatal("Unable to fetch the torrent. Exit", e);
			return;
		}
		
		List locationsList = (List) torrentMap.get("alternative locations");
		
		List alternativeDownloaders = getAlternativeDownloaders(locationsList,
				props);

		Map torrentInfoMap = (Map) torrentMap.get("info");

		byte[] torrentInfoPieces = (byte[]) torrentInfoMap.get("pieces");

		int numberOfPieces = torrentInfoPieces.length / 20;

		int pieceLength = ((Long) torrentInfoMap.get("piece length"))
				.intValue();

		logger.debug("Number of pieces: " + torrentInfoPieces.length
				+ " / 20 = " + numberOfPieces);
		logger.debug("Piece length: " + pieceLength);

		byte[] torrentInfoBytes = null;
		try {
			torrentInfoBytes = BEncoder.encode(torrentInfoMap);
		} catch (IOException e1) {
			logger.fatal("Unable to BEncode modified torrent. Exit", e1);
			return;
		}
		
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e2) {
			logger.fatal("Unable to calculate SHA1 of the torrent's info. Exit", e2);
			return;
		}
		byte[] torrentHashBytes = md.digest(torrentInfoBytes);

		logger.debug("Calculated info hash bytes: ["
				+ new String(torrentHashBytes) + "]");

		String trackerAnnounceURL = new String((byte[]) torrentMap
				.get("announce"));
		int idx = trackerAnnounceURL.lastIndexOf("/");

		String remoteTrackerBaseURL = trackerAnnounceURL.substring(0, idx);

		TrackerProxy tp = new TrackerProxy(remoteTrackerBaseURL, peerId,
				localPeerAddress, localPeerPort);

		tp.start();

		File updatedTorrentFile = null;
		try {
			updatedTorrentFile = updateTorrent(torrentMap, torrentURL, tp.getLocalTrackerPort());
		} catch (IOException e3) {
			logger.fatal("Unable to update torrent. Exit", e3);
			return;
		}

		if (alternativeDownloaders != null && alternativeDownloaders.size() > 0) {
			try {
				boolean launched = clientAdapter.launch(updatedTorrentFile);
			} catch (ClientAdapterException e4) {
				logger.fatal("Unable to launch real BitTorrent client", e4);
				return;
			}


			ClientSeed seed = new ClientSeed(clientAdapter.getAddress(),
					clientAdapter.getPort(), torrentHashBytes, peerId,
					numberOfPieces, pieceLength, alternativeDownloaders);

			completed = false;

			int retries = 3;
			
			while(! completed) {
				boolean isConnected = false;

				try {
					isConnected = seed.connect(2, 5);
				} catch (InterruptedException e6) {
					logger.warn("Connection process interrupted", e6);
				}

				if(isConnected) {
					try {
						PeerHandler peerHandler = seed.getPeerHandler();
						peerHandler.start();
						
						seed.startSeed();

						try {
							peerHandler.join();
						} catch (InterruptedException e7) {
							logger.error("Error while waiting for peer handler thread", e7);
						}
						
						completed = peerHandler.isCompleted();
					} catch (IOException e5) {
						logger.error("Error when seeding, disconnecting", e5);
						seed.disconnect();
						isConnected = false;
					}
				}
				if(retries-- > 0) {
					try {
						Thread.sleep(5 * 1000);
					} catch (InterruptedException e5) {
						logger.error("BiToo main process sleep interrupt. Exit", e5);
						return;
					}
				} else {
					//exit anyway
					break;
				}
			}
			logger.info((completed) ? "Seed completed" : "Seed failed");
		}
		
	}	
	
	public boolean isCompleted() {
		return completed;
	}
}