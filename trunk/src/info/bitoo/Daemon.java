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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

public class Daemon implements BiTooListener {

	private final static Logger logger = Logger.getLogger(Daemon.class
			.getName());

	private Properties props;

	private BufferedReader input;

	private Map biToos;

	/**
	 * @param props2
	 * @param input
	 */
	public Daemon(Properties props, BufferedReader input) {
		this.props = props;
		this.input = input;
	}

	private static Options createCommandLineOptions() {
		Option optConfig = new Option("c", "config", true,
				"configuration file location");

		Option optPipe = new Option("p", "pipe", true, "unix pipe path");
		optPipe.setRequired(true);

		Options cliOptions = new Options();
		cliOptions.addOption(optConfig);
		cliOptions.addOption(optPipe);

		return cliOptions;
	}

	public static void main(String[] args) throws IOException {
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(createCommandLineOptions(), args);
		} catch (ParseException e) {
			System.err.println("Parsing failed.  Reason: " + e.getMessage());
		}

		Properties props = Main.readConfiguration(cmd);

		BufferedReader input = new BufferedReader(new FileReader(cmd
				.getOptionValue("p")));

		Daemon daemon = new Daemon(props, input);
		daemon.deamonMain();
	}

	public void deamonMain() {

		biToos = new HashMap();

		String line = null;
		String filename = null;
		while (true) {
			try {
				line = input.readLine();
				filename = input.readLine();
			} catch (IOException e) {
				logger.warn("Error reading a line from input. Continue", e);
				line = null;
				filename = null;
			}
			if (line != null) {
				//new request
				logger.debug("New line from pipe: [" + line + "]");
				logger.info("New fetch request for file: [" + filename + "]");

				//first line is output pipe path
				PrintWriter output = null;
				try {
					output = new PrintWriter(new FileWriter(line));
				} catch (IOException e2) {
					logger.error(
							"Error opening comunication channel for request: ["
									+ filename + "]. Abort this request", e2);
					continue;
				}
				output.println("BEGIN");
				output.flush();

				//Create new BiToo

				BiToo biToo = null;
				try {
					biToo = new BiToo(props);

					biToo.setTorrent(filename);
				} catch (UnknownHostException e3) {
					logger.error("Error creating BiToo worker for request: ["
							+ filename + "]. Abort this request", e3);
					output.println("KO");
					output.flush();
					output.close();
					continue;
				} catch (MalformedURLException e3) {
					logger.error("Error creating BiToo worker for request: ["
							+ filename + "]. Abort this request", e3);
					output.println("KO");
					output.flush();
					output.close();
					continue;
				}
				biToo.setListener(this);
				biToos.put(biToo, output);
				Thread main = new Thread(biToo);
				main.setName("BiToo");
				main.start();
			}
			try {
				Thread.sleep(1 * 1000);
			} catch (InterruptedException e1) {
				logger.warn("Deamon interrupted while sleeping", e1);
			}
		}

	}

	public void downloadCompleted(BiToo biToo) {
		logger.debug("download completed event");
		PrintWriter output = (PrintWriter) biToos.remove(biToo);
		if (output != null) {
			output.println("OK");
			output.flush();
			output.close();
		} else {
			logger.warn("No output for BiToo worker: [" + biToo.toString()
					+ "]");
		}
		biToo.destroy();
	}

	public void downloadFailed(BiToo biToo) {
		logger.debug("download failed event");
		PrintWriter output = (PrintWriter) biToos.remove(biToo);
		if (output != null) {
			output.println("KO");
			output.flush();
			output.close();
		} else {
			logger.warn("No output for BiToo worker: [" + biToo.toString()
					+ "]");
		}
		biToo.destroy();
	}
}