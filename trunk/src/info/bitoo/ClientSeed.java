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
 */package info.bitoo;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

import org.apache.log4j.Logger;

public class ClientSeed {
	private final static Logger logger = Logger.getLogger(ClientSeed.class
			.getName());

	private InetAddress realBitTorrentAddress;

	private int realBitTorrentPort;

	private byte[] torrentHashBytes;

	private String peerId;

	private int numberOfPieces;

	private int pieceLength;

	private List downloaders;

	private Socket outSocket;

	private OutputStream os;

	private InputStream is;
	
	private PeerHandler peerHandler;

	public ClientSeed(InetAddress realBitTorrentAddress,
			int realBitTorrentPort, byte[] torrentHashBytes, String peerId,
			int numberOfPieces, int pieceLength, List downloaders) {
		this.realBitTorrentAddress = realBitTorrentAddress;
		this.realBitTorrentPort = realBitTorrentPort;
		this.torrentHashBytes = torrentHashBytes;
		this.peerId = peerId;
		this.numberOfPieces = numberOfPieces;
		this.pieceLength = pieceLength;
		this.downloaders = downloaders;
	}

	public OutputStream getOutputStream() {
		return os;
	}
	
	public InputStream getInputStream() {
		return is;
	}

	
	public boolean connect(int retries, int secondsDelay) throws InterruptedException {
		if (retries <= 0 || secondsDelay <= 0) {
			return false;
		}

		for (int i = 0; i < retries; i++) {
			try {
				outSocket = new Socket(realBitTorrentAddress,
						realBitTorrentPort);
				os = outSocket.getOutputStream();
				is = outSocket.getInputStream();

				peerHandler = new PeerHandler(is, os, numberOfPieces,
						pieceLength, downloaders);

				return true;
			} catch (IOException e) {
				logger.info("Connection with client terminated. ["
						+ e.getMessage() + "]");
				if (outSocket != null) {
					try {
						outSocket.close();
						outSocket = null;
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
			Thread.sleep(secondsDelay * 1000);
		}
		return false;

	}

	public PeerHandler getPeerHandler() {
		return peerHandler;
	}
	
	private void doHandShake() throws IOException {
		byte[] zero = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };
		byte i19 = 19;
		String msg1 = "BitTorrent protocol";

		os.write(i19);
		os.write(msg1.getBytes());
		os.write(zero);
		os.write(torrentHashBytes);
		os.write(peerId.getBytes());
		os.flush();
	}

	private void doBitField() throws IOException {

		int neededBytes = 1 + numberOfPieces / 8;
		int reminders = neededBytes * 8 - numberOfPieces;

		byte[] bitField = new byte[neededBytes];
		for (int i = 0; i < neededBytes - 1; i++) {
			bitField[i] = (byte) 255;
		}

		byte last = (byte) 255;

		bitField[neededBytes - 1] = (byte) (last << reminders);
		byte[] length = intToBytes(1 + neededBytes);
		os.write(length);
		os.write(5);
		os.write(bitField);
		os.flush();
	}

	private void doUnChoke() throws IOException {
		os.write(intToBytes(1));
		os.write(1);
		os.flush();
	}

	public void startSeed() throws IOException {
		doHandShake();
		doBitField();
		doUnChoke();
	}

	static byte[] intToBytes(int i) {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			dos.writeInt(i);
			dos.flush();
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return baos.toByteArray();
	}

	public void disconnect() {
		if (outSocket != null) {
			try {
				outSocket.close();
				outSocket = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}