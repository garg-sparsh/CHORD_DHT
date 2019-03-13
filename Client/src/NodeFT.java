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

public class NodeFT {
	
	private static final int messageSize = 64;
	
	private byte sendData[] = new byte[messageSize];
	
	private FTNodeDesc[] FTNodeDesc;
	
	private ChordNode chordNode = new ChordNode();
	
	public NodeFT() {
		
		FTNodeDesc = new FTNodeDesc[ChordPeerMain.m];
		
		for( int i = 0; i < FTNodeDesc.length; i++ ) {
			FTNodeDesc[i] = new FTNodeDesc(i);
		}
		
	}
	
	public void updateMyFT() {
		
		for(int i = 0; i < ChordPeerMain.m; i++ ) {
			FTNodeDesc[i].getIP();
		}
		
	}
	
	 
	public void runFT() {
		
		for(int i = 0; i < ChordPeerMain.m; i++ ) {
			
			int zoneUpdatingFrom = ChordNode.getNodeStart() - ( (int) Math.pow(2, i) );
			if( zoneUpdatingFrom < 0 ) {
				zoneUpdatingFrom = ChordPeerMain.n + zoneUpdatingFrom;
			}
			
			int zoneUpdatingTill = ChordNode.getNodeEnd() - ( (int) Math.pow(2, i) );
			if( zoneUpdatingTill < 0 ) {
				zoneUpdatingTill = ChordPeerMain.n + zoneUpdatingTill;
			}
			
			int times;
			
			if( zoneUpdatingFrom <= zoneUpdatingTill ) {
				times = zoneUpdatingTill - zoneUpdatingFrom;
			}
			else {
				int times1 = ChordPeerMain.n - zoneUpdatingFrom + 1;
				int times2 = zoneUpdatingTill - 0;
				
				times = times1 + times2;
				
			}
			
			int zoneUpdating = zoneUpdatingFrom;
			
			while( times >= 0 ) {
				
				if( zoneUpdating >= ChordNode.getNodeStart() && zoneUpdating <= ChordNode.getNodeEnd() ) {
					
				}
				else {	
		
					String peerIP = chordNode.getKeySpaceIP(zoneUpdating, ChordNode.getSuccessor().getIP() );
					String message[] = chordNode.getNodeDesc(peerIP);
					
					String zone[] = message[0].split(" ");
					int srtZone = Integer.parseInt(zone[0]);
					int endZone = Integer.parseInt(zone[1]);
					
					if( zoneUpdating != endZone ) {
						
						while( zoneUpdating != endZone ) {
							zoneUpdating++;
							if( zoneUpdating > ChordPeerMain.n - 1 ) {
								zoneUpdating = zoneUpdating % ChordPeerMain.n;
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
				if( zoneUpdating > ChordPeerMain.n - 1 ) {
					zoneUpdating = zoneUpdating % ChordPeerMain.n;
				}
				
				times--;
				
			}
			
		}
		
		
	}
	
	public void setFTIP( String ip, int i ) {
		FTNodeDesc[i].peerIP = ip;
	}


	public int getZone( int i ) {
		return FTNodeDesc[i].zone;
	}
	
	public String getPeerIP( int i ) {
		return FTNodeDesc[i].peerIP;
	}
	
	private class FTNodeDesc {
		
		int tableID;
		int zone;
		int zonePos;
		String peerIP;
		
		public FTNodeDesc( int ID ) {
			
			this.tableID = ID;
			
			zonePos = (int) Math.pow(2, tableID);
			
			getIP();
			
		}
				
		public void getIP() {
						
			zone = ChordNode.getNodeEnd() + zonePos;
			
			if( zone > ChordPeerMain.n - 1 ) {
				zone = zone % ChordPeerMain.n;
			}
			
			if( zone >= ChordNode.getNodeStart() && zone <= ChordNode.getNodeEnd() ) {
				peerIP = ChordNode.getMyIP();
			}
			else {
				peerIP = chordNode.getKeySpaceIP(zone, ChordNode.getSuccessor().getIP() );
			}
			
		}
		
	}

	
}

class NodeFTUpdate extends Thread {

	private static final int messageSize = 64;

	private ServerSocket serverSocket;

	Socket socket;

	private boolean isServerRunning;

	// constructor
	public NodeFTUpdate() {

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
	 * The class is a support class for NodeFTUpdate
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

				ChordNode.NodeFT.setFTIP(clientIP, message);

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


class Neighbour {

	private String ip;
	private int zoneSrt;
	private int zoneEnd;

	// constructor
	public Neighbour(String ip, int zoneSrt, int zoneEnd) {

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
	public int getNodeStart() {
		return zoneSrt;
	}

	// geter for end zone
	public int getNodeEnd() {
		return zoneEnd;
	}

}

class NeighbourUpdate extends Thread {

	private static final int messageSize = 64;

	private ServerSocket serverSocket;
	Socket socket;
	private boolean isServerRunning;

	// constructor
	public NeighbourUpdate() {

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
	 * The class is a supporter class for NeighbourUpdate.
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
					ChordNode.setPredecessor(clientIP, Integer.parseInt(message[1]), Integer.parseInt(message[2]));
				} else if (new String(recvData).trim().contains("successor")) {
					ChordNode.setSuccessor(clientIP, Integer.parseInt(message[1]), Integer.parseInt(message[2]));
				}

				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}

class TransferListener extends Thread {
	//data members to read and send the file
	private static final int messageSize = 1024;

	private ServerSocket serverSocket;

	ChordNode chordNode = new ChordNode();
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
	public TransferListener() {

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
				if (chordNode.isFileOwner(data)) {
					filePath = data;
					readFileToTransmit();
					packetsToCreate();
					packetsToSend();

				} else {
					data = "NoSuchFile";
					sendData = data.getBytes();
					dataOutputStream.write(sendData);
				}
				ChordNode.printOptionsMenu();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		/**
		 * method to send the file packets
		 */
		private void packetsToSend() {
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

			file = new File(String.valueOf(ChordNode.getMyIP() + "/" + chordNode.hash(filePath)));
			try {
				fileInBytes = Files.readAllBytes(file.toPath());
			} catch (IOException e) {

				e.printStackTrace();
			}

		}


		/**
		 * method to make file to be sent as packets
		 */
		private void packetsToCreate() {

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
