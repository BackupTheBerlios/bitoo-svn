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
 * 
 * 
 */
package info.bitoo.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.impl.TOTorrentDeserialiseImpl;

public class BiToorrentRemaker {

	public static void main(String[] args) throws IOException, TOTorrentException {
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(createCommandLineOptions(), args);
		} catch (ParseException e) {
			System.err.println("Parsing failed.  Reason: " + e.getMessage());
		}

		StringTokenizer stLocations = new StringTokenizer(cmd.getOptionValue("l"), "|");
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
				"pipe separated list of alternative download locations.");
		optLocations.setRequired(true);

		Options cliOptions = new Options();
		cliOptions.addOption(optTorrent);
		cliOptions.addOption(optLocations);

		return cliOptions;
	}
}