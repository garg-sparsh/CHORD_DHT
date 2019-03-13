import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;


public class ChordNode {

	private static final int messageSize = 64;

	private static int nodeStart;
	private static int nodeEnd;

	private static Neighbour predecessor;
	private static Neighbour successor;
	private File file;
	private static String filePath;

	private static String myIP;

	private byte sendData[] = new byte[messageSize];
	private byte recvData[] = new byte[messageSize];
	private byte recvBiggerData[] = new byte[1024];

	static KeySpace checkKeySpace;
	static KeySpaceManager KeySpaceManager;
	static NodeFT NodeFT;
	static NeighbourUpdate neighbourUpdate;
	static NodeDesc NodeDesc;
	static NodeFTUpdate NodeFTUpdate;
	static FileDownloadListener nodeFileDownload;
	static TransferListener peerFileTransmitter;
	static Boolean isEntryPoint = false;
	static List<String> fileNames = new ArrayList<String>();

	// getter & setter

	public static int getNodeStart() {
		return nodeStart;
	}

	public static int getNodeEnd() {
		return nodeEnd;
	}

	public static void setMyEndpoint(int srt, int end) {
		nodeStart = srt;
		nodeEnd = end;
	}

	public static Neighbour getPredecessor() {
		return predecessor;
	}

	public static Neighbour getSuccessor() {
		return successor;
	}

	public static void setPredecessor(String predecessorIP, int zoneSrt, int zoneEnd) {
		predecessor.updateZone(predecessorIP, zoneSrt, zoneEnd);
	}

	public static void setSuccessor(String successorIP, int zoneSrt, int zoneEnd) {
		successor.updateZone(successorIP, zoneSrt, zoneEnd);
	}

	public static String getMyIP() {
		return myIP;
	}


	public ChordNode() {

	}

	public void addFiles(String name) {
		fileNames.add(name);
	}

	public ChordNode(int randomNumber, String entryPoint, String currentIP) {
		System.out.println("Node number and IP: "+ randomNumber + " " + currentIP + "\n");
		myIP = currentIP;
		isEntryPoint = ChordPeerMain.isEntryPoint;

		if (ChordPeerMain.isEntryPoint) {
			nodeStart = 0;
			nodeEnd = 127;
			predecessor = new Neighbour(myIP, nodeStart, nodeEnd);
			successor = new Neighbour(myIP, nodeStart, nodeEnd);
		} else {

			keySpaceRequest(randomNumber, entryPoint);
		}

		//Initiates a ChordNode's listeners and transmitters.
		checkKeySpace = new KeySpace();
		checkKeySpace.start();
		KeySpaceManager = new KeySpaceManager();
		KeySpaceManager.start();
		NodeFT = new NodeFT();
		NodeFT.runFT();
		neighbourUpdate = new NeighbourUpdate();
		neighbourUpdate.start();
		NodeDesc = new NodeDesc();
		NodeDesc.start();
		NodeFTUpdate = new NodeFTUpdate();
		NodeFTUpdate.start();
		nodeFileDownload = new FileDownloadListener();
		nodeFileDownload.start();
		peerFileTransmitter = new TransferListener();
		peerFileTransmitter.start();
	}

	public void run() {

		Scanner scanner = new Scanner(System.in);

		boolean isNodeRunning = true;

		while (isNodeRunning) {

			//Print options menu on command line
			printOptionsMenu();

			try{
				int switchInt = scanner.nextInt();
				switch (switchInt) {

					case 1://Print node details including the list of finger tables
						getDetails();
						break;
					case 2://Option to leave chord zone
						if (myIP.equals(predecessor.getIP()) && myIP.equals(successor.getIP())) {
							System.out.println("You are the only peer.");
							System.out.println("At least one peer needed to maintain the chord!!!");
							break;
						}
						if (!leaveChord()) {
							System.out.println("Leave Zone failed. Try again.");
						} else {
							System.out.println("Leave Zone success. Node shutdown complete.");
							isNodeRunning = false;
						}
						break;
					case 3://Option to upload file to chord zone
						System.out.println("Enter the file name to upload: ");
						scanner = new Scanner(System.in);
						filePath = scanner.nextLine();
						NodeKeyManager peerFileUpload = new NodeKeyManager();
						peerFileUpload.uploadFile(filePath, false);
						break;
					case 4://Option to download file from chord zone
						System.out.print("Enter the file name for download: ");
						scanner = new Scanner(System.in);
						filePath = scanner.nextLine();
						filePath = new File(filePath).getName();
						NodeKeyManager nodeFileDownload = new NodeKeyManager();
						try {
							nodeFileDownload.downloadFile(filePath);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					case 5://Print the list of files stored at this peer.
						System.out.println("Files stored at this Peer :");
						System.out.println(fileNames);
						break;

					default:
						System.out.println("Enter the correct number");

				}
			}
			catch (InputMismatchException ime){
				System.out.println("Invalid input");
				printOptionsMenu();
			}

		}

		scanner.close();

	}

	public static void printOptionsMenu()
	{
		System.out.println("Choose an option:");
		System.out.println("1 - Finger Table Details ");
		System.out.println("2 - Leave chord ");
		System.out.println("3 - Upload File ");
		System.out.println("4 - Download File ");
		System.out.println("5 - Files present at this node ");

		System.out.print("Your input - ");
	}

	private void getDetails() {

		int totalIter = ChordPeerMain.n;
		int zoneChecking = nodeStart;

		while (zoneChecking != nodeEnd) {
			totalIter--;
			zoneChecking++;

		}

		System.out.println(myIP + " Zone details:" + nodeStart + "-" + nodeEnd);
		System.out.println(
				"Predecessor:" + predecessor.getIP() + " " + predecessor.getNodeStart() + " " + predecessor.getNodeEnd());
		System.out.println(
				"Successor:" + successor.getIP() + " " + successor.getNodeStart() + " " + successor.getNodeEnd());

		System.out.println("Finger Table:");
		for (int i = 0; i < ChordPeerMain.m; i++) {
			System.out.println(i + " " + NodeFT.getPeerIP(i));
		}

		System.out.println("Is EntryPoint:" + isEntryPoint);

		System.out.println("---------------------------------");

		totalIter--;
		zoneChecking++;
		if (zoneChecking >= ChordPeerMain.n) {
			zoneChecking = 0;
		}

		while (totalIter > 0) {

			String peerIP = getKeySpaceIP(zoneChecking, nearestNode(zoneChecking));

			String message[] = getNodeDesc(peerIP);
			String zone[] = message[0].split(" ");
			String lookUP[] = message[3].split(" ");
			int endZone = Integer.parseInt(zone[1]);

			System.out.println(peerIP + " Zone details:" + zone[0] + "-" + zone[1]);
			System.out.println("Predecessor:" + message[1]);
			System.out.println("Successor:" + message[2]);

			System.out.println("Finger Table:");
			for (int i = 0; i < ChordPeerMain.m; i++) {
				System.out.println(i + " " + lookUP[i]);
			}

			System.out.println("Is EntryPoint:" + message[4]);

			System.out.println("---------------------------------");

			totalIter--;
			zoneChecking++;
			if (zoneChecking >= ChordPeerMain.n) {
				zoneChecking = 0;
			}

			while (zoneChecking <= endZone) {
				totalIter--;
				zoneChecking++;
			}

			if (zoneChecking >= ChordPeerMain.n) {
				zoneChecking = 0;
			}

		}

		System.out.println("+++---------------------------+++");

	}

	public boolean isFileOwner(String fileName) {
		return fileNames.contains(fileName);
	}

	private void keySpaceRequest(int randomNumber, String requestIP) {

		requestIP = getKeySpaceIP(randomNumber, requestIP);
		getKeySpace(requestIP);

	}

	public int hash(String fileName) {
		byte[] filePathBytes = fileName.getBytes();
		Checksum value = new CRC32();
		value.update(filePathBytes, 0, filePathBytes.length);
		int hash = (int) value.getValue() % 127;
		if (hash < 0)
			hash = hash + 127;
		return hash;
	}

	private boolean leaveChord() {

		Socket socket;

		try {
			if (ChordNode.isEntryPoint) {
				socket = new Socket(predecessor.getIP(), 9991);

			} else {
				socket = new Socket(successor.getIP(), 9991);
			}

			DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
			DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

			MakeMessage makeMessage = new MakeMessage();
			sendData = makeMessage.message_creation(sendData, messageSize, "leave");

			dataOutputStream.write(sendData);
			dataOutputStream.flush();

			dataInputStream.read(recvData, 0, recvData.length);

			String response = new String(recvData).trim();
			if (response.contains("Confirmed")) {
				shareFilesToNeighbours();
				//New
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				stopAllNodeListeners();
				return true;
			}

			dataInputStream.close();
			dataOutputStream.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}

	private void stopAllNodeListeners() {

		checkKeySpace.stopServer();
		KeySpaceManager.stopServer();
		neighbourUpdate.stopServer();
		NodeDesc.stopServer();
		NodeFTUpdate.stopServer();
		peerFileTransmitter.stopServer();
		nodeFileDownload.stopServer();
	}

	private void shareFilesToNeighbours() {
		ChordNode.setMyEndpoint(-1, -1);
		NodeKeyManager fileManager = new NodeKeyManager();

		for(String fileName : ChordNode.fileNames)
		{
			fileManager.uploadFile_new_peer(fileName, true);
		}
	}

	private void getKeySpace(String requestIP) {

		try {

			Socket socket = new Socket(requestIP, 9991);

			DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
			DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

			MakeMessage makeMessage = new MakeMessage();
			sendData = makeMessage.message_creation(sendData, messageSize, "add");

			dataOutputStream.write(sendData);
			dataOutputStream.flush();

			dataInputStream.read(recvData, 0, recvData.length);

			String initDetails[] = new String(recvData).trim().split(" ");

			nodeStart = Integer.parseInt(initDetails[0]);
			nodeEnd = Integer.parseInt(initDetails[1]);

			predecessor = new Neighbour(initDetails[2], Integer.parseInt(initDetails[3]),
					Integer.parseInt(initDetails[4]));
			successor = new Neighbour(initDetails[5], Integer.parseInt(initDetails[6]),
					Integer.parseInt(initDetails[7]));

			socket.close();

			if (!predecessor.getIP().contains(requestIP)) {
				sendNeighbourUpdate(predecessor.getIP(), 1);
			}
			if (!successor.getIP().contains(requestIP)) {
				//System.out.println("Updating neighbour successor " + successor + " " + requestIP);
				sendNeighbourUpdate(successor.getIP(), 0);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getKeySpaceIP(int zoneNumber, String requestIP) {


		boolean isPeerZone = false;

		while (!isPeerZone) {
			try {

				//System.out.println("Request IP for Zone: " + zoneNumber + " to IP: " + requestIP);
				Socket socket = new Socket(requestIP, 9990);

				DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
				DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
				MakeMessage makeMessage = new MakeMessage();
				sendData = makeMessage.message_creation(sendData, messageSize, zoneNumber + "");

				dataOutputStream.write(sendData);
				dataOutputStream.flush();

				dataInputStream.read(recvData);

				String message = new String(recvData).trim();

				if (message.contains("isMyZone")) {
					isPeerZone = true;
				} else {
					requestIP = message;
				}

				socket.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return requestIP;

	}

	public void sendNeighbourUpdate(String IP, int value) {

		try {

			Socket socket = new Socket(IP, 9992);

			DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

			if (value == 0) {
				MakeMessage makeMessage = new MakeMessage();
				sendData = makeMessage.message_creation(sendData, messageSize, "predecessor" + " " + nodeStart + " " + nodeEnd);
			} else {
				MakeMessage makeMessage = new MakeMessage();
				sendData = makeMessage.message_creation(sendData, messageSize, "successor" + " " + nodeStart + " " + nodeEnd);
			}

			dataOutputStream.write(sendData);
			dataOutputStream.flush();

			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public String[] getNodeDesc(String IP) {

		String message[] = null;

		try {

			Socket socket = new Socket(IP, 9993);

			DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

			dataInputStream.read(recvBiggerData);

			message = new String(recvBiggerData).trim().split("\n");

			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return message;

	}

	public String nearestNode(int zone) {

		int nearestZone = nodeEnd + ChordNode.NodeFT.getZone(0);
		String nearestIP = ChordNode.NodeFT.getPeerIP(0);;
		int nearestSub = zone - nearestZone;

		int tempNearestZone;
		String tempNearestIP;

		for (int i = 1; i< ChordPeerMain.m; i++){
			tempNearestZone = nodeEnd + ChordNode.NodeFT.getZone(i);
			if (tempNearestZone >= ChordPeerMain.n) {
				tempNearestZone = tempNearestZone % ChordPeerMain.n;
			}
			tempNearestIP = ChordNode.NodeFT.getPeerIP(i);
			int tempSub = zone - tempNearestZone;

			if (tempSub < 0) {
				tempSub = ChordPeerMain.n + tempSub;
			}
			if (tempSub < nearestSub && tempSub >= 0) {
				nearestIP = tempNearestIP;
				nearestSub = tempSub;
			}


		}

		return nearestIP;

	}

	public boolean isInMyKeySpace(int zone)
	{
		return (zone <= ChordNode.getNodeEnd() && zone >= ChordNode.getNodeStart());
	}


}
