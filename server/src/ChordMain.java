
import java.io.IOException;

public class ChordMain
{
	// main program to start the server.
	public static void main(String args[]) throws IOException {
				
		ChordListener chordListener = new ChordListener();
		chordListener.start();
		
		ChordEntryListener chordEntryListener = new ChordEntryListener();
		chordEntryListener.start();
		
	}
}
