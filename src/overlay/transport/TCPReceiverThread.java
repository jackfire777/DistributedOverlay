// Author: Jordan Messec
// Date: Jan 23, 2015
// Email: jmess4@gmail.com
package overlay.transport;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import overlay.node.Node;
import overlay.wireformats.EventFactory;

public class TCPReceiverThread implements Runnable {
	private Socket socket;
	private DataInputStream din;
	private Node owner;
	private TCPConnection myConnection;

	public TCPReceiverThread(Socket socket, Node owner,
			TCPConnection myConnection) throws IOException {
		this.socket = socket;
		this.owner = owner;
		this.din = new DataInputStream(socket.getInputStream());
		this.myConnection = myConnection;
	}

	public void run() {
		int dataLength;
		while (socket != null) {
			try {
				dataLength = din.readInt();

				byte[] data = new byte[dataLength];

				din.readFully(data, 0, dataLength);

				owner.onEvent(EventFactory.getInstance().buildEvent(data),
						myConnection);

			} catch (SocketException se) {
				/* not printing the following error msg simply helps keep output
				 * clean, but assumes non debug status.
				 * System.out.println(se.getMessage() +
				 * " receiversocket, socketexception");
				*/
			} catch (IOException ioe) {
				/* not printing the following error msg simply helps keep output
				 * clean, but assumes non debug status.
				 * System.out.println(ioe.getMessage() +
				 * " receiversocket, ioexception");
				 * break;
				*/
			}
		}
	}
}
