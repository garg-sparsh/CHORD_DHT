import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;

public class PeerLookUP {
	
	private static final int messageSize = 64;
	
	private byte sendData[] = new byte[messageSize];
	
	private LookUPPeerDetails[] lookUPPeerDetails;
	
	private PeerNode peerNode = new PeerNode();
	
	public PeerLookUP() {
		
		lookUPPeerDetails = new LookUPPeerDetails[PeerMain.m];
		
		for( int i = 0; i < lookUPPeerDetails.length; i++ ) {
			lookUPPeerDetails[i] = new LookUPPeerDetails(i);
		}
		
	}
	
	public void updateMyLookUP() {
		
		for( int i = 0; i < PeerMain.m; i++ ) {
			lookUPPeerDetails[i].getIP();
		}
		
	}
	
	 
	public void runLookUP() {
		
		for( int i = 0; i < PeerMain.m; i++ ) {
			
			int zoneUpdatingFrom = PeerNode.getMyZoneSrt() - ( (int) Math.pow(2, i) );
			if( zoneUpdatingFrom < 0 ) {
				zoneUpdatingFrom = PeerMain.n + zoneUpdatingFrom;
			}
			
			int zoneUpdatingTill = PeerNode.getMyZoneEnd() - ( (int) Math.pow(2, i) );
			if( zoneUpdatingTill < 0 ) {
				zoneUpdatingTill = PeerMain.n + zoneUpdatingTill;
			}
			
			int times;
			
			if( zoneUpdatingFrom <= zoneUpdatingTill ) {
				times = zoneUpdatingTill - zoneUpdatingFrom;
			}
			else {
				int times1 = PeerMain.n - zoneUpdatingFrom + 1;
				int times2 = zoneUpdatingTill - 0;
				
				times = times1 + times2;
				
			}
			
			int zoneUpdating = zoneUpdatingFrom;
			
			while( times >= 0 ) {
				
				if( zoneUpdating >= PeerNode.getMyZoneSrt() && zoneUpdating <= PeerNode.getMyZoneEnd() ) {
					
				}
				else {	
		
					String peerIP = peerNode.getZoneIP(zoneUpdating, PeerNode.getSuccessor().getIP() );
					String message[] = peerNode.getPeerDetails(peerIP);
					
					String zone[] = message[0].split(" ");
					int srtZone = Integer.parseInt(zone[0]);
					int endZone = Integer.parseInt(zone[1]);
					
					if( zoneUpdating != endZone ) {
						
						while( zoneUpdating != endZone ) {
							zoneUpdating++;
							if( zoneUpdating > PeerMain.n - 1 ) {
								zoneUpdating = zoneUpdating % PeerMain.n;
							}
							
							times--;
							
							if( times < 0 ) {
								break;
							}
							
						}
						
					}
					
					if( times < 0 ) {
						break;
					}
				 
					try {
						Socket socket = new Socket( peerIP, 9994);
						
						DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
						MakeMessage makeMessage = new MakeMessage();
						sendData = makeMessage.message_creation(sendData, messageSize, i + "");
						dataOutputStream.write(sendData);
						dataOutputStream.flush();
						
						socket.close();
						
					} catch (IOException e) {
						e.printStackTrace();
					} 
					
				}
				
				zoneUpdating++;
				if( zoneUpdating > PeerMain.n - 1 ) {
					zoneUpdating = zoneUpdating % PeerMain.n;
				}
				
				times--;
				
			}
			
		}
		
		
	}
	
	public void setLookUPIP( String ip, int i ) {
		lookUPPeerDetails[i].peerIP = ip;
	}

	public int getZonePos( int i ) {
		return lookUPPeerDetails[i].zonePos;
	}

	public int getZone( int i ) {
		return lookUPPeerDetails[i].zone;
	}
	
	public String getPeerIP( int i ) {
		return lookUPPeerDetails[i].peerIP;
	}
	
	private class LookUPPeerDetails {
		
		int tableID;
		int zone;
		int zonePos;
		String peerIP;
		
		public LookUPPeerDetails( int ID ) {
			
			this.tableID = ID;
			
			zonePos = (int) Math.pow(2, tableID);
			
			getIP();
			
		}
				
		public void getIP() {
						
			zone = PeerNode.getMyZoneEnd() + zonePos;
			
			if( zone > PeerMain.n - 1 ) {
				zone = zone % PeerMain.n;
			}
			
			if( zone >= PeerNode.getMyZoneSrt() && zone <= PeerNode.getMyZoneEnd() ) {
				peerIP = PeerNode.getMyIP();
			}
			else {
				peerIP = peerNode.getZoneIP(zone, PeerNode.getSuccessor().getIP() );	
			}
			
		}
		
	}
	
	/*private void makeMessage(String message) {
		
		Arrays.fill(sendData, 0, messageSize, (byte) 0);
		byte messageByte[] = message.getBytes();
		ByteBuffer byteBuffer = ByteBuffer.wrap(sendData);
		byteBuffer.position(0);
		byteBuffer.put(messageByte);
		sendData = byteBuffer.array();
	
	}*/
	
}

class PeerLookUpUpdate extends Thread {

	private static final int messageSize = 64;

	private ServerSocket serverSocket;

	Socket socket;

	private boolean isServerRunning;

	// constructor
	public PeerLookUpUpdate() {

		try {
			isServerRunning = true;
			serverSocket = new ServerSocket(9994);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// thread starts
	public void run() {

		while (isServerRunning) {

			try {

				socket = serverSocket.accept();

				new PeerLookUpUpdateHandler(socket).start();

			} catch (IOException e) {

			}

		}

	}

	/**
	 * The class is a support class for PeerLookUpUpdate
	 */
	private class PeerLookUpUpdateHandler extends Thread {

		Socket socket;

		String clientIP;

		byte recvData[] = new byte[messageSize];

		// constructor
		public PeerLookUpUpdateHandler(Socket socket) {

			this.socket = socket;
			clientIP = socket.getInetAddress().toString();
			clientIP = clientIP.substring(1, clientIP.length());

		}

		// thread starts
		public void run() {
			try {

				DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

				dataInputStream.read(recvData, 0, recvData.length);

				int message = Integer.parseInt(new String(recvData).trim());

				PeerNode.peerLookUP.setLookUPIP(clientIP, message);

				socket.close();
			} catch (IOException e) {

			}
		}

	}

	/**
	 * stops serverSocket
	 */
	public void stopServer() {
		try {
			serverSocket.close();
		} catch (IOException e) {

		}
		isServerRunning = false;
	}

}


class PeerNeighbour {

	private String ip;
	private int zoneSrt;
	private int zoneEnd;

	// constructor
	public PeerNeighbour(String ip, int zoneSrt, int zoneEnd) {

		this.ip = ip;
		this.zoneSrt = zoneSrt;
		this.zoneEnd = zoneEnd;

	}

	/**
	 *
	 * @param ip neighbor IP
	 * @param zoneSrt start zone of the neighbor
	 * @param zoneEnd end zone of the neighbor
	 */
	public void updateZone(String ip, int zoneSrt, int zoneEnd) {
		this.ip = ip;
		this.zoneSrt = zoneSrt;
		this.zoneEnd = zoneEnd;
	}

	// getter for IP
	public String getIP() {
		return ip;
	}

	// getter for start zone
	public int getZoneSrt() {
		return zoneSrt;
	}

	// geter for end zone
	public int getZoneEnd() {
		return zoneEnd;
	}

}

class PeerNeighbourUpdate extends Thread {

	private static final int messageSize = 64;

	private ServerSocket serverSocket;
	Socket socket;
	private boolean isServerRunning;

	// constructor
	public PeerNeighbourUpdate() {

		try {
			serverSocket = new ServerSocket(9992);
			isServerRunning = true;
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// thread starts
	public void run() {



		while (isServerRunning) {

			try {

				socket = serverSocket.accept();

				new PeerNeighbourUpdateHandler(socket).start();

			} catch (IOException e) {

			}

		}

		// socket.close();

	}

	/**
	 * closes serverSocket
	 */
	public void stopServer() {
		try {
			serverSocket.close();
		} catch (IOException e) {

		}
		isServerRunning = false;
	}

	/**
	 *
	 * The class is a supporter class for PeerNeighbourUpdate.
	 *
	 */
	private class PeerNeighbourUpdateHandler extends Thread {

		Socket socket;

		String clientIP;

		byte recvData[] = new byte[messageSize];

		// constructor
		public PeerNeighbourUpdateHandler(Socket socket) {

			this.socket = socket;
			clientIP = socket.getInetAddress().toString();
			clientIP = clientIP.substring(1, clientIP.length());

		}

		// thread starts
		public void run() {
			try {

				DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

				dataInputStream.read(recvData, 0, recvData.length);

				String message[] = new String(recvData).trim().split(" ");

				if (new String(recvData).trim().contains("predecessor")) {
					PeerNode.setPredecessor(clientIP, Integer.parseInt(message[1]), Integer.parseInt(message[2]));
				} else if (new String(recvData).trim().contains("successor")) {
					PeerNode.setSuccessor(clientIP, Integer.parseInt(message[1]), Integer.parseInt(message[2]));
				}

				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}

class PeerTransmitterListener extends Thread {
	//data members to read and send the file
	private static final int messageSize = 1024;

	private ServerSocket serverSocket;

	PeerNode peerNode = new PeerNode();
	private boolean isServerRunning;
	Socket socket;

	private byte fileInBytes[];
	private byte fileInPackets[][];
	private File file;

	private static String filePath;
	private long fileSize;
	private int totalPackets;

	/**
	 * constructor of the class
	 */
	public PeerTransmitterListener() {

		try {
			serverSocket = new ServerSocket(9485);
			isServerRunning = true;
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	/**
	 * method to handle the thread of the class
	 */
	public void run() {

		while (isServerRunning) {

			try {

				socket = serverSocket.accept();

				new FileUploadHandler(socket).start();

			} catch (IOException e) {

			}

		}


	}
	/**
	 * method to stop the thread
	 */
	public void stopServer() {
		try {
			serverSocket.close();
		} catch (IOException e) {

		}
		isServerRunning = false;
	}
	/**
	 * class to handle the thread of the above class
	 *
	 */
	private class FileUploadHandler extends Thread {

		Socket socket;

		byte sendData[] = new byte[messageSize];
		byte recvData[] = new byte[messageSize];
		/**
		 * constructor for the class
		 * @param socket-socket to send and receive request
		 */
		public FileUploadHandler(Socket socket) {

			this.socket = socket;

		}
		/**
		 * method to handle the thread of the class
		 */
		public void run() {

			try {

				DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
				DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

				dataInputStream.read(recvData, 0, recvData.length);

				String data = new String(recvData).trim();
				System.out.println("File name requested :" + data);
				if (peerNode.isFileOwner(data)) {
					filePath = data;
					readFileToTransmit();
					makePackets();
					sendPackets();

				} else {
					data = "NoSuchFile";
					sendData = data.getBytes();
					dataOutputStream.write(sendData);
				}
				PeerNode.printOptionsMenu();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		/**
		 * method to send the file packets
		 */
		private void sendPackets() {
			try {

				DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
				System.out.println(new String(filePath + " " + totalPackets));
				String data = (filePath + " " + totalPackets + " ");
				MakeMessage makeMessage = new MakeMessage();
				sendData = makeMessage.message_creation(sendData, messageSize, data);
				dataOutputStream.write(sendData);
				for (int i = 0; i < totalPackets; i++) {
					dataOutputStream.write(fileInPackets[i]);
				}
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		/**
		 * method to read the file to be transmitted
		 */
		private void readFileToTransmit() {

			file = new File(String.valueOf("../../" + PeerNode.getMyIP() + "/" + peerNode.hash(filePath)));
			try {
				fileInBytes = Files.readAllBytes(file.toPath());
			} catch (IOException e) {

				e.printStackTrace();
			}

		}


		/**
		 * method to make file to be sent as packets
		 */
		private void makePackets() {

			fileSize = file.length();
			totalPackets = (int) Math.ceil(fileSize / (double) messageSize);

			fileInPackets = new byte[totalPackets][messageSize];
			for (int i = 0; i < totalPackets - 1; i++)
				System.arraycopy(fileInBytes, i * messageSize, fileInPackets[i], 0, messageSize);
			System.arraycopy(fileInBytes, (totalPackets - 1) * messageSize, fileInPackets[totalPackets - 1], 0, (int) fileSize - (totalPackets - 1) * 1024);

		}



	}

}

class MakeMessage{

	public MakeMessage(){

	}

	public byte[] message_creation(byte[] sendData, int messageSize, String message){
		Arrays.fill(sendData, 0, messageSize, (byte) 0);
		byte messageByte[] = message.getBytes();
		ByteBuffer byteBuffer = ByteBuffer.wrap(sendData);
		byteBuffer.position(0);
		byteBuffer.put(messageByte);
		sendData = byteBuffer.array();
		return sendData;
		}

	public byte[] message_creation(byte[] sendData, int messageSize, String message, int pos){
		Arrays.fill(sendData, 0, messageSize, (byte) 0);
		byte messageByte[] = message.getBytes();
		ByteBuffer byteBuffer = ByteBuffer.wrap(sendData);
		byteBuffer.position(pos);
		byteBuffer.put(messageByte);
		sendData = byteBuffer.array();
		return sendData;
	}
}
