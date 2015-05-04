// Author: Jordan Messec
// Date: Jan 23, 2015
// Email: jmess4@gmail.com
package overlay.wireformats;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RegistrySendsNodeManifest extends Event {

	private byte routingTableSize;
	private int nextHops[];
	private byte[] ipLens;
	private byte[][] ips;
	private int[] ports;
	private byte totalNodes;
	private int[] allNodes;

	public RegistrySendsNodeManifest(byte[] data) throws IOException {
		super(data);
		this.routingTableSize = din.readByte();
		this.nextHops = new int[routingTableSize];
		this.ipLens = new byte[routingTableSize];
		this.ips = new byte[routingTableSize][];
		this.ports = new int[routingTableSize];
		for (int i = 0; i < (int) routingTableSize; i++) {
			nextHops[i] = din.readInt();
			ipLens[i] = din.readByte();
			ips[i] = new byte[ipLens[i]]; // needs to be of len ipLens[i] not 4
			din.readFully(ips[i], 0, ipLens[i]); // should write len of
													// ipLens[i] not 4
			ports[i] = din.readInt();
		}

		this.totalNodes = din.readByte();
		this.allNodes = new int[totalNodes];
		for (int i = 0; i < totalNodes; i++)
			allNodes[i] = din.readInt();
		baInputStream.close();
		din.close();
	}

	public RegistrySendsNodeManifest(byte routingTableSize, int[] nextHops,
			byte[] ipLens, byte[][] ips, int[] ports, byte totalNodes,
			int[] allNodes) {
		super();
		this.eventType = 6;
		this.routingTableSize = routingTableSize;
		this.nextHops = nextHops;
		this.ipLens = ipLens;
		this.ips = ips;
		this.ports = ports;
		this.totalNodes = totalNodes;
		this.allNodes = allNodes;
	}

	public int getRoutingTableSize() {
		return this.routingTableSize;
	}

	public int getHopID(int index) {
		return nextHops[index];
	}

	public int getIPLen(int index) {
		return ipLens[index];
	}

	public byte[] getHopIP(int index) {
		return ips[index];
	}

	public int getHopPort(int index) {
		return ports[index];
	}

	public int[] getAllNodes() {
		return this.allNodes;
	}

	@Override
	public byte[] getBytes() throws IOException {
		byte[] marshalledBytes = null;
		ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(
				baOutputStream));

		dout.writeByte(eventType);
		dout.writeByte(routingTableSize);
		for (int i = 0; i < routingTableSize; i++) {
			dout.writeInt(nextHops[i]);
			dout.write(ipLens[i]);
			;
			dout.write(ips[i], 0, ipLens[i]);
			dout.writeInt(ports[i]);
		}
		dout.write(totalNodes);
		for (int i = 0; i < totalNodes; i++)
			dout.writeInt(allNodes[i]);

		dout.flush();
		marshalledBytes = baOutputStream.toByteArray();

		baOutputStream.close();
		dout.close();
		return marshalledBytes;
	}

}
