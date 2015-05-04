// Author: Jordan Messec
// Date: Jan 23, 2015
// Email: jmess4@gmail.com
package overlay.node;

import overlay.transport.TCPConnection;
import overlay.wireformats.Event;

public interface Node {

	void onEvent(Event event, TCPConnection activeConnection);
	
}
