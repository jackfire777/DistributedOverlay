// Author: Jordan Messec
// Date: Jan 23, 2015
// Email: jmess4@gmail.com
package overlay.transport;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import overlay.node.Node;

public class TCPServerThread implements Runnable {

	private ServerSocket serverSocket;
	Node owner;
	private int portNum;

	public TCPServerThread(int port, Node owner) throws IOException { // ctor
																		// for
																		// RegistryNode
		this.owner = owner;
		this.serverSocket = new ServerSocket(port);
		this.portNum = this.serverSocket.getLocalPort();
	}

	@Override
	public void run() {
		while (true) {
			// waiting to hear from MessageNodes
			Socket socket;
			try {
				socket = this.serverSocket.accept();
				new TCPConnection(socket, owner); // starts the receiver thread
			} catch (IOException e) {
				System.out.println(e.getMessage() + " serversocket");
			}
		}
	}

	public int getPort() {
		return this.portNum;
	}

}
