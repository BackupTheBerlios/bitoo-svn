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

package info.bitoo.clientadapters;

import info.bitoo.FetchTorrent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

public class BitTorrent implements ClientAdapter {
	private final static Logger logger = Logger.getLogger(BitTorrent.class
			.getName());	
	
	private String command;
	
	private File saveDir;
	
	private int port;
	
	private File logFile;

	public void init(Properties props) throws ClientAdapterException {
		command = props.getProperty("bitTorrentClientCommand");
		logger.info("BitTorrent client command is: [" + command + "]");
		if(command == null) {
			ClientAdapterException.generate(logger, "BitTorrent client command not specified");
		}
		
		String strSaveDir = props.getProperty("bitTorrentClientSaveDir");
		logger.info("BitTorrent client save dir is: [" + strSaveDir + "]");
		saveDir = new File(props.getProperty("bitTorrentClientSaveDir"));
		
		try {
		port = Integer.parseInt(props.getProperty("bitTorrentClientPort"));
		} catch(NumberFormatException nfe) {
			ClientAdapterException.generate(logger, "Bad BitTorrent client port: [" + props.getProperty("bitTorrentClientPort") + "]");
		}
		
		logFile = new File(props.getProperty("bitTorrentClientLogFile", "bitTorrentClient.log"));
		
	}

	public boolean launch(File torrent) throws ClientAdapterException {
		URL torrentURL = null;
		Map torrentMap = null;
		try {
			torrentURL = torrent.toURL();
			FetchTorrent fetchTorrent = new FetchTorrent(torrentURL);
			
			torrentMap = fetchTorrent.getTorrent();
		} catch (IOException e) {
			ClientAdapterException.generate(logger, "Unable to decode torrent: [" + torrent.toString() + "]");
		}
		
		
		Map infoMap = (Map)torrentMap.get("info");
		String filename = new String((byte[])infoMap.get("name"));
		
		String saveAs = saveDir + "/" + filename;
		
		command = command.replaceAll("\\$n", saveAs);
		command = command.replaceAll("\\$t", torrentURL.toString());

		command += " --minport " + port + " --maxport " + port + " --display_interval 5";
		
		logger.info("BitTorrent command line is: [" + command + "]");
		
	//	FileOutputStream outputStream = new FileOutputStream(logFile);
		
		Process process =null;
		try {
			process = Runtime.getRuntime().exec(command);
		} catch (IOException e1) {
			ClientAdapterException.generate(logger, "Launch of BitTorrent client failed", e1);
		}
		
		final InputStream stdOut = process.getInputStream();
		
		Thread clientOutput = new Thread()  {
			
			public void run() {
				int c = -1;
				try {
					while((c = stdOut.read()) != -1) {
						System.out.write(c);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		
		clientOutput.start();
		
		final InputStream stdErr = process.getInputStream();
		
		Thread clientError = new Thread()  {
			
			public void run() {
				int c = -1;
				try {
					while((c = stdErr.read()) != -1) {
						System.out.write(c);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		
		clientError.start();
		
		return true;
	}

	public InetAddress getAddress() {
		try {
			return InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			logger.error("getAddress error", e);
		}
		return null;
	}

	public int getPort() {
		return port;
	}


}
