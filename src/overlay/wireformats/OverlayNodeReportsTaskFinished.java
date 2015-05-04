// Author: Jordan Messec
// Date: Jan 23, 2015
// Email: jmess4@gmail.com
package overlay.wireformats;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class OverlayNodeReportsTaskFinished extends Event {

	private byte ipAddrLen;
	private byte[] ipAddr;
	private int portNum;
	private int nodeID;

	public OverlayNodeReportsTaskFinished(byte[] data) throws IOException {
		super(data);
		this.ipAddrLen = din.readByte();
		ipAddr = new byte[ipAddrLen];
		din.readFully(ipAddr, 0, ipAddrLen);
		this.portNum = din.readInt();
		this.nodeID = din.readInt();
		baInputStream.close();
		din.close();
	}

	public OverlayNodeReportsTaskFinished(byte[] ipAddr, int portNum, int nodeID) {
		super();
		this.eventType = 10;
		this.ipAddrLen = (byte) ipAddr.length;
		this.ipAddr = ipAddr;
		this.nodeID = nodeID;
	}

	public byte[] getBytes() throws IOException {
		byte[] marshalledBytes = null;
		ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(
				baOutputStream));

		dout.writeByte(this.eventType);
		dout.writeByte(this.ipAddrLen);
		dout.write(ipAddr, 0, ipAddrLen);
		dout.writeInt(portNum);
		dout.writeInt(nodeID);

		dout.flush();
		marshalledBytes = baOutputStream.toByteArray();

		baOutputStream.close();
		dout.close();
		return marshalledBytes;
	}

	public byte[] getIP() {
		return this.ipAddr;
	}

	public int getPort() {
		return this.portNum;
	}

}
