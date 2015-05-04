// Author: Jordan Messec
// Date: Jan 23, 2015
// Email: jmess4@gmail.com
package overlay.wireformats;

public final class Protocol {

	public static final byte OverlayNodeSendsRegistration = 2;
	public static final byte RegistryReportsRegistrationStatus = 3;
	public static final byte OverlayNodeSendsDeregistration = 4;
	public static final byte RegistryReportsDeregistrationStatus = 5;
	public static final byte RegistrySendsNodeManifest = 6;
	public static final byte NodeReportsOverlaySetupStatus = 7;
	public static final byte RegistryRequestsTaskInitiate = 8;
	public static final byte OverlayNodeSendsData = 9;
	public static final byte OverlayNodeReportsTaskFinished = 10;
	public static final byte RegistryRequestsTrafficSummary = 11;
	public static final byte OverlayNodeReportsTrafficSummary = 12;

	private Protocol() {

	}
}
