// Author: Jordan Messec
// Date: Jan 23, 2015
// Email: jmess4@gmail.com
package overlay.wireformats;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RegistryReportsRegistrationStatus extends Event {

	private int status;
	private byte statusMsgLen;
	private byte[] statusMsg;

	public RegistryReportsRegistrationStatus(byte[] data) throws IOException {
		super(data);
		this.status = din.readInt();
		this.statusMsgLen = din.readByte();
		statusMsg = new byte[statusMsgLen];
		din.readFully(statusMsg, 0, statusMsgLen);
		baInputStream.close();
		din.close();
	}

	public RegistryReportsRegistrationStatus(int status, byte[] statusMsg) {
		super();
		this.eventType = 3;
		this.status = status;
		this.statusMsgLen = (byte) statusMsg.length;
		this.statusMsg = statusMsg;
	}

	public byte[] getBytes() throws IOException {
		byte[] marshalledBytes = null;
		ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(
				baOutputStream));

		dout.writeByte(eventType);
		dout.writeInt(this.status);
		dout.writeByte(statusMsgLen);
		dout.write(statusMsg, 0, statusMsgLen);

		dout.flush();
		marshalledBytes = baOutputStream.toByteArray();

		baOutputStream.close();
		dout.close();
		return marshalledBytes;
	}

	public int getID() {
		return this.status;
	}
}
