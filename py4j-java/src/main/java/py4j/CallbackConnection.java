/******************************************************************************
 * Copyright (c) 2009-2016, Barthelemy Dagenais and individual contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * - The name of the author may not be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *****************************************************************************/
package py4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.SocketFactory;

/**
 * <p>
 * Default implementation of the CommunicationChannel interface using TCP
 * sockets.
 * </p>
 * 
 * @author Barthelemy Dagenais
 * 
 */
public class CallbackConnection implements Py4JClientConnection {

	private boolean used;

	public final static int DEFAULT_NONBLOCKING_SO_TIMEOUT = 1000;
	private final int port;

	private final InetAddress address;

	private final SocketFactory socketFactory;

	private Socket socket;

	private BufferedReader reader;

	private BufferedWriter writer;

	private final Logger logger = Logger.getLogger(CallbackConnection.class.getName());

	public CallbackConnection(int port, InetAddress address) {
		this(port, address, SocketFactory.getDefault());
	}

	public CallbackConnection(int port, InetAddress address, SocketFactory socketFactory) {
		super();
		this.port = port;
		this.address = address;
		this.socketFactory = socketFactory;
	}

	public String sendCommand(String command) {
		return this.sendCommand(command, true);
	}

	public String sendCommand(String command, boolean blocking) {
		logger.log(Level.INFO, "Sending CB command: " + command);
		String returnCommand = null;
		try {
			this.used = true;
			writer.write(command);
			writer.flush();

			if (blocking) {
				returnCommand = this.readBlockingResponse(this.reader);
			} else {
				returnCommand = this.readNonBlockingResponse(this.socket, this.reader);
			}
		} catch (Exception e) {
			throw new Py4JNetworkException("Error while sending a command: " + command, e);
		}

		if (Protocol.isReturnMessage(returnCommand)) {
			returnCommand = returnCommand.substring(1);
		}

		logger.log(Level.INFO, "Returning CB command: " + returnCommand);
		return returnCommand;
	}

	protected String readBlockingResponse(BufferedReader reader) throws IOException {
		return reader.readLine();
	}

	protected String readNonBlockingResponse(Socket socket, BufferedReader reader) throws IOException {
		String returnCommand = null;

		socket.setSoTimeout(DEFAULT_NONBLOCKING_SO_TIMEOUT);

		while (true) {
			try {
				returnCommand = reader.readLine();
				break;
			} finally {
				// Set back blocking timeout (necessary if
				// sockettimeoutexception is raised and propagated)
				socket.setSoTimeout(0);
			}
		}

		// Set back blocking timeout
		socket.setSoTimeout(0);

		return returnCommand;
	}

	public void setUsed(boolean used) {
		this.used = used;
	}

	public void shutdown() {
		NetworkUtil.quietlyClose(reader);
		NetworkUtil.quietlyClose(writer);
		NetworkUtil.quietlyClose(socket);
	}

	public void start() throws IOException {
		logger.info("Starting Communication Channel on " + address + " at " + port);
		socket = socketFactory.createSocket(address, port);
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), Charset.forName("UTF-8")));
		writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), Charset.forName("UTF-8")));
	}

	public boolean wasUsed() {
		return used;
	}

}
