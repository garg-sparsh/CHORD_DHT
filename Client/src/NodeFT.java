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


//This is the lookup class that handles stabilization by updating finger table
//whenever the node joins or leaves the system.
public class NodeFT {

	private static final int messageSize = Constant.MESSAGE_SIZE;

	private FTNodeDesc[] FTNodeDesc;
	
	private ChordNode chordNode = new ChordNode();
	
	public NodeFT() {
		
		FTNodeDesc = new FTNodeDesc[Constant.M];
		
		for( int i = 0; i < FTNodeDesc.length; i++ ) {
			FTNodeDesc[i] = new FTNodeDesc(i);
		}
		
	}
	
	public void updateMyFT() {
		
		for(int i = 0; i < Constant.M; i++ ) {
			FTNodeDesc[i].getIP();
		}
		
	}
	
	 //used to create/update Finger table details for each node
	public void runFT() {
		
		for(int i = 0; i < Constant.M; i++ ) {
			
			int updateStart = ChordNode.getNodeStart() - ( (int) Math.pow(2, i) );
			if( updateStart < 0 ) {
				updateStart = Constant.N + updateStart;
			}
			
			int updateEnd = ChordNode.getNodeEnd() - ( (int) Math.pow(2, i) );
			if( updateEnd < 0 ) {
				updateEnd = Constant.N + updateEnd;
			}
			
			int times;
			
			if( updateStart <= updateEnd ) {
				times = updateEnd - updateStart;
			}
			else {
				int times1 = Constant.N - updateStart + 1;
				int times2 = updateEnd - 0;
				
				times = times1 + times2;
				
			}
			
			int update = updateStart;
			
			while( times >= 0 ) {
				
				if( update >= ChordNode.getNodeStart() && update <= ChordNode.getNodeEnd() ) {
					
				}
				else {	
		
					String peerIP = chordNode.getKeySpaceIP(update, ChordNode.getSuccessor().getIP() );
					String message[] = chordNode.getNodeDesc(peerIP);
					
					String key[] = message[0].split(" ");
					int end = Integer.parseInt(key[1]);
					
					if( update != end ) {
						
						while( update != end ) {
							update++;
							if( update > Constant.N - 1 ) {
								update = update % Constant.N;
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
						Socket socket = new Socket( peerIP, Constant.FINGER_TABLE_lISTENER);
						
						DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
						MakeMessage makeMessage = new MakeMessage();
						byte sendData[] = new byte[messageSize];
						sendData = makeMessage.message_creation(sendData, messageSize, i + "");
						dataOutputStream.write(sendData);
						dataOutputStream.flush();
						
						socket.close();
						
					} catch (IOException e) {
						e.printStackTrace();
					} 
				}

				update++;
				if( update > Constant.N - 1 ) {
					update = update % Constant.N;
				}
				times--;
				
			}
		}
	}
	
	public void setFTIP( String ip, int i ) {
		FTNodeDesc[i].peerIP = ip;
	}


	public int getKeySpace( int i ) {
		return FTNodeDesc[i].keySpaceValue;
	}
	
	public String getPeerIP( int i ) {
		return FTNodeDesc[i].peerIP;
	}
	
	private class FTNodeDesc {
		
		int tableID;
		int keySpaceValue;
		int i;
		String peerIP;
		
		public FTNodeDesc( int ID ) {
			
			this.tableID = ID;

			i = (int) Math.pow(2, tableID);

			getIP();

		}
				
		public void getIP() {

			keySpaceValue = ChordNode.getNodeEnd() + i;
			
			if( keySpaceValue > Constant.N - 1 ) {
				keySpaceValue = keySpaceValue % Constant.N;
			}
			
			if( keySpaceValue >= ChordNode.getNodeStart() && keySpaceValue <= ChordNode.getNodeEnd() ) {
				peerIP = ChordNode.getMyIP();
			}
			else {
				peerIP = chordNode.getKeySpaceIP(keySpaceValue, ChordNode.getSuccessor().getIP() );
			}
			
		}
		
	}

	
}

//This class is called whenever a peer leaves the system or joins the system and
// Finger table of all the existing nodes need to be updated.
class NodeFTUpdate extends Thread {

	private static final int messageSize = Constant.MESSAGE_SIZE;

	private ServerSocket FTServer;

	Socket socket;

	private boolean isFTServerRunning;

	// constructor
	public NodeFTUpdate() {

		try {
			isFTServerRunning = true;
			FTServer = new ServerSocket(Constant.FINGER_TABLE_lISTENER);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// thread starts
	public void run() {

		while (isFTServerRunning) {

			try {

				socket = FTServer.accept();

				new PeerLookUpUpdateHandler(socket).start();

			} catch (IOException e) {

			}

		}

	}

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

	//stop FTServerSocket
	public void stopServer() {
		try {
			FTServer.close();
		} catch (IOException e) {

		}
		isFTServerRunning = false;
	}

}

// this class is used to store the neighbours of the node ie predecessors and successors
class Neighbour {

	private String ip;
	private int keySpaceStart;
	private int keySpaceEnd;

	// constructor
	public Neighbour(String ip, int start, int end) {

		this.ip = ip;
		this.keySpaceStart = start;
		this.keySpaceEnd = end;

	}

	public void updateKeySpace(String ip, int start, int end) {
		this.ip = ip;
		this.keySpaceStart = start;
		this.keySpaceEnd = end;
	}

	// getter for IP
	public String getIP() {
		return ip;
	}

	// getter for start key
	public int getNodeStart() {
		return keySpaceStart;
	}

	// geter for end key
	public int getNodeEnd() {
		return keySpaceEnd;
	}

}

//Get the updates of successors and predecessors from the other nodes in the chord,
// and to maintain the consistency multithreading is applied for multiple clients at same time
class NeighbourUpdate extends Thread {

	private static final int messageSize = Constant.MESSAGE_SIZE;

	private ServerSocket serverSocket;
	Socket socket;
	private boolean isServerRunning;

	// constructor
	public NeighbourUpdate() {

		try {
			serverSocket = new ServerSocket(Constant.KEY_SPACE_lISTENER_PORT);
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

				new NeighbourUpdateHandler(socket).start();

			} catch (IOException e) {

			}

		}
	}

	//stop server
	public void stopServer() {
		try {
			serverSocket.close();
		} catch (IOException e) {

		}
		isServerRunning = false;
	}


	private class NeighbourUpdateHandler extends Thread {

		Socket socket;
		String clientIP;
		byte recvData[] = new byte[messageSize];

		// constructor
		public NeighbourUpdateHandler(Socket socket) {

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

//this Class sends files which are requested by other nodes in the chord
class TransferListener extends Thread {
	//data members to read and send the file
	private static final int messageSize = Constant.FILE_MSG_SIZE;

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

	public TransferListener() {

		try {
			serverSocket = new ServerSocket(Constant.DOWNLOAD_PORT);
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

	// class to handle the thread of the TransferListener class
	private class FileUploadHandler extends Thread {

		Socket socket;

		byte sendData[] = new byte[messageSize];
		byte recvData[] = new byte[messageSize];

		public FileUploadHandler(Socket socket) {

			this.socket = socket;

		}

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
				ChordNode.menu();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		private void packetsToSend() {
			try {

				DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
				String data = (filePath + " " + totalPackets + " ");
				MakeMessage makeMessage = new MakeMessage();
				sendData = makeMessage.message_creation(sendData, messageSize, data);
				dataOutputStream.write(sendData);
				for (int i = 0; i < totalPackets; i++) {
					dataOutputStream.write(fileInPackets[i]);
				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void readFileToTransmit() {

			file = new File(String.valueOf(ChordNode.getMyIP() + "/" + chordNode.hash(filePath)));
			try {
				fileInBytes = Files.readAllBytes(file.toPath());
			} catch (IOException e) {

				e.printStackTrace();
			}

		}

		private void packetsToCreate() {

			fileSize = file.length();
			totalPackets = (int) Math.ceil(fileSize / (double) messageSize);

			fileInPackets = new byte[totalPackets][messageSize];
			for (int i = 0; i < totalPackets - 1; i++)
				System.arraycopy(fileInBytes, i * messageSize, fileInPackets[i], 0, messageSize);
			System.arraycopy(fileInBytes, (totalPackets - 1) * messageSize, fileInPackets[totalPackets - 1], 0, (int) fileSize - (totalPackets - 1) * Constant.FILE_MSG_SIZE);

		}
	}
}

//Converts the string to bytes to transfer the packets over sockets
class MakeMessage{

	public byte[] message_creation(byte[] sendData, int messageSize, String message){
		Arrays.fill(sendData, 0, messageSize, (byte) 0);
		byte messageByte[] = message.getBytes();
		ByteBuffer byteBuffer = ByteBuffer.wrap(sendData);
		byteBuffer.position(0);
		byteBuffer.put(messageByte);
		sendData = byteBuffer.array();
		return sendData;
		}
}
