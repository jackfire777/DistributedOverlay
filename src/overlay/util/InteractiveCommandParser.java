// Author: Jordan Messec
// Date: Jan 23, 2015
// Email: jmess4@gmail.com
package overlay.util;

import java.io.IOException;
import java.util.Scanner;

import overlay.node.MessagingNode;
import overlay.node.Node;
import overlay.node.Registry;

public class InteractiveCommandParser {

	private Scanner scanIn;
	Node owner;

	public InteractiveCommandParser(Scanner scanIn, Node owner) {
		this.scanIn = scanIn;
		this.owner = owner;
	}

	public void readCommands() {
		while (true) {
			String command = this.scanIn.nextLine();
			String[] commands = command.split(" ");
			int commandLen = commands.length;
			if (owner instanceof MessagingNode) {
				switch (command) {
				case "print-counters-and-diagnostics":
					((MessagingNode) owner).printCountersAndDiagnostics();
					break;
				case "exit-overlay":
					((MessagingNode) owner).exitOverlay();
					break;
				default:
					System.out.println("Invalid command");
					break;
				}
			}
			if (owner instanceof Registry) {
				switch (commands[0]) {
				case "list-messaging-nodes":
					((Registry) owner).listMsgNodes();
					break;
				case "setup-overlay":
					if (commandLen == 2)
						((Registry) owner).setupOverlay(Integer
								.parseInt(commands[1]));
					else
						((Registry) owner).setupOverlay(3);
					break;
				case "list-routing-tables":
					((Registry) owner).listRoutingTables();
					break;
				case "start":
					try {
						((Registry) owner).start(Integer.parseInt(commands[1]));
					} catch (NumberFormatException e) {
						System.out.println("trouble telling nodes to start sending msgs");
					} catch (IOException e) {
						System.out.println("trouble telling nodes to start sending msgs");
					}
					break;
				default:
					System.out.println("Invalid command");
					break;
				}
			}
		}
	}
}
