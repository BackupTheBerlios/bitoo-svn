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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;

public class TrackerProxy extends Thread {
	private final static Logger logger = Logger.getLogger(TrackerProxy.class.getName());
	
	private String peerId;
	private InetAddress localPeerAddress;
	private int localPeerPort;
	private InetAddress remoteTrackerAddress;
	private int remoteTrackerPort;
	private int localTrackerPort;
	private String remoteTrackerBaseURL;
	
	private ServerSocket proxyServer;
	
	private Map localPeerMap;
	private byte[] localPeerBytes;
	
	
	public TrackerProxy(String remoteTrackerBaseURL, String peerId, InetAddress localPeerAddress, int localPeerPort) {
		super("TrackerProxy");
	    this.remoteTrackerBaseURL = remoteTrackerBaseURL;
		this.peerId = peerId;
		this.localPeerAddress = localPeerAddress;
		this.localPeerPort = localPeerPort;
		
		localPeerMap = new HashMap();
		
		localPeerMap.put("peer id", this.peerId);
		localPeerMap.put("ip", this.localPeerAddress.getHostAddress());
		localPeerMap.put("port", new Integer(localPeerPort));
		
		localPeerBytes = new byte[6];
		System.arraycopy(this.localPeerAddress.getAddress(), 0, localPeerBytes, 0, 4);
		
		byte portHigh = (byte)(localPeerPort / 256);
		byte portLow =(byte) (localPeerPort % 256);
		
		localPeerBytes[4] = portHigh;
		localPeerBytes[5] = portLow;

		
		try {
			proxyServer = new ServerSocket(0);
			localTrackerPort = proxyServer.getLocalPort();

		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}

	}
	
	public int getLocalTrackerPort() {
		return localTrackerPort;
	}
	
	public void run() {


		
		try {
			
			while(true) {
			Socket inSocket = proxyServer.accept();
			BufferedReader d = new BufferedReader(new InputStreamReader(inSocket.getInputStream()));
			String requestLine = d.readLine();
			logger.debug("New incoming request: [" + requestLine + "]");
			if(! requestLine.startsWith("GET")) {
				throw new IOException("Bad HTTP Method");
			}
			
			
			String tmp = requestLine.substring("GET ".length()).trim();
			String request = tmp.substring(0, tmp.indexOf(' '));
			
			boolean scrape = request.startsWith("/scrape");
			
			tmp = remoteTrackerBaseURL + request;
			logger.debug("Rewrite request is: [" + tmp + "]");
			
			URL trackerRequestURL = new URL(tmp);
			HttpURLConnection conn = (HttpURLConnection) trackerRequestURL.openConnection();
			conn.connect();
			/*
			String decodedURL = URLDecoder.decode(tmp);
			int idxInfoHash = decodedURL.indexOf("info_hash=");
			int idxBegin = idxInfoHash + "info_hash=".length();
			String infoHash = decodedURL.substring(idxBegin, idxBegin + 20);
			
			logger.debug("Info hash bytes: [" + infoHash + "]");
			*/
			BufferedInputStream inReply = new BufferedInputStream(conn.getInputStream());
			StringBuffer sb = new StringBuffer();
			
			byte[] allBuf = new byte[0];
			byte[] buf = new byte[1024];
			int numRead = -1;
			while((numRead = inReply.read(buf)) != -1) {
				byte[] newBuf = new byte[allBuf.length + numRead];
				System.arraycopy(allBuf, 0, newBuf, 0, allBuf.length);
				System.arraycopy(buf, 0, newBuf, allBuf.length, numRead);
				allBuf = newBuf;
			}
			
			logger.debug("Real tracker response bytes: [" + new String(allBuf) +"]");
			
			Map replyMap = BDecoder.decode(allBuf);
			
			if(! scrape) {
				Object peersObject = replyMap.get("peers");
				if(peersObject == null || peersObject instanceof List) {
					List peers = (List)peersObject;
					if(peers == null) {
						peers = new ArrayList(1);
					}
					peers.add(0, localPeerMap);
				}
				else {
					replyMap.put("peers", localPeerBytes);
				}
			}
			byte[] forwardReplyBytes = BEncoder.encode(replyMap);
			
			OutputStream outReply = inSocket.getOutputStream();
			
			outReply.write(forwardReplyBytes);
			outReply.flush();
			
			inSocket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
