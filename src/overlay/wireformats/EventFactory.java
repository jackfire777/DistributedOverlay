// Author: Jordan Messec
// Date: Jan 23, 2015
// Email: jmess4@gmail.com
package overlay.wireformats;

import java.io.IOException;

public class EventFactory {

	private static EventFactory instance = new EventFactory();

	private EventFactory() {
	}

	public static EventFactory getInstance() {
		return instance;
	}

	public Event buildEvent(byte[] data) throws IOException {
		Event event = null;

		switch (data[0]) {
		case 2:
			event = new OverlayNodeSendsRegistration(data);
			break;
		case 3:
			event = new RegistryReportsRegistrationStatus(data);
			break;
		case 4:
			event = new OverlayNodeSendsDeregistration(data);
			break;
		case 5:
			event = new RegistryReportsDeregistrationStatus(data);
			break;
		case 6:
			event = new RegistrySendsNodeManifest(data);
			break;
		case 7:
			event = new NodeReportsOverlaySetupStatus(data);
			break;
		case 8:
			event = new RegistryRequestsTaskInitiate(data);
			break;
		case 9:
			event = new OverlayNodeSendsData(data);
			break;
		case 10:
			event = new OverlayNodeReportsTaskFinished(data);
			break;
		case 11:
			event = new RegistryRequestsTrafficSummary(data);
			break;
		case 12:
			event = new OverlayNodeReportsTrafficSummary(data);
			break;
		default:
			System.out.println("Error inside factory");
			break;
		}
		return event;
	}
}
