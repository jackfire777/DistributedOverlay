// Author: Jordan Messec
// Date: Jan 23, 2015
// Email: jmess4@gmail.com
package overlay.wireformats;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class OverlayNodeSendsDeregistration extends Event {

	private byte[] ipAddress;
	private byte ipAddressLen; // sending node's ip address
	private int sendingNodePortNum;
	private int assignedID;

	public OverlayNodeSendsDeregistration(byte[] data) throws IOException {
		super(data);
		this.ipAddressLen = din.readByte();
		this.ipAddress = new byte[ipAddressLen];
		din.readFully(ipAddress, 0, (int) ipAddressLen);
		this.sendingNodePortNum = din.readInt();
		this.assignedID = din.readInt();
		baInputStream.close();
		din.close();
	}

	public OverlayNodeSendsDeregistration(byte[] ipAddress, int portNum, int ID) {
		super();
		this.eventType = 4;
		this.ipAddressLen = (byte) ipAddress.length;
		this.ipAddress = ipAddress;
		this.sendingNodePortNum = portNum;
		this.assignedID = ID;
	}

	public byte[] getBytes() throws IOException {
		byte[] marshalledBytes = null;
		ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(
				baOutputStream));

		dout.writeByte(eventType);
		dout.writeByte(ipAddressLen);
		dout.write(ipAddress, 0, ipAddressLen);
		dout.writeInt(sendingNodePortNum);
		dout.writeInt(assignedID);

		dout.flush();
		marshalledBytes = baOutputStream.toByteArray();

		baOutputStream.close();
		dout.close();
		return marshalledBytes;
	}

	public byte[] getIP() {
		return this.ipAddress;
	}

	public int getPort() {
		return this.sendingNodePortNum;
	}

	public int getID() {
		return this.assignedID;
	}

}
