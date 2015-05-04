// Author: Jordan Messec
// Date: Jan 23, 2015
// Email: jmess4@gmail.com
package overlay.wireformats;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class OverlayNodeSendsData extends Event {

	private int destinationID;
	private int sourceID;
	private int payload;
	private int traceFieldLen;
	private int[] traceField;
	
	public OverlayNodeSendsData(byte[] data) throws IOException {
		super(data);
		this.destinationID = din.readInt();
		this.sourceID = din.readInt();
		this.payload = din.readInt();
		this.traceFieldLen = din.readInt();
		this.traceField = new int[traceFieldLen];
		for (int i = 0; i < traceFieldLen; i++)
			traceField[i] = din.readInt();
		baInputStream.close();
		din.close();
	}
	
	public OverlayNodeSendsData(int destID, int sourceID, int payload, int traceFieldLen, int[] traceField){
		super();
		this.eventType = 9;
		this.destinationID = destID;
		this.sourceID = sourceID;
		this.payload = payload;
		this.traceFieldLen = traceFieldLen;
		this.traceField = traceField;
	}

	public OverlayNodeSendsData(OverlayNodeSendsData template, int addID){
		this.eventType = 9;
		this.destinationID = template.getDestID();
		this.sourceID = template.getSourceID();
		this.payload = template.getPayload();
		this.traceFieldLen = template.getTraceFieldLen() + 1;
		//this.traceField = new int[traceFieldLen];
		this.traceField = Arrays.copyOf(template.getTraceField(), traceFieldLen);
		traceField[traceFieldLen-1] = addID;
	}
	
	public int getDestID(){
		return this.destinationID;
	}
	
	public int getSourceID(){
		return this.sourceID;
	}

	public int getPayload(){
		return this.payload;
	}

	public int getTraceFieldLen(){
		return this.traceFieldLen;
	}
	
	public int[] getTraceField(){
		return this.traceField;
	}
	
	public byte[] getBytes() throws IOException{
		byte[] marshalledBytes = null;
		ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));
		
		dout.writeByte(this.eventType);
		dout.writeInt(this.destinationID);
		dout.writeInt(this.sourceID);
		dout.writeInt(payload);
		dout.writeInt(traceFieldLen);
		for (int i = 0; i < traceFieldLen; i++)
			dout.writeInt(traceField[i]);
		
		dout.flush();
		marshalledBytes = baOutputStream.toByteArray();
		
		baOutputStream.close();
		dout.close();
		return marshalledBytes;
	}
	
}
