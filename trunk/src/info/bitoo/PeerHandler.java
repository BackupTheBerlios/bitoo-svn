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

import info.bitoo.downloaders.AlternativeDownloader;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

public class PeerHandler extends Thread {
	private final static Logger logger = Logger.getLogger(PeerHandler.class
			.getName());

	private InputStream input;

	private OutputStream output;

	private int numberOfPieces;

	private int pieceLength;

	private List downloaders;
	
	private boolean completed = false;
	
	private boolean[] remoteBitField;
	
	public PeerHandler(InputStream is, OutputStream os, int numberOfPieces,
			int pieceLength, List downloaders) {
		super("PeerHandler");
		input = is;
		output = os;
		this.numberOfPieces = numberOfPieces;
		this.pieceLength = pieceLength;

		this.downloaders = downloaders;
		
		remoteBitField = new boolean[numberOfPieces];
		Arrays.fill(remoteBitField, false);
	}

	public boolean isCompleted() {
		return completed;
	}
	
	public void run() {

		try {
			readHandshake();

			Message msg = nextMessage();
			
			if(msg == null) {
				logger.info("Connection with client terminated.");
				return;
			}
			
			if(msg.getType() == Message.BIT_FIELD) {
				readBitField(msg);
				completed = checkIfCompleted();
				if(completed) {
					logger.info("Remote peer completed the download. Stop reading messages");
					return;
				}

			}
			
			while (true) {
				if (msg.getType() == Message.REQUEST) {
					readRequest(msg.getPayload());
				} else if(msg.getType() == Message.HAVE) {
					readHave(msg.getPayload());
					completed = checkIfCompleted();
					if(completed) {
						logger.info("Remote peer completed the download. Stop reading messages");
						notifyAll();
						return;
					}
				}
				msg = nextMessage();
				if(msg == null) {
					logger.info("Connection with client terminated.");
					return;
				}
			}
		} catch (IOException e) {
			logger.info("Connection with client terminated. [" + e.getMessage()
					+ "]");
		}
	}

	private boolean checkIfCompleted() {
		for(int i = 0; i < remoteBitField.length; i++) {
			if(! remoteBitField[i]) {
				return false;
			}
		}
		

		return true;
	}
	
	private void readBitField(Message msg) {
		byte[] bitField = msg.getPayload();
		
		int neededBytes = 1 + numberOfPieces / 8;
		int reminders = neededBytes * 8 - numberOfPieces;
		
		for(int i = 0; i < neededBytes - 1; i++) {
			int b = bitField[i];
			int c = 0;
			for(int m = 128; m >= 1; m /= 2) {
				int and =  (b & m);
				int idx = i * 8 + c;
				remoteBitField[idx] = (b & m) > 0; 
				c++;
			}
		}
		
		int b = bitField[neededBytes -1];
		int c = 0;
		int exp = (int)Math.pow(2, reminders );
		for(int m = 128; m >= exp ; m /= 2) {
			remoteBitField[(neededBytes - 1) * 8 + c] = (b & m) > 0; 
			c++;
		}
		
	}

	private void readHave(byte[] payload) throws IOException {
		int index = bytesToInt(payload);
		
		remoteBitField[index] = true;
	}
	
	private void readRequest(byte[] payload) throws IOException {
		ByteArrayInputStream input = new ByteArrayInputStream(payload);
		byte[] intBytes = new byte[4];
		input.read(intBytes);

		int index = bytesToInt(intBytes);

		input.read(intBytes);

		int begin = bytesToInt(intBytes);

		input.read(intBytes);

		int length = bytesToInt(intBytes);

		logger.debug("Request message data are: [" + index + "] / [" + begin
				+ "] / [" + length + "]");

		int start = index * pieceLength + begin;

		int end = start + length - 1;

		logger.debug("Bytes range is: [" + start + " --> " + end + "]");

		try {
		downloadPiece(index, length, begin, start, end);
		} catch(IOException ioe) {
			logger.error("Download failed, continue", ioe);
		}
	}

	private void downloadPiece(int index, int length, int begin, int start,
			int end) throws IOException {

		Collections.shuffle(downloaders);

		AlternativeDownloader currentDownloader = (AlternativeDownloader) downloaders
				.get(0);
		logger.info("Using downloader Location: ["
				+ currentDownloader.toString() + "]");

		try {
		currentDownloader.start(start, end);
		} catch(IOException ioe) {
			logger.error("Downloader fail, removing it", ioe);
			downloaders.remove(currentDownloader);
			throw new IOException("Download failed");
		}
		output.write(ClientSeed.intToBytes(9 + length));
		output.write(7);
		output.write(ClientSeed.intToBytes(index));
		output.write(ClientSeed.intToBytes(begin));
		output.flush();

		int c = -1;
		while ((c = currentDownloader.read()) != -1) {
			output.write(c);
		}

		output.flush();

		currentDownloader.stop();
	}

	private void readHandshake() throws IOException {
		byte[] handshakeBytes = new byte[1 + 19 + 8 + 20 + 20];

		input.read(handshakeBytes);

		logger.debug("Got Handshake message: [" + new String(handshakeBytes)
				+ "]");
	}
/*
	private byte[] readBitField() {
		
	}
	*/
	private int bytesToInt(byte[] intBytes) throws IOException {
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(
				intBytes));

		return dis.readInt();
	}

	public Message nextMessage() throws IOException {
		
		byte[] intBytes = new byte[4];
		if (input.read(intBytes) == -1) {
			logger.info("Peer close the connection");
			return null;
		}

		
		int length = bytesToInt(intBytes);
		logger.debug("Length prefix is: [" + length + "]");

		//keep-alive
		int messageType = -1;
		if (length == 0) { 
			return new Message(Message.KEEP_ALIVE);
		}

		messageType = input.read();
		if (messageType == -1) {
			logger.info("Peer close the connection");
			return null;				
		}

		logger.debug("Message type is: [" + messageType + "]");

		byte[] payload = new byte[length - 1];
		if (input.read(payload) == -1) {
			logger.info("Peer close the connection");
			return null;
		}
		logger.debug("Payload of message is: ["
				+ new String(payload) + "]");
		return new Message(messageType, payload);
		
		}
		
	}
	
	
	class Message {
		static final int KEEP_ALIVE = 0;
		static final int REQUEST = 6;
		static final int BIT_FIELD = 5;
		static final int HAVE = 4;
		
		int length;
		int type;
		byte[] payload;
		
		Message(int type) {
			this(type, null);
		};
		
		Message(int type, byte[] payload) {
			this.type = type;
			this.payload = payload;
		}
		
		int getType() {
			return type;
		}
		
		byte[] getPayload() {
			return payload;
		}
	}
