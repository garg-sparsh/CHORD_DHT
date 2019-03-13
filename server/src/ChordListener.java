
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

/**
 *
 * The class ChordListener listens for peers that need to join
 * into the chord network. The class is a thread which listens for
 * multiple clients at a given point.
 */
public class ChordListener extends Thread {
	
	private static final int messageSize = 64;
	
	private Random random = new Random(); 
	private ServerSocket serverSocket ;
	private Socket socket;
	static private String chordIP;

	// constructor.
	public ChordListener() {
		
		try {
			
			chordIP = new String("");
			serverSocket = new ServerSocket(8880);
			
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	
	}

	// thread starts
	public void run() {
		
		try {
			
			while( true ) {
				System.out.println("Chord listener running");
				socket = serverSocket.accept();
			
				new ChordHandler( socket ).start();
			}
			
		}
		catch( Exception e ) {
			
		}
	}

	/**
	 *
	 * The class is a supporting class for ChordListener. It responds
	 * to the peers that needs to join into the chord network. The class
	 * responds back with a random zone number and an entrypoint IP. If the
	 * peer is the first peer to the network, the Chord makes the peer
	 * as the entry point.
	 *
	 */
	private class ChordHandler extends Thread {

		Socket socket;
		byte[] sendByte=new byte[messageSize];

		// constructor
		public ChordHandler( Socket socket ) {
			
			this.socket = socket;
			
		}

		// thread starts
		public void run() {
			
			try {
				
				DataOutputStream dataOutputStream = new DataOutputStream( socket.getOutputStream() );
				
				int zonePos;
				int messagePos = 0;
				
				zonePos = random.nextInt(ChordMain.getN() - 2) + 1;
				
				if( chordIP.equals("") ) {
					makeMessage(zonePos + " " + "isEntryPoint", messagePos);
					chordIP = socket.getInetAddress().toString();
					chordIP = chordIP.substring(1, chordIP.length());
					System.out.println("Chord Entry Point set - " + chordIP);
				}
				else {
					makeMessage(zonePos + " " + chordIP, messagePos);
				}
				
				dataOutputStream.write(sendByte);
				dataOutputStream.flush();
				
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
			
		}

		// wraps message into the sendByte
		private void makeMessage(String message, int pos) {
			
			Arrays.fill(sendByte, 0, messageSize, (byte) 0);
			byte messageByte[] = message.getBytes();
			ByteBuffer byteBuffer = ByteBuffer.wrap(sendByte);
			byteBuffer.position(pos);
			byteBuffer.put(messageByte);
			sendByte = byteBuffer.array();
			
		}
		
	}

	/**
	 *
	 * Entry point of the chord is assigned
	 *
	 * @param ip IP to set as entry point
     */
	public static void setChordIP( String ip ) {
		chordIP = ip;
	}

}


class ChordEntryListener extends Thread {

	private static final int messageSize = 64;

	private ServerSocket serverSocket ;
	private Socket socket;

	// constructor
	public ChordEntryListener() {

		try {

			serverSocket = new ServerSocket(8881);

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

	/**
	 *
	 * The class is a supporting class for ChordEntryListener. It responds
	 * to the peers that sends its IP as entry point.
	 *
	 */
	private class ChordEntryHandler extends Thread {

		Socket socket;
		byte[] recvMessage =new byte[messageSize];
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
				System.out.println("New Chord Entry Point set - " + clientIP);

			}
			catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

}
