
import java.io.IOException;

/**
 *
 * The class ChordMain is the main class for Bootstrap
 *
 */
public class ChordMain
{
	
	private static final int n = 128;
	private static final int m = 7;

	// main program
	public static void main(String args[]) throws IOException {
				
		ChordListener chordListener = new ChordListener();
		chordListener.start();
		
		ChordEntryListener chordEntryListener = new ChordEntryListener();
		chordEntryListener.start();
		
	}

	// getter for m
	public static int getM() {
		return m;
	}

	// setter for n
	public static int getN() {
		return n;
	}
	
}
