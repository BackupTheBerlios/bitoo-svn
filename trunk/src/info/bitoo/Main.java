/*
 * Created on 28-nov-2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
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

/**
 * @author abel
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public final class Main {
	private final static Logger logger = Logger
	.getLogger(Main.class.getName());
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
			System.out.println("Configuration file not found: [" + configFilename
					+ "]");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Bad configuration file: [" + configFilename + "]");
			e.printStackTrace();
		}		
		
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

		Thread main = new Thread(biToo);
		main.setName("BiToo");
		main.start();
		
		//wait until thread complete
		main.join();
		
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