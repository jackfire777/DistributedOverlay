// Author: Jordan Messec
// Date: Jan 23, 2015
// Email: jmess4@gmail.com
package overlay.transport;

import java.io.IOException;
import java.net.Socket;

import overlay.node.Node;

public class TCPConnection {

	private TCPReceiverThread receiver;
	private TCPSender sender;
	private Socket socket;

	public TCPConnection(Socket socket, Node owner) {
		try {
			this.socket = socket;
			this.receiver = new TCPReceiverThread(socket, owner, this);
			this.sender = new TCPSender(socket);
			Thread thread = new Thread(receiver);
			thread.start();
		} catch (IOException e) {
			System.err.println(e.getMessage()
					+ "Error starting Receiver Thread in TCPConn");
		}
	}

	public byte[] getIP() { // returns the remote IP
		return this.socket.getInetAddress().getAddress();
	}

	public int getRemotePort() { // returns the remote port
		return this.socket.getPort();
	}

	public int getLocalPort() {
		return this.socket.getLocalPort();
	}

	public TCPSender getSender() {
		return this.sender;
	}

	public boolean checkSocketStatus() {
		return this.socket.isConnected();
	}

	public void closeSocket() throws IOException {
		this.socket.close();
	}

}