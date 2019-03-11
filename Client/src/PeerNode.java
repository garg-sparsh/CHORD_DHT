import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.zip.CRC32;
import java.util.zip.Checksum;


/***
 * Class PeerNode is the main program for each peer which initiates
 * several listeners and transmitters needed to maintain zones in Chord.
 */
public class PeerNode {

	private static final int messageSize = 64;

	private static int myZoneSrt;
	private static int myZoneEnd;

	private static PeerNeighbour predecessor;
	private static PeerNeighbour successor;
	private File file;
	private static String filePath;

	private static String myIP;

	private byte sendData[] = new byte[messageSize];
	private byte recvData[] = new byte[messageSize];
	private byte recvBiggerData[] = new byte[1024];

	static test peerCheckZone;
	static PeerZoneManager peerZoneManager;
	static PeerLookUP peerLookUP;
	static PeerNeighbourUpdate peerNeighbourUpdate;
	static PeerDetails peerDetails;
	static PeerLookUpUpdate peerLookUpUpdate;
	static PeerFileDownloadListener peerFileDownload;
	static PeerTransmitterListener peerFileUpload;
	static Boolean isEntryPoint = false;
	static List<String> fileNames = new ArrayList<String>();

	// getter & setter

	public static int getMyZoneSrt() {
		return myZoneSrt;
	}

	public static int getMyZoneEnd() {
		return myZoneEnd;
	}

	public static void setMyZone(int srt, int end) {
		myZoneSrt = srt;
		myZoneEnd = end;
	}

	/**
	 * GetPredecessor method returns the predecessor of this method.
	 * @return PeerNeighbour class object of the predecessor
     */
	public static PeerNeighbour getPredecessor() {
		return predecessor;
	}

	/**
	 * GetSuccessor method returns the successor of this method.
	 * @return PeerNeighbour class object of the successor
	 */
	public static PeerNeighbour getSuccessor() {
		return successor;
	}

	/**
	 * setPredecessor method sets this node's predecessor with the given Ip, zone start and zone end values.
	 * @param predecessorIP
	 * @param zoneSrt
	 * @param zoneEnd
     */
	public static void setPredecessor(String predecessorIP, int zoneSrt, int zoneEnd) {
		predecessor.updateZone(predecessorIP, zoneSrt, zoneEnd);
	}

	/***
	 * setSuccessor method sets this node's successor with the given Ip, zone start and zone end values.
	 * @param successorIP
	 * @param zoneSrt
	 * @param zoneEnd
     */
	public static void setSuccessor(String successorIP, int zoneSrt, int zoneEnd) {
		successor.updateZone(successorIP, zoneSrt, zoneEnd);
	}

	/***
	 * getMyIP returns this PeerNode's IP address
	 * @return
     */
	public static String getMyIP() {
		return myIP;
	}


	public PeerNode() {

	}

	/***
	 * addFilesNames method adds the give file name to the list of files
	 * stored at this node.
	 * @param name
     */
	public void addfileNames(String name) {
		fileNames.add(name);
	}


	/***
	 * Constructor for the PeerNode which takes a randomNumber and an entryPoint value
	 * @param randomNumber
	 * @param entryPoint
     */
	public PeerNode(int randomNumber, String entryPoint) {
		System.out.println("entryPoint:"+entryPoint+" "+randomNumber);

		isEntryPoint = PeerMain.isEntryPoint;

		MyIP();

		if (PeerMain.isEntryPoint) {
			myZoneSrt = 0;
			myZoneEnd = 127;
			predecessor = new PeerNeighbour(myIP, myZoneSrt, myZoneEnd);
			successor = new PeerNeighbour(myIP, myZoneSrt, myZoneEnd);
		} else {

			requestForZone(randomNumber, entryPoint);
		}

		//Initiates a PeerNode's listeners and transmitters.
		peerCheckZone = new test();
		peerCheckZone.start();
		peerZoneManager = new PeerZoneManager();
		peerZoneManager.start();
		peerLookUP = new PeerLookUP();
		peerLookUP.runLookUP();
		peerNeighbourUpdate = new PeerNeighbourUpdate();
		peerNeighbourUpdate.start();
		peerDetails = new PeerDetails();
		peerDetails.start();
		peerLookUpUpdate = new PeerLookUpUpdate();
		peerLookUpUpdate.start();
		peerFileDownload = new PeerFileDownloadListener();
		peerFileDownload.start();
		peerFileUpload = new PeerTransmitterListener();
		peerFileUpload.start();
	}

	public void run() {

		Scanner scanner = new Scanner(System.in);

		boolean isNodeRunning = true;

		while (isNodeRunning) {

			//Print options menu on command line
			printOptionsMenu();


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
				if (!leaveZone()) {
					System.out.println("Leave Zone failed. Try again.");
				} else {
					System.out.println("Leave Zone success. Node shutdown complete.");
					isNodeRunning = false;
				}
				break;
			case 3://Option to upload file to chord zone
				System.out.println("Enter the file name to upload");
				scanner = new Scanner(System.in);
				filePath = scanner.nextLine();
				PeerFileManager peerFileUpload = new PeerFileManager();
				peerFileUpload.uploadFile(filePath, false);
				System.out.println("Upload complete. Yay!");
				break;
			case 4://Option to download file from chord zone
				System.out.print("Enter the file name for download");
				scanner = new Scanner(System.in);
				filePath = scanner.nextLine();
				filePath = new File(filePath).getName();
				PeerFileManager peerFiledownload = new PeerFileManager();
				try {
					peerFiledownload.downloadFile(filePath);
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

		scanner.close();

	}

	/**
	 * Prints command line options for user input
	 */
	public static void printOptionsMenu()
	{
		System.out.println("Choose an option:");
		System.out.println("1 - Finger Table Details ");
		System.out.println("2 - Leave chord zone ");
		System.out.println("3 - Upload File to zone ");
		System.out.println("4 - Download File from zone ");
		System.out.println("5 - Files present at this node ");

		System.out.print("Your input - ");
	}

	/**
	 * getDetails method prints the node details with list of finger tables maintained.
	 */
	private void getDetails() {

		int totalIter = PeerMain.n;
		int zoneChecking = myZoneSrt;

		while (zoneChecking != myZoneEnd) {
			totalIter--;
			zoneChecking++;

		}

		System.out.println(myIP + " Zone details:" + myZoneSrt + "-" + myZoneEnd);
		System.out.println(
				"Predecessor:" + predecessor.getIP() + " " + predecessor.getZoneSrt() + " " + predecessor.getZoneEnd());
		System.out.println(
				"Successor:" + successor.getIP() + " " + successor.getZoneSrt() + " " + successor.getZoneEnd());

		System.out.println("Finger Table:");
		for (int i = 0; i < PeerMain.m; i++) {
			System.out.println(i + " " + peerLookUP.getPeerIP(i));
		}

		System.out.println("Is EntryPoint:" + isEntryPoint);

		System.out.println("---------------------------------");

		totalIter--;
		zoneChecking++;
		if (zoneChecking >= PeerMain.n) {
			zoneChecking = 0;
		}

		while (totalIter > 0) {

			String peerIP = getZoneIP(zoneChecking, nearestPeer(zoneChecking));

			String message[] = getPeerDetails(peerIP);
			String zone[] = message[0].split(" ");
			String lookUP[] = message[3].split(" ");
			int endZone = Integer.parseInt(zone[1]);

			System.out.println(peerIP + " Zone details:" + zone[0] + "-" + zone[1]);
			System.out.println("Predecessor:" + message[1]);
			System.out.println("Successor:" + message[2]);

			System.out.println("Finger Table:");
			for (int i = 0; i < PeerMain.m; i++) {
				System.out.println(i + " " + lookUP[i]);
			}

			System.out.println("Is EntryPoint:" + message[4]);

			System.out.println("---------------------------------");

			totalIter--;
			zoneChecking++;
			if (zoneChecking >= PeerMain.n) {
				zoneChecking = 0;
			}

			while (zoneChecking <= endZone) {
				totalIter--;
				zoneChecking++;
			}

			if (zoneChecking >= PeerMain.n) {
				zoneChecking = 0;
			}

		}

		System.out.println("+++---------------------------+++");

	}

	/**
	 * isFileOwner method check if this node contains the given file name
	 * @param fileName
	 * @return
     */
	public boolean isFileOwner(String fileName) {
		return fileNames.contains(fileName);
	}

	/***
	 * requestForZone method takes a randomNumber and requestIP as input and based
	 * on the IP found for the zone, a request is placed to that particular zone owner.
	 * @param randomNumber
	 * @param requestIP
     */
	private void requestForZone(int randomNumber, String requestIP) {
		System.out.println("requestIP:"+requestIP);

		requestIP = getZoneIP(randomNumber, requestIP);
		System.out.println("requestIP_new:"+requestIP);
		getMyZone(randomNumber, requestIP);

	}

	/**
	 * hash method generates the hash of the file name that is supplied.
	 * @param fileName
	 * @return
     */
	public int hash(String fileName) {
		byte[] filePathBytes = fileName.getBytes();
		Checksum value = new CRC32();
		value.update(filePathBytes, 0, filePathBytes.length);
		int hash = (int) value.getValue() % 127;
		if (hash < 0)
			hash = hash + 127;
		return hash;
	}

	/**
	 * leaveZone method takes care of all the updates it needs to do before this
	 * node leaves the zone.
	 * @return a boolean value if the request for leaving the zone has been confirmed
	 * by the concerned peer.
     */
	private boolean leaveZone() {

		Socket socket;

		try {
			if (PeerNode.isEntryPoint) {
				socket = new Socket(predecessor.getIP(), 9991);

			} else {
				socket = new Socket(successor.getIP(), 9991);
			}

			DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
			DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

			makeMessage("leave");

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

	/**
	 * stopAllNodeListeners method takes care of stopping all
	 * the servers at this peer node.
	 */
	private void stopAllNodeListeners() {

		peerCheckZone.stopServer();
		peerZoneManager.stopServer();
		peerNeighbourUpdate.stopServer();
		peerDetails.stopServer();
		peerLookUpUpdate.stopServer();
		peerFileUpload.stopServer();
		peerFileDownload.stopServer();
	}

	/***
	 * shareFilesToNeighbours method takes care of sending the files
	 * present at this node to their respective zone owners.
	 */
	private void shareFilesToNeighbours() {
		PeerNode.setMyZone(-1, -1);
		PeerFileManager fileManager = new PeerFileManager();

		for(String fileName : PeerNode.fileNames)
		{
			fileManager.uploadFile(fileName, true);
		}
	}

	/***
	 * getMyZone method places the request to get into the zone owner by
	 * a peer. Once confirmed by the peer, this node is instantiated with
	 * the neccessary details.
	 *
	 * @param randomNumber
	 * @param requestIP
     */
	private void getMyZone(int randomNumber, String requestIP) {

		try {

			Socket socket = new Socket(requestIP, 9991);

			DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
			DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

			makeMessage("add");
			dataOutputStream.write(sendData);
			dataOutputStream.flush();

			dataInputStream.read(recvData, 0, recvData.length);

			String initDetails[] = new String(recvData).trim().split(" ");

			myZoneSrt = Integer.parseInt(initDetails[0]);
			myZoneEnd = Integer.parseInt(initDetails[1]);

			predecessor = new PeerNeighbour(initDetails[2], Integer.parseInt(initDetails[3]),
					Integer.parseInt(initDetails[4]));
			successor = new PeerNeighbour(initDetails[5], Integer.parseInt(initDetails[6]),
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

	/***
	 * makeMessage constructs any message before it is sent
	 * across the zone.
	 * @param message
     */
	private void makeMessage(String message) {

		Arrays.fill(sendData, 0, messageSize, (byte) 0);
		byte messageByte[] = message.getBytes();
		ByteBuffer byteBuffer = ByteBuffer.wrap(sendData);
		byteBuffer.position(0);
		byteBuffer.put(messageByte);
		sendData = byteBuffer.array();

	}

	/**
	 * MyIP method returns this node's IP address.
	 */
	private void MyIP() {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface iface = interfaces.nextElement();
				if (iface.isLoopback() || !iface.isUp())
					continue;

				Enumeration<InetAddress> addresses = iface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress addr = addresses.nextElement();
					myIP = addr.getHostAddress();
				}
			}
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
	}


	/**
	 * getZoneIP method is common class used by several components to retrieve IP address for
	 * a given zone number.
	 * @param zoneNumber
	 * @param requestIP
     * @return
     */
	public String getZoneIP(int zoneNumber, String requestIP) {


		boolean isPeerZone = false;

		while (!isPeerZone) {
			try {

				//System.out.println("Request IP for Zone: " + zoneNumber + " to IP: " + requestIP);
				Socket socket = new Socket(requestIP, 9990);

				DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
				DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

				makeMessage(zoneNumber + "");
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

	/**
	 * sendNeighbourUpdate method sends an update to the corresponding
	 * neighbour based on the parameter value.
	 * @param IP
	 * @param value
     */
	public void sendNeighbourUpdate(String IP, int value) {

		try {

			Socket socket = new Socket(IP, 9992);

			DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

			if (value == 0) {
				makeMessage("predecessor" + " " + myZoneSrt + " " + myZoneEnd);
			} else {
				makeMessage("successor" + " " + myZoneSrt + " " + myZoneEnd);
			}

			dataOutputStream.write(sendData);
			dataOutputStream.flush();

			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * getPeerDetails method retrieves a peer's details when an IP address
	 * is supplied.
	 * @param IP
	 * @return
     */
	public String[] getPeerDetails(String IP) {

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

	/**
	 * nearestPeer method returns the closest peer for
	 * a particular zone that's given.
	 * @param zone
	 * @return
     */
	public String nearestPeer(int zone) {

		int nearestZone;

		String nearestIP;

		int tempNearestZone;
		String tempNearestIP;

		nearestZone = myZoneEnd + PeerNode.peerLookUP.getZonePos(0);
		System.out.println("nearestZone:"+nearestZone);
		if (nearestZone >= PeerMain.n) {
			nearestZone = nearestZone % PeerMain.n;
		}

		nearestIP = PeerNode.peerLookUP.getPeerIP(0);

		System.out.println("nearestIP:"+nearestIP);
		int nearestSub = zone - nearestZone;
		if (nearestSub < 0) {
			nearestSub = PeerMain.n + nearestSub;
		}
		System.out.println("nearestSub:"+nearestSub);
		for (int i = 1; i < PeerMain.m; i++) {
			tempNearestZone = PeerNode.peerLookUP.getZonePos(i);
			System.out.println("tempNearestZone:"+tempNearestZone);
			if (tempNearestZone < 0) {
				tempNearestZone = PeerMain.n + tempNearestZone;
			}
			tempNearestIP = PeerNode.peerLookUP.getPeerIP(i);
			System.out.println("tempNearestIP:"+tempNearestIP);
			int tempSub = zone - tempNearestZone;
			if (tempSub < 0) {
				tempSub = PeerMain.n + tempSub;
			}
			System.out.println("tempSub:"+tempSub);
			if (tempSub < nearestSub && tempSub >= 0) {
				nearestZone = tempNearestZone;
				nearestIP = tempNearestIP;
				nearestSub = tempSub;
			}
			System.out.println("nearestSub_new:"+nearestSub);

		}
		System.out.println("nearestIP_new:"+nearestSub);
		return nearestIP;

	}

	/**
	 * isInMyZone method checks whether the given zone is in this peer node's range.
	 * @param zone
	 * @return
     */
	public boolean isInMyZone(int zone)
	{
		return (zone <= PeerNode.getMyZoneEnd() && zone >= PeerNode.getMyZoneSrt());
	}


}
