package kootoueg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;

public class Main {

	// Server thread that listens to this node's port
	public static Server server;

	// Total number of nodes in the network connected to this node
	public static Integer noNodes;

	// The Node object of this node - contains port number and host name
	public static Node myNode;

	// Map of all nodes with their identifiers as keys
	public static HashMap<Integer, Node> nodeMap = new HashMap<>();

	// Client object for this node
	public static Client client;

	// Ordered by 1.VectorClock 2.FirstLabelSent 3.LastLabelReceived
	// 4.LastLabelSent
	public static Integer[][] vectors;

	/*
	 * Main.vectors[0] = VECTOR_CLOCK Main.vectors[1] = FIRST_LABEL_SENT
	 * Main.vectors[2] = LAST_LABEL_RECEIVED Main.vectors[3] = LAST_LABEL_SENT
	 */
	public static enum VectorType {
		VECTOR_CLOCK, FIRST_LABEL_SENT, LAST_LABEL_RECEIVED, LAST_LABEL_SENT
	}

	public static enum EventType {
		SEND_MSG, RECEIVE_MSG, CHECKPOINT, RECOVERY
	}

	public static Boolean isFinalRun = false;

	public static Integer checkpointSequenceNumber = 0;

	public static List<Checkpoint> checkpointsTaken = new ArrayList<>();

	public static Integer instanceDelay = 0, sendDelay = 0, msgCount = 0, totalNoOfMsgs = 0;

	// Sequence given in the Config file
	public static List<EventSequence> checkpointRecoverySequence = new ArrayList<>();

	public static boolean checkpointingInProgress = false;

	public static Checkpoint temporaryCheckpoint = null;

	public static HashMap<Integer, Boolean> confirmationsPending = new HashMap<>();

	public static Integer recoveryInitiator = null;

	public static void main(String[] args) throws IOException {
		try {
			ConfigParser parser = new ConfigParser(args[0]);
			parser.parseFile();
			Utils.setupVectors();
			server = new Server(myNode.getHostName(), myNode.getPortNo());
			server.start();
			Thread.sleep(7000);
			Client.sendMessage();
			checkpointsTaken.add(new Checkpoint(Main.checkpointSequenceNumber, Main.vectors));
			initiateCheckpointOrRecoveryIfMyTurn();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void initiateCheckpointOrRecoveryIfMyTurn() {
		if (Main.checkpointRecoverySequence.size() > 0
				&& Main.checkpointRecoverySequence.get(0).nodeId.equals(Main.myNode.getId())) {
			Utils.log("My Checkpointing turn: ");
			Timer timer = new Timer();
			timer.schedule(new java.util.TimerTask() {
				@Override
				public void run() {
					if (Main.checkpointRecoverySequence.size() > 0
							&& Main.checkpointRecoverySequence.get(0).nodeId.equals(Main.myNode.getId())) {
						if (Main.checkpointRecoverySequence.get(0).type == EventType.CHECKPOINT) {
							Utils.log("Initiating checkpointing protocol");
							CheckpointingUtils.initiateCheckpointProtocol();
						} else {
							Utils.log("Initiating recovery protocol");
							RecoveryUtils.initiateRecoveryProtocol();
						}
					}
				}
			}, Utils.getExponentialDistributedValue(Main.instanceDelay));

		}
	}

	/*
	 * Kills the server thread and closes server socket
	 */
	@SuppressWarnings("deprecation")
	public static void killServer() {
		server.stop();
		server.destroy();
	}

}
