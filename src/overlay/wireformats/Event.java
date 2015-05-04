// Author: Jordan Messec
// Date: Jan 23, 2015
// Email: jmess4@gmail.com
package overlay.wireformats;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public abstract class Event {

	protected ByteArrayInputStream baInputStream;
	protected DataInputStream din;
	protected byte eventType;

	public Event(byte[] data) throws IOException {
		this.baInputStream = new ByteArrayInputStream(data);
		this.din = new DataInputStream(new BufferedInputStream(baInputStream));
		this.eventType = din.readByte();
	}

	public Event() {
	}

	public int getType() {
		return this.eventType;
	}

	abstract byte[] getBytes() throws IOException;

}
