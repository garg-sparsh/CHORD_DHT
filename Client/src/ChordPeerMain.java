import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;


// Main class that starts the peer.It initially requests calls to the ChordMain Server to join.
// The ChordMain responds back with a random zone number and an entry point IP. If the
// peer is the first peer to the network, the ChordMain makes the peer as the entry point.

public class ChordPeerMain {


	public static String serverIP;
	private int nodeVal;
	private String entryPoint;
	private String currentIP;
	private byte recvByte[] = new byte[Constant.MESSAGE_SIZE];
	static boolean isEntryPoint = false;

	// Main fuction
	public static void main(String args[]) {
		ChordPeerMain chordPeerMain = new ChordPeerMain();

		serverIP = args[0];

		chordPeerMain.chordInitialize(serverIP);
		ChordNode chordNode = new ChordNode(chordPeerMain.nodeVal, chordPeerMain.entryPoint, chordPeerMain.currentIP);
		chordNode.run();
		
	}
	// Asks the ChordMain Server to join the chord network
	private void chordInitialize(String serverIP) {

		try {
			Socket socket = new Socket(serverIP, Constant.SERVER_PORT);
			currentIP = socket.getLocalSocketAddress().toString().replaceAll("^/+", "").split(":")[0];
			DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

			dataInputStream.read(recvByte, 0, recvByte.length);

			String message = new String(Arrays.copyOfRange(recvByte, 0, Constant.MESSAGE_SIZE)).trim(); //input from peer ie upload file, download file etc.

			String messageArray[] = message.split(" ");

			nodeVal = Integer.parseInt(messageArray[0]);

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
