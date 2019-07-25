package routing.community;


import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;


public class SnWDecisionEngine implements RoutingDecisionEngine
{
	/** identifier for the initial number of copies setting ({@value})*/
	public static final String NROF_COPIES_S = "nrofCopies";

	/** Message property key for the remaining available copies of a message */
	public static final String MSG_COUNT_PROP = "SprayAndFocus.copies";

	/** identifier for the binary-mode setting ({@value})*/
	public static final String BINARY_MODE = "binaryMode";

	protected int initialNrofCopies;
	protected boolean isBinary;

	public SnWDecisionEngine(Settings s)
	{
		initialNrofCopies = s.getInt(NROF_COPIES_S);
		isBinary = s.getBoolean(BINARY_MODE);

	}

	public SnWDecisionEngine(SnWDecisionEngine proto)
	{
		this.initialNrofCopies = proto.initialNrofCopies;
		this.isBinary = proto.isBinary;
	}

	/**
	 * Called when a connection goes up between this host and a peer. Note that,
	 * doExchangeForNewConnection() may be called first.
	 *
	 * @param thisHost
	 * @param peer
	 */
	@Override
	public void connectionUp(DTNHost thisHost, DTNHost peer) {}

	/**
	 * Called when a connection goes down between this host and a peer.
	 *
	 * @param thisHost
	 * @param peer
	 */
	@Override
	public void connectionDown(DTNHost thisHost, DTNHost peer) {}

	/**
	 * Called once for each connection that comes up to give two decision engine
	 * objects on either end of the connection to exchange and update information
	 * in a simultaneous fashion. This call is provided so that one end of the
	 * connection does not perform an update based on newly updated information
	 * from the opposite end of the connection (real life would reflect an update
	 * based on the old peer information).
	 *
	 * @param con
	 * @param peer
	 */
	@Override
	public void doExchangeForNewConnection(Connection con, DTNHost peer) {

	}

	/**
	 * Allows the decision engine to gather information from the given message and
	 * determine if it should be forwarded on or discarded. This method is only
	 * called when a message originates at the current host (not when received
	 * from a peer). In this way, applications can use a Message to communicate
	 * information to this routing layer.
	 *
	 * @param m the new Message to consider routing
	 * @return True if the message should be forwarded on. False if the message
	 * should be discarded.
	 */
	@Override
	public boolean newMessage(Message m) {
		m.addProperty(MSG_COUNT_PROP, initialNrofCopies);
		return true;
	}

	/**
	 * Determines if the given host is an intended recipient of the given Message.
	 * This method is expected to be called when a new Message is received at a
	 * given router.
	 *
	 * @param m     Message just received
	 * @param aHost Host to check
	 * @return true if the given host is a recipient of this given message. False
	 * otherwise.
	 */
	@Override
	public boolean isFinalDest(Message m, DTNHost aHost) {
		Integer nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROP);
		nrofCopies = (int)Math.ceil(nrofCopies/2.0);
		m.updateProperty(MSG_COUNT_PROP, nrofCopies);

		return m.getTo() == aHost;
	}

	/**
	 * Called to determine if a new message received from a peer should be saved
	 * to the host's message store and further forwarded on.
	 *
	 * @param m        Message just received
	 * @param thisHost The requesting host
	 * @return true if the message should be saved and further routed.
	 * False otherwise.
	 */
	@Override
	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
		return m.getTo() != thisHost;
	}

	/**
	 * Called to determine if the given Message should be sent to the given host.
	 * This method will often be called multiple times in succession as the
	 * DecisionEngineRouter loops through its respective Message or Connection
	 * Collections.
	 *
	 * @param m         Message to possibly sent
	 * @param otherHost peer to potentially send the message to.
	 * @return true if the message should be sent. False otherwise.
	 */
	@Override
	public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
		if(m.getTo() == otherHost) return true;

		Integer nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROP);
		if(nrofCopies > 1) return true;

		DTNHost dest = m.getTo();

		SnWDecisionEngine de = getOtherDecisionEngine(otherHost);
		assert nrofCopies != null : "Not a SnW message: " + m;

		if (isBinary) {
			/* in binary S'n'W the receiving node gets ceil(n/2) copies */
			nrofCopies = (int)Math.ceil(nrofCopies/2.0);
		}
		else {
			/* in standard S'n'W the receiving node gets only single copy */
			nrofCopies = 1;
		}

		m.updateProperty(MSG_COUNT_PROP, nrofCopies);

		return false;
	}

	/**
	 * Called after a message is sent to some other peer to ask if it should now
	 * be deleted from the message store.
	 *
	 * @param m         Sent message
	 * @param otherHost Host who received the message
	 * @return true if the message should be deleted. False otherwise.
	 */
	@Override
	public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
		int nrofCopies;
		nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROP);
		if(nrofCopies > 1) nrofCopies /= 2;
		else return true;
		m.updateProperty(MSG_COUNT_PROP, nrofCopies);

		return false;
	}

	/**
	 * Called if an attempt was unsuccessfully made to transfer a message to a
	 * peer and the return code indicates the message is old or already delivered,
	 * in which case it might be appropriate to delete the message.
	 *
	 * @param m                Old Message
	 * @param hostReportingOld Peer claiming the message is old
	 * @return true if the message should be deleted. False otherwise.
	 */
	@Override
	public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
		return m.getTo() == hostReportingOld;
	}

	private SnWDecisionEngine getOtherDecisionEngine(DTNHost h)
	{
		MessageRouter otherRouter = h.getRouter();
		assert otherRouter instanceof DecisionEngineRouter : "This router only works " +
				" with other routers of same type";

		return (SnWDecisionEngine) ((DecisionEngineRouter)otherRouter).getDecisionEngine();
	}

	/**
	 * Duplicates this decision engine.
	 *
	 * @return
	 */
	@Override
	public RoutingDecisionEngine replicate() {
		return new SnWDecisionEngine(this);
	}
}
