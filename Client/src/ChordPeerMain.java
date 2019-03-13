import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 *
 * The class is a main program for Peers in chord. The class initially
 * requests calls the Bootstrap to join. The Bootstrap responses back with
 * a random zone number and an entrypoint IP. If the
 * peer is the first peer to the network, the Bootstrap makes the peer
 * as the entry point.
 *
 */
public class ChordPeerMain {

	private static final int messageSize = 64;

	public static final int n = 128;
	public static final int m = 7;
	public static String serverIP;

	private int randomNumberGenerated;

	private String entryPoint;

	private String currentIP;

	private byte recvByte[] = new byte[messageSize];

	static boolean isEntryPoint = false;

	// Main fuction
	public static void main(String args[]) {
		ChordPeerMain chordPeerMain = new ChordPeerMain();

		serverIP = args[0];

		chordPeerMain.chordInitialize(serverIP);

		ChordNode chordNode = new ChordNode(chordPeerMain.randomNumberGenerated, chordPeerMain.entryPoint, chordPeerMain.currentIP);

		chordNode.run();
		
	}

	/**
	 * Asks the bootstrap to join the chord network
	 * @param serverIP IP of the bootstrap
     */
	private void chordInitialize(String serverIP) {

		try {
			Socket socket = new Socket(serverIP, 8880);
			currentIP = socket.getLocalSocketAddress().toString().replaceAll("^/+", "").split(":")[0];
			DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

			dataInputStream.read(recvByte, 0, recvByte.length);

			String message = new String(Arrays.copyOfRange(recvByte, 0, messageSize)).trim(); //input from peer ie upload file, download file etc.

			String messageArray[] = message.split(" ");

			randomNumberGenerated = Integer.parseInt(messageArray[0]);

			if (messageArray[1].equals("isEntryPoint")) {
				isEntryPoint = true;
				entryPoint = "";
			} else {
				entryPoint = messageArray[1];
			}

			socket.close();

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
