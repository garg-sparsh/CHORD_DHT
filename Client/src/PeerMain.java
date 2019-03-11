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
 * @author Srinath Kanna, Krishna Prasad, Ajeeth Kannan
 */
public class PeerMain {

	private static final int messageSize = 64;

	public static final int n = 128;
	public static final int m = 7;
	public static String serverIP;

	private int randomNumberGenerated;

	private String entryPoint;

	private byte recvByte[] = new byte[messageSize];

	static boolean isEntryPoint = false;

	// Main fuction
	public static void main(String args[]) {
		PeerMain peerMain = new PeerMain();

		serverIP = args[0];
		System.out.println("serverIP:"+serverIP);
		peerMain.initialSetUp(serverIP);

		PeerNode peerNode = new PeerNode(peerMain.randomNumberGenerated, peerMain.entryPoint);

		peerNode.run();
		
	}

	/**
	 * Asks the bootstrap to join the chord network
	 * @param serverIP IP of the bootstrap
     */
	private void initialSetUp(String serverIP) {

		try {
			Socket socket = new Socket(serverIP, 8880);

			DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

			dataInputStream.read(recvByte, 0, recvByte.length);

			String message = new String(Arrays.copyOfRange(recvByte, 0, messageSize)).trim(); //input from peer ie upload file, download file etc.
			System.out.println("message:"+message);
			String messageArray[] = message.split(" ");

			System.out.println("messageArray[]:"+messageArray[0]+" "+messageArray[1]);

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
