// Author: Jordan Messec
// Date: Jan 23, 2015
// Email: jmess4@gmail.com
package overlay.wireformats;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RegistryRequestsTaskInitiate extends Event {

	private int numPackets;

	public RegistryRequestsTaskInitiate(byte[] data) throws IOException {
		super(data);
		numPackets = din.readInt();
		baInputStream.close();
		din.close();
	}

	public RegistryRequestsTaskInitiate(int numPackets) {
		super();
		this.eventType = 8;
		this.numPackets = numPackets;
	}

	public byte[] getBytes() throws IOException {
		byte[] marshalledBytes = null;
		ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(
				baOutputStream));

		dout.writeByte(eventType);
		dout.writeInt(this.numPackets);

		dout.flush();
		marshalledBytes = baOutputStream.toByteArray();

		baOutputStream.close();
		dout.close();
		return marshalledBytes;
	}

	public int getNumPackets() {
		return this.numPackets;
	}
}
