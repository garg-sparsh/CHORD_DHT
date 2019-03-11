
import java.io.IOException;

/**
 *
 * The class BootstrapMain is the main class for Bootstrap
 *
 * @author Srinath Kanna, Krishna Prasad, Ajeeth Kannan
 */
public class BootstrapMain 
{
	
	private static final int n = 128;
	private static final int m = 7;

	// main program
	public static void main(String args[]) throws IOException {
				
		BootstrapListener bootstrapListener = new BootstrapListener();
		bootstrapListener.start();
		
		BootstrapEntryListener bootstrapEntryListener = new BootstrapEntryListener();
		bootstrapEntryListener.start();
		
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
