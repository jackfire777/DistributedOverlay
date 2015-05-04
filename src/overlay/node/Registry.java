// Author: Jordan Messec
// Date: Jan 23, 2015
// Email: jmess4@gmail.com
package overlay.node;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import overlay.transport.TCPConnection;
import overlay.transport.TCPConnectionsCache;
import overlay.transport.TCPServerThread;
import overlay.util.InteractiveCommandParser;
import overlay.wireformats.Event;
import overlay.wireformats.NodeReportsOverlaySetupStatus;
import overlay.wireformats.OverlayNodeReportsTrafficSummary;
import overlay.wireformats.OverlayNodeSendsDeregistration;
import overlay.wireformats.OverlayNodeSendsRegistration;
import overlay.wireformats.Protocol;
import overlay.wireformats.RegistryReportsDeregistrationStatus;
import overlay.wireformats.RegistryReportsRegistrationStatus;
import overlay.wireformats.RegistryRequestsTaskInitiate;
import overlay.wireformats.RegistryRequestsTrafficSummary;
import overlay.wireformats.RegistrySendsNodeManifest;

public class Registry implements Node {

	// manages incoming getConnections() from MsgNodes
	private TCPServerThread incomingMsgNodes;
	private TCPConnectionsCache registeredMsgNodes;
	// tracks the ports each MsgNode is listening on for getConnections()
	private Map<Integer, Integer> advertisedPorts;
	// Ensures each assigned DHT ID is unique
	private boolean[] takenIDs;
	private final int numIDs = 128;

	private Map<Integer, String> outputs;
	private int totalPacketsSent = 0;
	private int totalPacketsReceived = 0;
	private int totalPacketsRelayed = 0;
	private long totalSumSent = 0;
	private long totalSumReceived = 0;

	private ArrayList<Integer> nodeIDs;
	private Map<Integer, ArrayList<Integer>> routingTable;
	private int finalNumNodes = 0;
	private int routingTableSize = 0;
	private int numNodesReportingSuccess = 0;
	private int numNodesReportingTaskComplete = 0;
	private int numNodesReportingTraffic = 0;

	private Random rand;

	public Registry(int portNum) {
		rand = new Random();
		takenIDs = new boolean[128];
		registeredMsgNodes = new TCPConnectionsCache();
		advertisedPorts = new HashMap<Integer, Integer>();
		outputs = new HashMap<Integer, String>();
		try {
			this.incomingMsgNodes = new TCPServerThread(portNum, this);
			Thread thread = new Thread(incomingMsgNodes);
			thread.start();
		} catch (IOException e) {
			System.out.println(e.getMessage()
					+ " problem setting up ServerSocket in registry.");
		}
	}

	// Determines how to handle incoming msgs from MsgNodes
	// event is the msg, and associatedConnection is the originator of event
	@Override
	public void onEvent(Event event, TCPConnection associatedConnection) {
		switch (event.getType()) {
		case Protocol.OverlayNodeSendsRegistration:
			try {
				registerIncomingNode((OverlayNodeSendsRegistration) (event),
						associatedConnection);
			} catch (IOException e) {
				System.out.println(e.getMessage()
						+ " problem responding to registering node.");
			}
			break;
		case Protocol.OverlayNodeSendsDeregistration:
			try {
				deregisterNode((OverlayNodeSendsDeregistration) (event),
						associatedConnection);
			} catch (IOException e) {
				System.out.println(e.getMessage()
						+ " problem responding to DEregistering node.");
			}
			break;
		case Protocol.NodeReportsOverlaySetupStatus:
			acceptOverlayStatusReport((NodeReportsOverlaySetupStatus) event);
			break;
		case Protocol.OverlayNodeReportsTaskFinished:
			incrementNumNodesReportingTaskComplete();
			if (numNodesReportingTaskComplete == finalNumNodes) {
				System.out.println("Every node reports complete.");
				try { // sleep for 20 secs to ensure all msgs have finished
						// circulating.
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					System.out.println(e.getMessage()
							+ " Registry had nightmares.");
				}
				retrieveTrafficSummaries();
			}
			break;
		case Protocol.OverlayNodeReportsTrafficSummary:
			processTrafficSummary((OverlayNodeReportsTrafficSummary) event);
			break;
		// Shouldn't occur, prints error msg
		default:
			System.out.println("Registry received improper Event type");
			break;
		}
	}

	private synchronized void registerIncomingNode(
			OverlayNodeSendsRegistration event,
			TCPConnection associatedConnection) throws IOException {
		boolean liar = checkForLiar(event, associatedConnection);
		if (liar) {
			String msg = "Registration unsuccessful because IP given did not match your IP";
			associatedConnection.getSender().sendData(
					new RegistryReportsRegistrationStatus(-1, msg
							.getBytes(Charset.forName("UTF-8"))).getBytes());
			return;
		}
		boolean previouslyRegistered = checkPreviouslyRegistered(event);
		if (previouslyRegistered) {
			String msg = "Registration unsuccessful because this port/IP combination was already registered";
			associatedConnection.getSender().sendData(
					new RegistryReportsRegistrationStatus(-1, msg
							.getBytes(Charset.forName("UTF-8"))).getBytes());
			return;
		}
		int randomID = rand.nextInt(numIDs);
		while (takenIDs[randomID] == true) {
			randomID = rand.nextInt(numIDs);
		}
		takenIDs[randomID] = true;
		registeredMsgNodes.putConnection(associatedConnection, randomID);
		advertisedPorts.put(randomID, event.getPort());
		String msg = "The number of messaging nodes currently constituting the overlay is ("
				+ registeredMsgNodes.getConnections().size() + ")";
		try { // msgNode failed right after registering, remove from map
			associatedConnection.getSender().sendData(
					new RegistryReportsRegistrationStatus(randomID, msg
							.getBytes(Charset.forName("UTF-8"))).getBytes());
		} catch (IOException e) {
			takenIDs[randomID] = false;
			registeredMsgNodes.getConnections().remove(randomID);
		}
	}

	private synchronized void deregisterNode(
			OverlayNodeSendsDeregistration event,
			TCPConnection associatedConnection) throws IOException {
		boolean liar = checkForLiar(event, associatedConnection);
		if (liar) {
			String msg = "Deregistration unsuccessful because IP/port given did not match your IP/port";
			associatedConnection.getSender().sendData(
					new RegistryReportsDeregistrationStatus(-1, msg
							.getBytes(Charset.forName("UTF-8"))).getBytes());
			return;
		}
		boolean previouslyRegistered = checkPreviouslyRegistered(event);
		if (!previouslyRegistered) {
			String msg = "Deregistration unsuccessful because this port/IP combination was not registered";
			associatedConnection.getSender().sendData(
					new RegistryReportsRegistrationStatus(-1, msg
							.getBytes(Charset.forName("UTF-8"))).getBytes());
			return;
		}
		takenIDs[event.getID()] = false;
		registeredMsgNodes.getConnections().remove(event.getID());
		associatedConnection.getSender().sendData(
				new RegistryReportsDeregistrationStatus(event.getID(),
						"Deregistered".getBytes(Charset.forName("UTF-8")))
						.getBytes());
	}

	private boolean checkPreviouslyRegistered(OverlayNodeSendsRegistration event) {
		for (Map.Entry<Integer, TCPConnection> entry : registeredMsgNodes.getConnections()
				.entrySet()) {
			TCPConnection conn = entry.getValue();
			if (Arrays.equals(conn.getIP(), event.getIP())
					&& advertisedPorts.get(entry.getKey()) == event.getPort()) {
				return true;
			}
		}
		return false;
	}

	private boolean checkPreviouslyRegistered(
			OverlayNodeSendsDeregistration event) {
		for (Map.Entry<Integer, TCPConnection> entry : registeredMsgNodes.getConnections()
				.entrySet()) {
			TCPConnection conn = entry.getValue();
			if (Arrays.equals(conn.getIP(), event.getIP())
					&& advertisedPorts.get(entry.getKey()) == event.getPort())
				return true;
		}
		return false;
	}

	//Determines if a MsgNode has provided false information
	private boolean checkForLiar(OverlayNodeSendsRegistration event,
			TCPConnection associatedConnection) {
		if (Arrays.equals(event.getIP(), associatedConnection.getIP()))
			return false;
		return true;
	}

	//Determines if a MsgNode has provided false information
	private boolean checkForLiar(OverlayNodeSendsDeregistration event,
			TCPConnection associatedConnection) {
		if (Arrays.equals(event.getIP(), associatedConnection.getIP()))
			return false;
		return true;
	}

	private synchronized void acceptOverlayStatusReport(
			NodeReportsOverlaySetupStatus event) {
		if (event.getReportStatus() == -1)
			System.out
					.println("There was a problem with one of the node's attempts to setup their overlay. Unable to proceed.");
		else {
			incrementSuccess();
			if (this.numNodesReportingSuccess == this.finalNumNodes)
				System.out.println("Registry now ready to initiate tasks.");
		}
	}

	private void acceptCommands() {
		Scanner input = new Scanner(System.in);
		InteractiveCommandParser commandParser = new InteractiveCommandParser(
				input, this);
		commandParser.readCommands(); // blocking command reader
	}

	public void listMsgNodes() {
		for (Map.Entry<Integer, TCPConnection> entry : registeredMsgNodes.getConnections()
				.entrySet()) {
			TCPConnection conn = entry.getValue();
			System.out.println(Arrays.toString(conn.getIP()) + "\t"
					+ conn.getRemotePort() + "\t" + entry.getKey());
		}
	}

	public void setupOverlay(int numEntries) {
		this.numNodesReportingSuccess = 0; // reset this tracker so you can
											// setup a new overlay for new runs
		this.routingTableSize = numEntries;
		nodeIDs = new ArrayList<Integer>();
		for (Map.Entry<Integer, TCPConnection> entry : registeredMsgNodes.getConnections()
				.entrySet()) {
			nodeIDs.add(entry.getKey());
		}
		Collections.sort(nodeIDs);
		this.finalNumNodes = registeredMsgNodes.getConnections().size();
		routingTable = new HashMap<Integer, ArrayList<Integer>>();
		for (int i = 0; i < finalNumNodes; i++) {
			ArrayList<Integer> router = new ArrayList<Integer>();
			for (int j = 0; j < numEntries; j++) {
				router.add(nodeIDs.get((int) ((i + Math.pow(2, j)) % finalNumNodes)));
			}
			routingTable.put(nodeIDs.get(i), router);
		}
		sendManifest();
	}

	private void sendManifest() {
		for (Map.Entry<Integer, TCPConnection> entry : registeredMsgNodes.getConnections()
				.entrySet()) {
			try {
				entry.getValue().getSender()
						.sendData(createManifest(entry.getKey()).getBytes());
			} catch (IOException e) {
				System.out.println(e.getMessage()
						+ " problem sending manifest to MsgNode.");
			}
		}
	}

	private RegistrySendsNodeManifest createManifest(Integer key) {
		ArrayList<Integer> nextHopsAL = routingTable.get(key);
		int[] nextHops = new int[routingTableSize];
		for (int i = 0; i < nextHopsAL.size(); i++) {
			nextHops[i] = nextHopsAL.get(i).intValue();
		}
		byte[] ipLens = new byte[routingTableSize];
		byte[][] ips = new byte[routingTableSize][];
		int[] ports = new int[routingTableSize];
		for (int i = 0; i < routingTableSize; i++) {
			TCPConnection hop = registeredMsgNodes.getConnection(nextHopsAL
					.get(i));
			ipLens[i] = (byte) hop.getIP().length;
			ips[i] = hop.getIP();
			ports[i] = advertisedPorts.get(nextHopsAL.get(i));
		}
		byte totalNodes = (byte) finalNumNodes;
		int[] allNodes = new int[finalNumNodes];
		for (int i = 0; i < finalNumNodes; i++)
			allNodes[i] = nodeIDs.get(i).intValue();
		return new RegistrySendsNodeManifest((byte) routingTableSize, nextHops,
				ipLens, ips, ports, totalNodes, allNodes);
	}

	private void retrieveTrafficSummaries() {
		for (Map.Entry<Integer, TCPConnection> entry : registeredMsgNodes.getConnections()
				.entrySet()) {
			TCPConnection eachMsgNode = entry.getValue();
			try {
				eachMsgNode.getSender().sendData(
						new RegistryRequestsTrafficSummary().getBytes());
			} catch (IOException e) {
				System.out.println(e.getMessage()
						+ " Trouble requesting traffic summaries.");
			}
		}
	}

	private synchronized void processTrafficSummary(
			OverlayNodeReportsTrafficSummary event) {
		incrementNodesReportingTraffic();
		accumulateData(event);
		createOutputString((OverlayNodeReportsTrafficSummary) event);
		if (numNodesReportingTraffic == finalNumNodes){
			outputResults();
		}
	}

	private synchronized void createOutputString(
			OverlayNodeReportsTrafficSummary event) {
		String outputString = "" + "\t" + event.getTotalPacketsSent() + "\t"
				+ event.getTotalPacketsReceived() + "\t\t"
				+ event.getTotalPacketsRelayed() + "\t\t"
				+ event.getSumDataSent() + "\t" + event.getSumDataReceived();
		outputs.put(event.getNodeID(), outputString);
	}

	private void outputResults() {
		System.out.println("\t\t" + "PSent" + "\t" + "PReceived" + "\t"
				+ "PRelayed" + "\t" + "SumValuesSent" + "\t"
				+ "SumValuesReceived");
		for (Map.Entry<Integer, String> event : outputs.entrySet()) {
			System.out.println("Node" + event.getKey() + "\t"
					+ event.getValue());
		}
		System.out.println("Sum" + "\t\t" + totalPacketsSent + "\t"
				+ totalPacketsReceived + "\t\t" + totalPacketsRelayed + "\t\t"
				+ totalSumSent + "\t" + totalSumReceived);

		// reset variables for another run
		this.numNodesReportingTaskComplete = 0;
		this.numNodesReportingTraffic = 0;
		this.totalPacketsReceived = 0;
		this.totalPacketsRelayed = 0;
		this.totalPacketsSent = 0;
		this.totalSumReceived = 0;
		this.totalSumSent = 0;
		this.outputs.clear();
	}

	private synchronized void accumulateData(
			OverlayNodeReportsTrafficSummary event) {
		this.totalPacketsSent += event.getTotalPacketsSent();
		this.totalPacketsReceived += event.getTotalPacketsReceived();
		this.totalPacketsRelayed += event.getTotalPacketsRelayed();
		this.totalSumReceived += event.getSumDataReceived();
		this.totalSumSent += event.getSumDataSent();
	}

	private synchronized void incrementSuccess() {
		this.numNodesReportingSuccess++;
	}

	private synchronized void incrementNodesReportingTraffic() {
		this.numNodesReportingTraffic++;
	}

	private synchronized void incrementNumNodesReportingTaskComplete() {
		this.numNodesReportingTaskComplete++;
	}

	public void listRoutingTables() {
		for (Map.Entry<Integer, ArrayList<Integer>> entry : routingTable
				.entrySet()) {
			System.out.println(entry.getKey() + "\t" + entry.getValue());
			System.out.println("\n");
		}
	}

	public void start(int numMsgs) throws IOException {
		for (Map.Entry<Integer, TCPConnection> entry : registeredMsgNodes.getConnections()
				.entrySet()) {
			entry.getValue()
					.getSender()
					.sendData(
							new RegistryRequestsTaskInitiate(numMsgs)
									.getBytes());
		}
	}

	public static void main(String[] args) {
		Registry tester = new Registry(Integer.parseInt(args[0]));
		System.out.println("Registry");
		tester.acceptCommands();
	}

}
