// Author: Jordan Messec
// Date: Jan 23, 2015
// Email: jmess4@gmail.com
package overlay.wireformats;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class OverlayNodeReportsTrafficSummary extends Event {

	private int nodeID;
	private int totalPacketsSent;
	private int totalRelayed;
	private long dataSentSum;
	private int totalReceived;
	private long dataReceivedSum;

	public OverlayNodeReportsTrafficSummary(byte[] data) throws IOException {
		super(data);
		this.nodeID = din.readInt();
		this.totalPacketsSent = din.readInt();
		this.totalRelayed = din.readInt();
		this.dataSentSum = din.readLong();
		this.totalReceived = din.readInt();
		this.dataReceivedSum = din.readLong();
		baInputStream.close();
		din.close();
	}

	public OverlayNodeReportsTrafficSummary(int nodeID, int totalPacketsSent,
			int totalRelayed, long dataSentSum, int totalReceived,
			long dataReceivedSum) {
		this.eventType = 12;
		this.nodeID = nodeID;
		this.totalPacketsSent = totalPacketsSent;
		this.totalRelayed = totalRelayed;
		this.dataSentSum = dataSentSum;
		this.totalReceived = totalReceived;
		this.dataReceivedSum = dataReceivedSum;
	}

	public byte[] getBytes() throws IOException {
		byte[] marshalledBytes = null;
		ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(
				baOutputStream));

		dout.writeByte(eventType);
		dout.writeInt(nodeID);
		dout.writeInt(totalPacketsSent);
		dout.writeInt(totalRelayed);
		dout.writeLong(dataSentSum);
		dout.writeInt(totalReceived);
		dout.writeLong(dataReceivedSum);

		dout.flush();
		marshalledBytes = baOutputStream.toByteArray();

		baOutputStream.close();
		dout.close();
		return marshalledBytes;
	}

	public int getNodeID() {
		return this.nodeID;
	}

	public int getTotalPacketsSent() {
		return this.totalPacketsSent;
	}

	public int getTotalPacketsRelayed() {
		return this.totalRelayed;
	}

	public long getSumDataSent() {
		return this.dataSentSum;
	}

	public int getTotalPacketsReceived() {
		return this.totalReceived;
	}

	public long getSumDataReceived() {
		return this.dataReceivedSum;
	}
}
