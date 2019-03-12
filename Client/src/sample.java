import java.io.File;

public class sample {
    public static void main(String... args) {
        String filePathString = "/Users/sparshgarg/Desktop/Untitled.png";
        File f = new File(filePathString);
        if (f.exists() && !f.isDirectory()) {
            System.out.println("True");
        }
    }
}
