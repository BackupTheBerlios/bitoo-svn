package info.bitoo.utils;

import info.bitoo.FetchTorrent;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.impl.TOTorrentDeserialiseImpl;

import com.sun.corba.se.internal.iiop.LocalClientRequestImpl;

public class TorrentRemaker {

	public static void main(String[] args) throws IOException, TOTorrentException {
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(createCommandLineOptions(), args);
		} catch (ParseException e) {
			System.err.println("Parsing failed.  Reason: " + e.getMessage());
		}

		StringTokenizer stLocations = new StringTokenizer(cmd.getOptionValue("l"), ",");
		List locations = new ArrayList(stLocations.countTokens());
		
		while(stLocations.hasMoreTokens()) {
			URL locationURL = new URL((String)stLocations.nextToken());
			locations.add(locationURL.toString());
		}
		
		String torrentFileName = cmd.getOptionValue("t");
		
		File torrentFile = new File(torrentFileName);
		
		TOTorrentDeserialiseImpl torrent = new TOTorrentDeserialiseImpl(torrentFile);
		
		torrent.setAdditionalListProperty("alternative locations", locations);
		
		torrent.serialiseToBEncodedFile(torrentFile);
		
	}

	/**
	 * @return
	 */
	private static Options createCommandLineOptions() {
		Option optTorrent = new Option("t", "torrent", true,
				"torrent file to remake");
		optTorrent.setRequired(true);

		Option optLocations = new Option("l", "locations", true,
				"comma separated list of alternative download locations");
		optLocations.setRequired(true);

		Options cliOptions = new Options();
		cliOptions.addOption(optTorrent);
		cliOptions.addOption(optLocations);

		return cliOptions;
	}
}