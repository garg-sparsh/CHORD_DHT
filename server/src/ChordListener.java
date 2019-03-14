import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

//Listens for peers to join the network. Thread used to handle multiple client requests
public class ChordListener extends Thread {

	private Random random = new Random(); 
	private ServerSocket serverSocket ;
	private Socket socket;
	static private String chordIP;

	// constructor.
	public ChordListener() {
		
		try {
			
			chordIP = new String("");
			serverSocket = new ServerSocket(Constant.SERVER_PORT);
			
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	
	}

	// thread starts
	public void run() {
		
		try {
			
			while( true ) {
				System.out.println("Server listener running");
				socket = serverSocket.accept();
			
				new ChordHandler( socket ).start();
			}
			
		}
		catch( Exception e ) {
			
		}
	}

	//generates a random number to hash IP address of the peer joining the system and
	// sends the node number and entry pt details to the peer
	private class ChordHandler extends Thread {

		Socket socket;
		byte[] sendByte=new byte[Constant.MESSAGE_SIZE];

		// constructor
		public ChordHandler( Socket socket ) {
			
			this.socket = socket;
			
		}

		// thread starts
		public void run() {
			
			try {
				
				DataOutputStream dataOutputStream = new DataOutputStream( socket.getOutputStream() );

				int nodeNumber;
				nodeNumber = random.nextInt(Constant.N - 2) + 1;

				//If initial chordIP is empty will set the peer Id as entry point.
				// Else will send entry pt details to new peers joining the ring
				if( chordIP.equals("") )
				{
					makeMessage(nodeNumber + " " + "isEntryPoint");

					chordIP = socket.getInetAddress().toString().substring(1, chordIP.length());
					System.out.println(chordIP+" is the entry point");
				}
				else {
					makeMessage(nodeNumber + " " + chordIP);
				}
				
				dataOutputStream.write(sendByte);
				dataOutputStream.flush();
				
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
			
		}

		// wraps message from string to bytes
		private void makeMessage(String message) {
			
			Arrays.fill(sendByte, 0, Constant.MESSAGE_SIZE, (byte) 0);
			byte messageByte[] = message.getBytes();
			ByteBuffer byteBuffer = ByteBuffer.wrap(sendByte);
			byteBuffer.position(0);
			byteBuffer.put(messageByte);
			sendByte = byteBuffer.array();
			
		}
		
	}

	public static void setChordIP( String ip ) {
		chordIP = ip;
	}

}


//Listens for assignment of new entry point.
// This will be called whenever the entry point leaves the chord and its predecessor is assigned as new entry point
class ChordEntryListener extends Thread {

	private ServerSocket serverSocket ;
	private Socket socket;

	// constructor
	public ChordEntryListener() {

		try {

			serverSocket = new ServerSocket(Constant.ENTRY_lISTENER_PORT);

		}
		catch( Exception e ) {
			e.printStackTrace();
		}

	}

	// runs the thread
	public void run() {

		try {

			while( true ) {
				System.out.println("Chord Entry Point listener running");
				socket = serverSocket.accept();

				new ChordEntryHandler( socket ).start();
			}

		}
		catch( Exception e ) {

		}
	}

	//Handler class for entry point listener and whenever a new entry point is selected it updates the Chord Server
	// about it so that new nodes can enter with this new IP

	private class ChordEntryHandler extends Thread {

		Socket socket;
		byte[] recvMessage =new byte[Constant.MESSAGE_SIZE];
		String clientIP;

		// constructor
		public ChordEntryHandler( Socket socket ) {

			this.socket = socket;
			clientIP = socket.getInetAddress().toString();
			clientIP = clientIP.substring(1);

		}

		// thread started
		public void run() {

			try {

				DataInputStream dataInputStream = new DataInputStream( socket.getInputStream() );
				dataInputStream.read(recvMessage, 0, recvMessage.length);

				ChordListener.setChordIP(clientIP);
				System.out.println(clientIP+" is the entry point");

			}
			catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

}
