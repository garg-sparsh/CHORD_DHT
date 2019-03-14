import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

// All the properties of Node are set in this class
// This class initially assigns a keyspace for each node and
// based on that sets its predecessor and successor value and
// then opens all the sockets necessary to perform all the other operations.
public class ChordNode {

	private byte sendData[] = new byte[Constant.MESSAGE_SIZE];
	private byte recvData[] = new byte[Constant.MESSAGE_SIZE];
	private byte recvBiggerData[] = new byte[Constant.FILE_MSG_SIZE];

	private static int nodeStart;
	private static int nodeEnd;
	private static Neighbour predecessor;
	private static Neighbour successor;
	private static String myIP;
	static Boolean isEntryPoint = false;

	static KeySpace checkKeySpace;
	static KeySpaceManager KeySpaceManager;
	static NodeFT NodeFT;
	static NeighbourUpdate neighbourUpdate;
	static NodeDesc NodeDesc;
	static NodeFTUpdate NodeFTUpdate;
	static FileDownloadListener nodeFileDownload;
	static TransferListener peerFileTransmitter;
	static List<String> fileNames = new ArrayList<String>();

	public ChordNode() {

	}


	//Assigns each node a keyspace and predecessor and successor value.
	// If its entry point it is assigned a key space as 0-127
	// otherwise it requests for one based on where it actually lies in the chord.
	// Along with that it initiates all the listeners and transmitters for a node.

	public ChordNode(int randomNumber, String entryPoint, String currentIP) {
		System.out.println("Node number and IP: "+ randomNumber + " " + currentIP + "\n");
		myIP = currentIP;
		isEntryPoint = ChordPeerMain.isEntryPoint;

		if (ChordPeerMain.isEntryPoint) {
			nodeStart = 0;
			nodeEnd = Constant.N-1;
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
		String filePath;
		boolean isNodeRunning = true;

		while (isNodeRunning) {

			//Print options menu on command line
			menu();

			try{
				int switchInt = scanner.nextInt();
				switch (switchInt) {

					case 1://Print node details including the list of finger tables
						getDetails();
						break;

					case 2://Option to upload file to chord key space
						System.out.println("Enter the file name to upload: ");
						scanner = new Scanner(System.in);
						filePath = scanner.nextLine();
						NodeKeyManager peerFileUpload = new NodeKeyManager();
						peerFileUpload.uploadFile(filePath, false);
						break;
					case 3://Option to download file from chord key space
						System.out.print("Enter the file name for download: ");
						scanner = new Scanner(System.in);
						filePath = scanner.nextLine();
						filePath = new File(filePath).getName();
						NodeKeyManager nodeFileDownload = new NodeKeyManager();
						try {
							nodeFileDownload.downloadFile(filePath);
						} catch (IOException e) {
							e.printStackTrace();
						}
						break;
					case 4://Print the list of files stored at this peer.
						System.out.println("Files stored at this Peer :");
						System.out.println(fileNames);
						break;

					case 5://Option to leave chord key space
						if (myIP.equals(predecessor.getIP()) && myIP.equals(successor.getIP())) {
							System.out.println("You are the only peer.");
							System.out.println("At least one peer needed to maintain the chord!!!");
							break;
						}
						if (!leaveChord()) {
							System.out.println("Leave chord ring failed. Try again.");
						} else {
							System.out.println("Leave chord ring success. Node shutdown complete.");
							isNodeRunning = false;
						}
						break;

					default:
						System.out.println("Invalid option. Select from the given options.");

				}
			}
			catch (InputMismatchException ime){
				System.out.println("Invalid option. Select from the given options");
				menu();
			}

		}

		scanner.close();

	}
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

	public static void setPredecessor(String predecessorIP, int start, int end) {
		predecessor.updateKeySpace(predecessorIP, start, end);
	}

	public static void setSuccessor(String successorIP, int start, int end) {
		successor.updateKeySpace(successorIP, start, end);
	}

	public static String getMyIP() {
		return myIP;
	}

	public boolean isInMyKeySpace(int keyValue)
	{
		return (keyValue <= ChordNode.getNodeEnd() && keyValue >= ChordNode.getNodeStart());
	}


	//Add files to the list that each node hold
	public void addFiles(String name) {
		fileNames.add(name);
	}

	//prints all the available actions that can be performed
	public static void menu()
	{
		System.out.println("Choose an option:");
		System.out.println("1 - Finger Table Details ");
		System.out.println("2 - Upload File ");
		System.out.println("3 - Download File ");
		System.out.println("4 - Files present at this node ");
		System.out.println("5 - Leave chord ");
		System.out.println("Select option:");
	}

	// Prints the node details-IP ,keyspace,its predecessor and successor IP, if its an entry point
	// followed by finger table details of the node
	private void getDetails() {

		int n=Constant.N;
		int keySpaceStart = nodeStart;

		while (keySpaceStart != nodeEnd) {
			n--;
			keySpaceStart++;

		}

		System.out.println("IP:"+myIP );
		System.out.println("KeySpace:" + nodeStart + "-" + nodeEnd);
		System.out.println("Predecessor:" + predecessor.getIP());
		System.out.println("Successor:" + successor.getIP());
		if(isEntryPoint)
			System.out.println("I am the main ENTRY POINT");

		System.out.println("---------------------------------");
		System.out.println("Finger Table:");
		for (int i = 0; i < Constant.M; i++) {
			System.out.println(i + " " + NodeFT.getPeerIP(i));
		}

		System.out.println("---------------------------------");
	}

	// Checks if the current node is the owner of the file
	public boolean isFileOwner(String fileName) {
		return fileNames.contains(fileName);
	}


	// If the node is not an entry point this function is called to request for its exact location
	//	in the chord and get its respective keyspace, predecessor and successor value
	private void keySpaceRequest(int randomNumber, String requestIP) {

		requestIP = getKeySpaceIP(randomNumber, requestIP);
		getKeySpace(requestIP);

	}

	//A hash function that hashes the value of the file name to get the key
	//value at which the file needs to be stored or retrieved from
	public int hash(String fileName) {
		byte[] filePathBytes = fileName.getBytes();
		Checksum value = new CRC32();
		value.update(filePathBytes, 0, filePathBytes.length);
		int hash = (int) value.getValue() % (Constant.N-1);
		if (hash < 0)
			hash = hash + (Constant.N-1);
		return hash;
	}

	//function called when a node leaves the chord and it needs to notify its predecessor if its entry point gets updated
	//else successor is notified about it
	private boolean leaveChord() {

		Socket socket;
		try {
			if (ChordNode.isEntryPoint) {
				socket = new Socket(predecessor.getIP(), Constant.KEY_SPACE_MANAGER_PORT);

			} else {
				socket = new Socket(successor.getIP(), Constant.KEY_SPACE_MANAGER_PORT);
			}

			DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
			DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

			MakeMessage makeMessage = new MakeMessage();
			sendData = makeMessage.message_creation(sendData, Constant.MESSAGE_SIZE, "leave");

			dataOutputStream.write(sendData);
			dataOutputStream.flush();

			dataInputStream.read(recvData, 0, recvData.length);

			String response = new String(recvData).trim();
			//once successor or predecessor updates the keyspace and finger table respectively
			// and copies all the files from the current node only then the node can leave the system.
			if (response.contains("Confirmed")) {
				shareFilesToNeighbours();

				try {
					Thread.sleep(Constant.THREAD_SLEEP_TIME);
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
	//after leaving the node all other nodes should stop listening to it
	private void stopAllNodeListeners() {

		checkKeySpace.stopServer();
		KeySpaceManager.stopServer();
		neighbourUpdate.stopServer();
		NodeDesc.stopServer();
		NodeFTUpdate.stopServer();
		peerFileTransmitter.stopServer();
		nodeFileDownload.stopServer();
	}

	//Called when node leaves the chord to transfer all the files at its location to its neighbour
	private void shareFilesToNeighbours() {
		ChordNode.setMyEndpoint(-1, -1);
		NodeKeyManager fileManager = new NodeKeyManager();

		for(String fileName : ChordNode.fileNames)
		{
			fileManager.uploadFile_new_peer(fileName, true);
		}
	}
	//sends the request to get key space start and end based on the IP provided
	private void getKeySpace(String requestIP) {

		try {

			Socket socket = new Socket(requestIP, Constant.KEY_SPACE_MANAGER_PORT);

			DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
			DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

			MakeMessage makeMessage = new MakeMessage();
			sendData = makeMessage.message_creation(sendData, Constant.MESSAGE_SIZE, "add");

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
				sendNeighbourUpdate(successor.getIP(), 0);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//sends the request to get the ip of the node in which the key of the file name resides in the key space

	public String getKeySpaceIP(int nodeNo, String requestIP) {


		boolean isPeerKeySpace = false;

		while (!isPeerKeySpace) {
			try {

				Socket socket = new Socket(requestIP, Constant.CONNECT_NODE_PORT);

				DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
				DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

				MakeMessage makeMessage = new MakeMessage();
				sendData = makeMessage.message_creation(sendData, Constant.MESSAGE_SIZE, nodeNo + "");

				dataOutputStream.write(sendData);
				dataOutputStream.flush();

				dataInputStream.read(recvData);

				String message = new String(recvData).trim();

				if (message.contains("isInMySpace")) {
					isPeerKeySpace = true;
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

	//when node leaves/joins function is called to update the neighbours.
	public void sendNeighbourUpdate(String IP, int value) {

		try {

			Socket socket = new Socket(IP, Constant.KEY_SPACE_lISTENER_PORT);

			DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

			if (value == 0) {
				MakeMessage makeMessage = new MakeMessage();
				sendData = makeMessage.message_creation(sendData, Constant.MESSAGE_SIZE, "predecessor" + " " + nodeStart + " " + nodeEnd);
			} else {
				MakeMessage makeMessage = new MakeMessage();
				sendData = makeMessage.message_creation(sendData, Constant.MESSAGE_SIZE, "successor" + " " + nodeStart + " " + nodeEnd);
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

			Socket socket = new Socket(IP, Constant.NEIGHBOUR_lISTENER);

			DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
			dataInputStream.read(recvBiggerData);
			message = new String(recvBiggerData).trim().split("\n");
			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return message;

	}

	//get nearest node based on the finger table details.
	//if node not found in FT check predecessor or successor.
	//If key value not present in key space of predecessor successor send successor details to check finger table
	public String nearestNode(int keyValue) {

		int nearestNode = nodeEnd + ChordNode.NodeFT.getKeySpace(0);
		String nearestIP = ChordNode.NodeFT.getPeerIP(0);;
		int IPDiff = keyValue - nearestNode;

		int tempSpace;
		String tempIP;

		for (int i = 1; i< Constant.M; i++){
			tempSpace = nodeEnd + ChordNode.NodeFT.getKeySpace(i);
			if (tempSpace >= Constant.N) {
				tempSpace = tempSpace % Constant.N;
			}
			tempIP = ChordNode.NodeFT.getPeerIP(i);
			int tempDiff = keyValue - tempSpace;

			if (tempDiff < 0) {
				tempDiff = Constant.N + tempDiff;
			}
			if (tempDiff < IPDiff && tempDiff >= 0) {
				nearestIP = tempIP;
				IPDiff = tempDiff;
			}


		}
		return nearestIP;

	}




}
