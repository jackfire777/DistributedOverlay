// Author: Jordan Messec
// Date: Jan 23, 2015
// Email: jmess4@gmail.com
package overlay.transport;

import java.util.HashMap;

public class TCPConnectionsCache {
	
	private HashMap<Integer, TCPConnection> connections;
	
	public TCPConnectionsCache(){
		this.connections = new HashMap<Integer, TCPConnection>();
	}
	
	public void putConnection(TCPConnection value, int key){
		this.connections.put(key, value);
	}
	
	public TCPConnection getConnection(int key){
		return this.connections.get((Integer)key);
	}
	
	public HashMap<Integer, TCPConnection> getConnections() {
		return new HashMap<Integer, TCPConnection>(connections);
	}
}
