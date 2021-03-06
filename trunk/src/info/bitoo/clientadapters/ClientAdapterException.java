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

import org.apache.log4j.Logger;

public class ClientAdapterException extends Exception {

	/**
	 * @param arg0
	 * @param arg1
	 */
	public ClientAdapterException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public ClientAdapterException(String arg0) {
		super(arg0);
	}
	
	public static ClientAdapterException generate(Logger logger, String message, Throwable t) throws ClientAdapterException {
		logger.error(message, t);
		throw new ClientAdapterException(message, t);
	}

	/**
	 * @param logger
	 * @param string
	 * @throws ClientAdapterException
	 */
	public static void generate(Logger logger, String string) throws ClientAdapterException {
		logger.equals(string);
		throw new ClientAdapterException(string);
	}
}
