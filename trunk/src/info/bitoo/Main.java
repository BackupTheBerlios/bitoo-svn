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

import info.bitoo.clientadapters.ClientAdapterException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public final class Main {
	private final static Logger logger = Logger.getLogger(Main.class.getName());

	public static final String defaultConfigFilename = "bitoo.properties";

	public static void main(String[] args) throws InterruptedException,
			IOException, NoSuchAlgorithmException, ClientAdapterException {
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(createCommandLineOptions(), args);
		} catch (ParseException e) {
			System.err.println("Parsing failed.  Reason: " + e.getMessage());
		}

		Properties props = readConfiguration(cmd);
		BiToo biToo = new BiToo(props);

		URL torrentURL = null;

		if (cmd.hasOption("f")) {
			String parmValue = cmd.getOptionValue("f");
			String torrentName = parmValue + ".torrent";
			biToo.setTorrent(torrentName);
		} else if (cmd.hasOption("t")) {
			torrentURL = new URL(cmd.getOptionValue("t"));
			biToo.setTorrent(torrentURL);
		} else {
			return;
		}

		try {
			Thread main = new Thread(biToo);
			main.setName("BiToo");
			main.start();

			//wait until thread complete
			main.join();
		} finally {
			biToo.destroy();
		}

		if (biToo.isCompleted()) {
			System.out.println("Download completed");
			System.exit(0);
		} else {
			System.out.println("Download failed");
			System.exit(1);
		}

	}

	static Properties readConfiguration(CommandLine cmd) {
		/*
		 * Read configuration file
		 */
		String configFilename = defaultConfigFilename;
		if (cmd.hasOption("c")) {
			configFilename = cmd.getOptionValue("c");
		}

		Properties props = new Properties();
		try {
			FileInputStream fis = new FileInputStream(configFilename);
			props.load(fis);
			fis.close();
			PropertyConfigurator.configure(props);
			logger.debug("Log4j initialized");
		} catch (FileNotFoundException e) {
			System.out.println("Configuration file not found: ["
					+ configFilename + "]");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Bad configuration file: [" + configFilename
					+ "]");
			e.printStackTrace();
		}
		return props;
	}
	
	private static Options createCommandLineOptions() {
		Option optConfig = new Option("c", "config", true,
				"configuration file location");

		Option optTorrent = new Option("t", "torrent", true,
				"a .torrent file to download");
		optTorrent.setRequired(true);

		Option optFile = new Option("f", "file", true,
				"a file to download on the specified tracker");
		optTorrent.setRequired(true);

		OptionGroup optionGroup = new OptionGroup();
		optionGroup.addOption(optTorrent).addOption(optFile);

		Options cliOptions = new Options();
		cliOptions.addOption(optConfig);
		cliOptions.addOptionGroup(optionGroup);

		return cliOptions;
	}

}