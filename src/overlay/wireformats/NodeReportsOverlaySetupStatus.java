// Author: Jordan Messec
// Date: Jan 23, 2015
// Email: jmess4@gmail.com
package overlay.wireformats;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class NodeReportsOverlaySetupStatus extends Event {

	private int status;
	private byte msgLen;
	private byte[] msg;

	public NodeReportsOverlaySetupStatus(byte[] data) throws IOException {
		super(data);
		this.status = din.readInt();
		this.msgLen = din.readByte();
		msg = new byte[msgLen];
		din.readFully(msg, 0, msgLen);
		baInputStream.close();
		din.close();
	}

	public NodeReportsOverlaySetupStatus(int status, byte[] msg) {
		super();
		this.eventType = 7;
		this.status = status;
		this.msgLen = (byte) msg.length;
		this.msg = msg;
	}

	public byte[] getBytes() throws IOException {
		byte[] marshalledBytes = null;
		ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(
				baOutputStream));

		dout.writeByte(this.eventType);
		dout.writeInt(this.status);
		dout.writeByte(msgLen);
		dout.write(msg, 0, msgLen);

		dout.flush();
		marshalledBytes = baOutputStream.toByteArray();

		baOutputStream.close();
		dout.close();
		return marshalledBytes;
	}

	public int getReportStatus() {
		return this.status;
	}
}
