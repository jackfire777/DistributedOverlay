// Author: Jordan Messec
// Date: Jan 23, 2015
// Email: jmess4@gmail.com
package overlay.node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;

import overlay.transport.TCPConnection;
import overlay.transport.TCPConnectionsCache;
import overlay.transport.TCPServerThread;
import overlay.util.InteractiveCommandParser;
import overlay.wireformats.Event;
import overlay.wireformats.NodeReportsOverlaySetupStatus;
import overlay.wireformats.OverlayNodeReportsTaskFinished;
import overlay.wireformats.OverlayNodeReportsTrafficSummary;
import overlay.wireformats.OverlayNodeSendsData;
import overlay.wireformats.OverlayNodeSendsDeregistration;
import overlay.wireformats.OverlayNodeSendsRegistration;
import overlay.wireformats.Protocol;
import overlay.wireformats.RegistryReportsDeregistrationStatus;
import overlay.wireformats.RegistryReportsRegistrationStatus;
import overlay.wireformats.RegistryRequestsTaskInitiate;
import overlay.wireformats.RegistrySendsNodeManifest;

public class MessagingNode extends Thread implements Node {

	private Queue<OverlayNodeSendsData> relayQueue = new LinkedList<OverlayNodeSendsData>();

	private TCPConnection registry;				//the single RegistryNode we'll connect to
	private TCPServerThread incomingMsgNodes;	//this manages incoming msgs from MsgingNodes
	private TCPConnectionsCache nodesToSendTo;	//routing table
	private int[] allNodes;

	private Random rand;

	private int sendTracker = 0;		// keeps track of how many msgs we send
	private int receiveTracker = 0;		// keeps track of how many msgs we receive
	private int relayTracker = 0;		// keeps track of how many packets we relay
	private long sendSummation = 0;		// keeps track of sum of values sent in pckts
	private long receiveSummation = 0;	// keeps track of sum of values recieved in pckts

	private byte[] myIPAddress;
	private int myServerThreadPortNum;	//Port # this MsgNode listens on for incoming getConnections()
	private int myAssignedID;			//this MsgNode's ID in DHT

	public MessagingNode(String registryHost, int registryPort) {
		rand = new Random();
		// Connect to the Registry
		try {
			Socket registrySocket = new Socket(registryHost, registryPort);
			this.registry = new TCPConnection(registrySocket, this);
		} catch (UnknownHostException e) {
			System.out.println(e.getMessage()
					+ " Trouble opening socket to Registry.");
		} catch (IOException e) {
			System.out.println(e.getMessage()
					+ " Trouble opening socket to Registry.");
		}
		// Prepare the routing table
		nodesToSendTo = new TCPConnectionsCache();
	}

	// making this class runnable helps to ensure no deadlock occurs
	public void run() {
		while (true) {
			OverlayNodeSendsData relayMsg;
			synchronized (relayQueue) {
				relayMsg = relayQueue.poll();
			}
			if (relayMsg != null) {
				try {
					byte[] bytesToSend = relayMsg.getBytes();
					// send the bytes through the appropriate TCPSender
					nodesToSendTo.getConnections()
							.get(relayPayloadDestination(relayMsg)).getSender()
							.sendData(bytesToSend);
					// Increment the relay counter
				} catch (IOException e) {
					System.err.println(e.getMessage());
				}
			}
		}
	}

	//Determines how to handle incoming msgs, both from Registry and peers
	//event is the msg, and associatedConnection is the originator of event
	@Override
	public void onEvent(Event event, TCPConnection associatedConnection) {
		switch (event.getType()) {
		//Extract our assigned ID
		case Protocol.RegistryReportsRegistrationStatus:
			this.myAssignedID = ((RegistryReportsRegistrationStatus) event)
					.getID();
			break;
		//Ack from Registry that this node has deregistered, ID==-1 if error
		case Protocol.RegistryReportsDeregistrationStatus:
			System.out.println("This node with the ID:"
					+ ((RegistryReportsDeregistrationStatus) event).getID()
					+ " has deregistered and is now exiting.");
			System.exit(0);
			break;
		//Registry sent routing tables. Setup routing tables
		case Protocol.RegistrySendsNodeManifest:
			try {
				if (initiateConnections((RegistrySendsNodeManifest) event)) {
					String msg = "Successfully connected to routing nodes";
					registry.getSender().sendData(
							new NodeReportsOverlaySetupStatus(myAssignedID, msg
									.getBytes(Charset.forName("UTF-8")))
									.getBytes());
				} else {
					String msg = "!Could not connect to routing nodes! Or couldn't send status on connecting";
					registry.getSender().sendData(
							new NodeReportsOverlaySetupStatus(-1, msg
									.getBytes(Charset.forName("UTF-8")))
									.getBytes());
				}
			} catch (UnknownHostException e) {
				System.out.println(e.getMessage()
						+ "trouble connecting to routing nodes");
				String msg = "!Could not connect to routing nodes! Or couldn't send status on connecting";
				try {
					registry.getSender().sendData(
							new NodeReportsOverlaySetupStatus(-1, msg
									.getBytes(Charset.forName("UTF-8")))
									.getBytes());
				} catch (IOException e1) {
					System.out.println(e1.getMessage());
				}
			} catch (IOException e) {
				System.out.println(e.getMessage());
				String msg = "!Could not connect to routing nodes!";
				try {
					registry.getSender().sendData(
							new NodeReportsOverlaySetupStatus(-1, msg
									.getBytes(Charset.forName("UTF-8")))
									.getBytes());
				} catch (IOException e1) {
					System.out.println(e1.getMessage());
				}
			}
			break;
		//Registry instructs node to begin communications with peers
		case Protocol.RegistryRequestsTaskInitiate:
			sendPayloads(((RegistryRequestsTaskInitiate) event).getNumPackets());
			break;
		//Peer data packet received
		case Protocol.OverlayNodeSendsData:
			//Determine if this node is final destination of packet
			if (finalDestination((OverlayNodeSendsData) event)) {
				this.incrementReceiveTracker();
				this.incrementReceiveSummation(((OverlayNodeSendsData) event)
						.getPayload());
			}
			//if the message should be relayed
			else {
				synchronized (relayQueue) {
					OverlayNodeSendsData eventUpdated = new OverlayNodeSendsData(
							(OverlayNodeSendsData) event, myAssignedID);
					relayQueue.add(eventUpdated);
					this.incrementRelayTracker();
				}
			}
			break;
		//Registry requests update on traffic, respond accordingly
		case Protocol.RegistryRequestsTrafficSummary:
			sendTrafficSummary();
			break;
		//Should not be triggered, output error msg
		default:
			System.out.println("MsgNode received improper Event type");
			break;
		}
	}

	//register this MsgNode with the Registry.  We will get back an assigned ID
	private void register() {
		InetAddress iNA;
		try {
			iNA = InetAddress.getLocalHost();
			myIPAddress = iNA.getAddress();
			OverlayNodeSendsRegistration register = new OverlayNodeSendsRegistration(
					myIPAddress, myServerThreadPortNum);
			this.registry.getSender().sendData(register.getBytes());
		} catch (UnknownHostException e1) {
			System.out.println(e1.getMessage()
					+ " Trouble sending registration event.");
		} catch (IOException e) {
			System.out.println(e.getMessage()
					+ " Trouble sending registration event.");
		}
	}

	//Deregister from Registry.  Will receive ack from Registry
	private void deRegister() {
		try {
			OverlayNodeSendsDeregistration deregister = new OverlayNodeSendsDeregistration(
					myIPAddress, myServerThreadPortNum, this.myAssignedID);
			this.registry.getSender().sendData(deregister.getBytes());
		} catch (UnknownHostException e1) {
			System.out.println(e1.getMessage()
					+ " Trouble sending DEregistration event.");
		} catch (IOException e) {
			System.out.println(e.getMessage()
					+ " Trouble sending DEregistration event.");
		}
	}

	//Have received routing table, make appropriate getConnections()
	private boolean initiateConnections(RegistrySendsNodeManifest event)
			throws UnknownHostException, IOException {
		// close all getConnections() from previous overlay.
		for (Map.Entry<Integer, TCPConnection> entry : this.nodesToSendTo.getConnections()
				.entrySet()) {
			TCPConnection connector = entry.getValue();
			connector.closeSocket();
		}
		//Create sockets to peers
		allNodes = event.getAllNodes();
		int numConnectionsToMake = event.getRoutingTableSize();
		nodesToSendTo = new TCPConnectionsCache();
		for (int i = 0; i < numConnectionsToMake; i++) {
			String ipAdd = "";
			for (int j = 0; j < event.getIPLen(i); j++) {
				int ipPortion = event.getHopIP(i)[j] & 0xFF;
				ipAdd += ipPortion;
				if (j < event.getIPLen(i) - 1)
					ipAdd += ".";
			}
			Socket hopSocket = new Socket(ipAdd, event.getHopPort(i));
			TCPConnection hop = new TCPConnection(hopSocket, this);
			nodesToSendTo.putConnection(hop, event.getHopID(i));
		}
		//Ensure each socket connection was successful
		boolean connectionsAllGood = true;
		for (Map.Entry<Integer, TCPConnection> entry : nodesToSendTo.getConnections()
				.entrySet()) {
			TCPConnection value = entry.getValue();
			if (!value.checkSocketStatus())
				return false;
		}
		return connectionsAllGood;
	}

	//Create packet payload and choose destination, both are random
	//After all packets sent, report TaskFinished to Registry
	private void sendPayloads(int numPayloads) {
		for (int i = 0; i < numPayloads; i++) {
			int destination = rand.nextInt(allNodes.length);
			while (allNodes[destination] == myAssignedID) {
				destination = rand.nextInt(allNodes.length);
			}
			int payload = rand.nextInt();
			this.sendSummation += payload;
			int[] traceField = { myAssignedID };
			OverlayNodeSendsData dataPacket = new OverlayNodeSendsData(
					allNodes[destination], myAssignedID, payload, 1, traceField);
			try {
				nodesToSendTo.getConnections()
						.get(relayPayloadDestination(dataPacket)).getSender()
						.sendData(dataPacket.getBytes());
			} catch (IOException e) {
				System.out.println(e.getMessage()
						+ " trouble sending packet to first hop");
			}
			this.sendTracker++;
		}
		try {
			registry.getSender().sendData(
					new OverlayNodeReportsTaskFinished(myIPAddress,
							incomingMsgNodes.getPort(), myAssignedID)
							.getBytes());
		} catch (IOException e) {
			System.out.println(e.getMessage()
					+ " trouble reporting task finished.");
		}
	}

	//Received packet that we should forward, determine best next hop
	private int relayPayloadDestination(OverlayNodeSendsData event) {
		int destination = event.getDestID();
		if (nodesToSendTo.getConnections().containsKey(destination))
			return destination;
		else { // determine which routing entry to send it to
			int bestHopLen = 128;
			int bestHop = 0;
			boolean increasedHopDist = false;
			boolean finalFlag = false;
			if (event.getDestID() < myAssignedID)
				destination += 128; // previously 127
			for (Map.Entry<Integer, TCPConnection> entry : nodesToSendTo.getConnections()
					.entrySet()) {
				int possibleHop = entry.getKey();
				if (possibleHop < myAssignedID) {
					possibleHop += 128;
					increasedHopDist = true;
				} else {
					increasedHopDist = false;
				}
				if (possibleHop < destination
						&& ((destination - possibleHop) < bestHopLen)) {
					bestHopLen = destination - possibleHop;
					bestHop = possibleHop;
					finalFlag = increasedHopDist;
				}
			}
			if (finalFlag)
				bestHop -= 128;
			return bestHop;
		}
	}

	//Report statistics to Registry
	private void sendTrafficSummary() {
		try {
			registry.getSender().sendData(
					new OverlayNodeReportsTrafficSummary(myAssignedID,
							sendTracker, relayTracker, sendSummation,
							receiveTracker, receiveSummation).getBytes());
			this.sendTracker = 0; // reset all counters after reporting
			this.relayTracker = 0;
			this.receiveTracker = 0;
			this.sendSummation = 0;
			this.receiveSummation = 0;

		} catch (IOException e) {
			System.out.println(e.getMessage()
					+ " Trouble reporting traffic summary or closing sockets.");
		}
	}

	//check if this node is final destination for a received packet
	private boolean finalDestination(OverlayNodeSendsData event) {
		if (event.getDestID() == myAssignedID)
			return true;
		return false;

	}

	private synchronized void incrementReceiveTracker() {
		this.receiveTracker++;
	}

	private synchronized void incrementRelayTracker() {
		this.relayTracker++;
	}

	private synchronized void incrementReceiveSummation(int value) {
		this.receiveSummation += value;
	}

	//Begins to listen on given port number for incoming getConnections()
	private void setUpServerThread() {
		try {
			this.incomingMsgNodes = new TCPServerThread(0, this);
			Thread thread = new Thread(incomingMsgNodes);
			thread.start();
			this.myServerThreadPortNum = incomingMsgNodes.getPort();
		} catch (IOException e) {
			System.out.println(e.getMessage()
					+ " Trouble creating Server Socket");
		}
	}

	//Sets up user interface to accept commands
	private void acceptCommands() {
		Scanner input = new Scanner(System.in);
		InteractiveCommandParser commandParser = new InteractiveCommandParser(
				input, this);
		commandParser.readCommands(); // blocking command reader
	}

	//Lets Registry know we are exiting overlay
	public void exitOverlay() {
		deRegister(); 
	}

	//Outputs statistics being tracked to standard out
	public void printCountersAndDiagnostics() {
		System.out.println("Node " + myAssignedID + "::" + "\t" + "Sent" + "\t"
				+ "Rec'd" + "\t" + "Relayed" + "\t" + "SumSent" + "\t"
				+ "SumRecd");
		System.out.println("\t\t" + this.sendTracker + "\t"
				+ this.receiveTracker + "\t" + this.relayTracker + "\t"
				+ this.sendSummation + "\t" + this.receiveSummation);
	}

	public static void main(String[] args) throws InterruptedException {
		MessagingNode tester = new MessagingNode(args[0],
				Integer.parseInt(args[1]));
		Thread thread = new Thread(tester);
		thread.start();
		System.out.println("MsgNode");
		tester.setUpServerThread();
		tester.register();
		tester.acceptCommands();

	}

}
