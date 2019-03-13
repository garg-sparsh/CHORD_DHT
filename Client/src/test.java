import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 *
 * The class is checks whether the requested zone number from a peer
 * is this peer's zone number. The class is a thread which can
 * support multiple clients at a given point.
 *
 * @author Srinath Kanna, Krishna Prasad, Ajeeth Kannan
 */
public class test extends Thread {

    private static final int messageSize = 64;

    private ServerSocket serverSocket;

    PeerNode peerNode = new PeerNode();
    private boolean isServerRunning;
    Socket socket;

    // constructor
    public test() {

        try {
            serverSocket = new ServerSocket(9990);
            isServerRunning = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // thread starts
    public void run() {

        while( isServerRunning ) {

            try {

                socket = serverSocket.accept();

                new PeerZoneCheckHandler(socket).start();

            } catch (IOException e) {

            }

        }


    }

    /**
     * stops the serverSocket
     */
    public void stopServer()
    {
        try {
            serverSocket.close();
        } catch (IOException e) {

        }
        isServerRunning = false;
    }

    /**
     *
     * The class is a supporter class for PeerCheckZone. The class gives response
     * to the peer by checking its zone.
     *
     *
     * @author Srinath Kanna, Krishna Prasad, Ajeeth Kannan
     */

    private class PeerZoneCheckHandler extends Thread {

        Socket socket;

        byte sendData[] = new byte[messageSize];
        byte recvData[] = new byte[messageSize];

        // constructor
        public PeerZoneCheckHandler(Socket socket) {

            this.socket = socket;

        }

        // thread starts
        public void run() {

            try {

                int messagePos = 0;

                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                dataInputStream.read(recvData, 0, recvData.length);

                int zoneQuery = Integer.parseInt( new String( recvData ).trim() );
                System.out.println("zoneQuery:"+zoneQuery);

                System.out.println("PeerNode.getMyZoneSrt():"+PeerNode.getMyZoneSrt()+" "+PeerNode.getMyZoneEnd());

                if( zoneQuery >= PeerNode.getMyZoneSrt() && zoneQuery <= PeerNode.getMyZoneEnd() ) {
                    MakeMessage makeMessage = new MakeMessage();
                    sendData = makeMessage.message_creation(sendData, messageSize, "isMyZone", messagePos);
                    dataOutputStream.write(sendData);
                    dataOutputStream.flush();
                }
                else {

                    String nearestIP;

                    if( zoneQuery >= PeerNode.getPredecessor().getZoneSrt() &&
                            zoneQuery <= PeerNode.getPredecessor().getZoneEnd() ) {
                        nearestIP = PeerNode.getPredecessor().getIP();
                    }
                    else {
                        nearestIP = peerNode.nearestPeer(zoneQuery);
                    }
                    MakeMessage makeMessage = new MakeMessage();
                    sendData = makeMessage.message_creation(sendData, messageSize, nearestIP, messagePos);
                    dataOutputStream.write(sendData);
                    dataOutputStream.flush();
                }


            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        // wraps message into the sendByte
//        private void makeMessage(String message, int pos) {
//
//            Arrays.fill(sendData, 0, messageSize, (byte) 0);
//            byte messageByte[] = message.getBytes();
//            ByteBuffer byteBuffer = ByteBuffer.wrap(sendData);
//            byteBuffer.position(pos);
//            byteBuffer.put(messageByte);
//            sendData = byteBuffer.array();
//
//        }

    }

}

class PeerDetails extends Thread {

    private static final int messageSize = 1024;

    private ServerSocket serverSocket;
    private boolean isServerRunning;
    Socket socket;

    // constructor
    public PeerDetails() {

        try {
            isServerRunning = true;
            serverSocket = new ServerSocket(9993);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // thread starts
    public void run() {

        while (isServerRunning) {

            try {

                socket = serverSocket.accept();

                new PeerDetailsHandler(socket).start();

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

    /**
     *
     * The class is a supporter class for PeerDetails. The class gives response
     * to the peer by giving details of this peer
     *
     *
     * @author Srinath Kanna, Krishna Prasad, Ajeeth Kannan
     */
    private class PeerDetailsHandler extends Thread {

        Socket socket;

        String clientIP;

        byte sendData[] = new byte[messageSize];

        // // constructor
        public PeerDetailsHandler(Socket socket) {

            this.socket = socket;
            clientIP = socket.getInetAddress().toString();
            clientIP = clientIP.substring(1, clientIP.length());

        }

        // thread starts
        public void run() {
            try {

                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                String message1 = PeerNode.getMyZoneSrt() + " " + PeerNode.getMyZoneEnd() + "\n";
                String message2 = PeerNode.getPredecessor().getIP() + " " + PeerNode.getPredecessor().getZoneSrt() + " " +
                        PeerNode.getPredecessor().getZoneEnd() + "\n";
                String message3 = PeerNode.getSuccessor().getIP() + " " + PeerNode.getSuccessor().getZoneSrt() + " " +
                        PeerNode.getSuccessor().getZoneEnd() + "\n";
                String message4 = new String("");
                String message5 = PeerNode.isEntryPoint.toString() + "\n";

                for (int i = 0; i < PeerMain.m; i++) {
                    if (i != PeerMain.m - 1) {
                        message4 = message4 + PeerNode.peerLookUP.getPeerIP(i) + " ";
                    } else {
                        message4 = message4 + PeerNode.peerLookUP.getPeerIP(i) + "\n";
                    }
                }
                MakeMessage makeMessage = new MakeMessage();
                sendData = makeMessage.message_creation(sendData, messageSize, message1 + message2 + message3 + message4 + message5);
                dataOutputStream.write(sendData);
                dataOutputStream.flush();

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        // wraps message into the sendByte
//        private void makeMessage(String message) {
//
//            Arrays.fill(sendData, 0, messageSize, (byte) 0);
//            byte messageByte[] = message.getBytes();
//            ByteBuffer byteBuffer = ByteBuffer.wrap(sendData);
//            byteBuffer.position(0);
//            byteBuffer.put(messageByte);
//            sendData = byteBuffer.array();
//
//        }

    }

}

class PeerZoneManager extends Thread {
    //data members of the class
    private static final int messageSize = 64;

    private ServerSocket serverSocket;

    private PeerNode peerNode = new PeerNode();

    private boolean isServerRunning;
    Socket socket;
    /**
     * constructor of the class to accept server request
     */
    public PeerZoneManager() {

        try {
            serverSocket = new ServerSocket(9991);
            isServerRunning = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    /**
     * method to handle thread of the class
     */
    public void run() {

        while (isServerRunning) {

            try {

                socket = serverSocket.accept();
                new PeerZoneManagerHandler(socket).start();

            } catch (IOException e) {

            }

        }

    }
    /**
     * method to stop the class thread
     */
    public void stopServer() {
        try {
            serverSocket.close();
        } catch (IOException e) {

        }
        isServerRunning = false;
    }
    /**
     * method to handle accepted thread of above class
     * @author Srinath Kanna, Krishna Prasad and Ajeeth Kannan
     *
     */
    private class PeerZoneManagerHandler extends Thread {
        //data member of the class
        Socket socket;

        String clientIP;

        byte sendData[] = new byte[messageSize];
        byte recvData[] = new byte[messageSize];
        /**
         * constructor to handle the accepted threads
         * @param socket
         */
        public PeerZoneManagerHandler(Socket socket) {

            this.socket = socket;
            clientIP = socket.getInetAddress().toString();
            clientIP = clientIP.substring(1, clientIP.length());

        }
        /**
         * method to handle the thread of the class
         */
        public void run() {
            try {

                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                dataInputStream.read(recvData, 0, recvData.length);

                String message = new String(recvData).trim();

                if (message.contains("add")) {
                    add(dataOutputStream);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    shareFilesToNewPeer();
                } else if(message.contains("leave")) {

                    leave(dataOutputStream);
                }

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        /**
         * method  to send the file to new peer
         */
        private void shareFilesToNewPeer() {

            PeerFileManager fileManager = new PeerFileManager();
            List<String> deleteList = new ArrayList<>();

            for(String fileName : peerNode.fileNames)
            {
                if(!peerNode.isInMyZone(peerNode.hash(fileName))) {
                    fileManager.uploadFile(fileName, true);
                    deleteList.add(fileName);
                }

            }

            peerNode.fileNames.removeAll(deleteList);

        }
        /**
         * method to initiate for node leave
         * @param dataOutputStream
         * @throws IOException
         */
        private void leave(DataOutputStream dataOutputStream) throws IOException {

            String peerDetails[] = peerNode.getPeerDetails(clientIP);
            boolean isEntryPoint = Boolean.parseBoolean(peerDetails[4]);

            if(isEntryPoint)
            {

                PeerMain.isEntryPoint = true;
                PeerNode.isEntryPoint = true;
                sendToBootStrap();

                String zone[] = peerDetails[2].split(" ");

                PeerNode.setMyZone(  PeerNode.getMyZoneSrt() , PeerNode.getSuccessor().getZoneEnd());
                PeerNode.setSuccessor(zone[0], Integer.parseInt(zone[1]), Integer.parseInt(zone[2]));

                if(zone[0].contains(PeerNode.getMyIP()))
                {
                    PeerNode.setSuccessor(PeerNode.getMyIP(), PeerNode.getMyZoneSrt(), PeerNode.getMyZoneEnd());
                    PeerNode.setPredecessor(PeerNode.getMyIP(), PeerNode.getMyZoneSrt(), PeerNode.getMyZoneEnd());

                }

            }
            else {

                String zone[] = peerDetails[1].split(" ");

                PeerNode.setMyZone(  PeerNode.getPredecessor().getZoneSrt() , PeerNode.getMyZoneEnd());
                PeerNode.setPredecessor(zone[0], Integer.parseInt(zone[1]), Integer.parseInt(zone[2]));

                if(zone[0].contains(PeerNode.getMyIP()))
                {

                    PeerNode.setSuccessor(PeerNode.getMyIP(), PeerNode.getMyZoneSrt(), PeerNode.getMyZoneEnd());
                    PeerNode.setPredecessor(PeerNode.getMyIP(), PeerNode.getMyZoneSrt(), PeerNode.getMyZoneEnd());

                }

            }

            peerNode.sendNeighbourUpdate(PeerNode.getPredecessor().getIP(), 1);
            peerNode.sendNeighbourUpdate(PeerNode.getSuccessor().getIP(), 0);

            PeerNode.peerLookUP.runLookUP();

            PeerNode.peerLookUP.updateMyLookUP();

            MakeMessage makeMessage = new MakeMessage();
            sendData = makeMessage.message_creation(sendData, messageSize, "Confirmed");

            dataOutputStream.write(sendData);
            dataOutputStream.flush();
            dataOutputStream.close();

        }
        /**
         * method to send the update information to bootstrap regarding new entry point
         * @throws UnknownHostException
         * @throws IOException
         */
        private void sendToBootStrap() throws UnknownHostException, IOException {
            Socket entrySocket = new Socket(  PeerMain.serverIP, 8881 );
            DataOutputStream entryOutputStream = new DataOutputStream(entrySocket.getOutputStream());

            MakeMessage makeMessage = new MakeMessage();
            sendData = makeMessage.message_creation(sendData, messageSize, PeerNode.getMyIP());
            entryOutputStream.write(sendData);
            entryOutputStream.flush();
            entrySocket.close();
        }
        /**
         * method to initiate node join in chord
         * @param dataOutputStream
         * @throws IOException
         */
        private void add(DataOutputStream dataOutputStream) throws IOException {
            int midPoint = PeerNode.getMyZoneSrt() + ((PeerNode.getMyZoneEnd() - PeerNode.getMyZoneSrt()) / 2);

            if ( PeerNode.getMyIP().equals( PeerNode.getPredecessor().getIP() )
                    && PeerNode.getMyIP().equals( PeerNode.getSuccessor().getIP() ) ) {

                String message1 = PeerNode.getMyZoneSrt() + " " + midPoint + " ";

                PeerNode.setPredecessor(clientIP, PeerNode.getMyZoneSrt(), midPoint);
                PeerNode.setSuccessor(clientIP, PeerNode.getMyZoneSrt(), midPoint);

                PeerNode.setMyZone((midPoint + 1), PeerNode.getMyZoneEnd());

                String message2 = PeerNode.getMyIP() + " " + PeerNode.getMyZoneSrt() + " " + PeerNode.getMyZoneEnd() + " " +
                        PeerNode.getMyIP() + " " + PeerNode.getMyZoneSrt() + " " + PeerNode.getMyZoneEnd();

                MakeMessage makeMessage = new MakeMessage();
                sendData = makeMessage.message_creation(sendData, messageSize, message1 + message2);

            } else {

                String message1 = PeerNode.getMyZoneSrt() + " " + midPoint + " " + PeerNode.getPredecessor().getIP() + " "
                        + PeerNode.getPredecessor().getZoneSrt() + " " + PeerNode.getPredecessor().getZoneEnd() + " " ;

                PeerNode.setPredecessor(clientIP, PeerNode.getMyZoneSrt(), midPoint);
                PeerNode.setMyZone((midPoint + 1), PeerNode.getMyZoneEnd());

                String message2 = PeerNode.getMyIP() + " " + PeerNode.getMyZoneSrt() + " " + PeerNode.getMyZoneEnd();

                MakeMessage makeMessage = new MakeMessage();
                sendData = makeMessage.message_creation(sendData, messageSize, message1 + message2);

                peerNode.sendNeighbourUpdate(PeerNode.getSuccessor().getIP(), 0);

            }

            dataOutputStream.write(sendData);

            dataOutputStream.flush();

        }
        /**
         * method to wrap the message to be sent to message size
         * @param message-message to be sent
         */
//        private void makeMessage(String message) {
//
//            Arrays.fill(sendData, 0, messageSize, (byte) 0);
//            byte messageByte[] = message.getBytes();
//            ByteBuffer byteBuffer = ByteBuffer.wrap(sendData);
//            byteBuffer.position(0);
//            byteBuffer.put(messageByte);
//            sendData = byteBuffer.array();
//
//        }

    }

}

class PeerFileDownloadListener extends Thread {
    //data members of the class
    private static final int messageSize = 1024;

    private ServerSocket serverSocket;

    PeerNode peerNode = new PeerNode();
    private boolean isServerRunning;
    Socket socket;
    private byte fileInPackets[][];
    /**
     * constructor to handle the server request
     */
    public PeerFileDownloadListener() {

        try {
            serverSocket = new ServerSocket(9495);
            isServerRunning = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    /**
     * method to handle the thread of the class
     */
    public void run() {

        while( isServerRunning ) {

            try {

                socket = serverSocket.accept();

                new FileDownloadHandler(socket).start();

            } catch (IOException e) {

            }

        }


    }
    /**
     * method stop the server
     */
    public void stopServer()
    {
        try {
            serverSocket.close();
        } catch (IOException e) {

        }
        isServerRunning = false;
    }
    /**
     * class to handle the accepted threads of the server
     * Srinath Kanna, Krishna Prasad and Ajeeth Kannan
     *
     */
    private class FileDownloadHandler extends Thread {
        //data members of the class
        Socket socket;

        byte sendData[] = new byte[messageSize];
        byte recvData[] = new byte[messageSize];
        /**
         * constructor of the class
         * @param socket
         */
        public FileDownloadHandler(Socket socket) {

            this.socket = socket;

        }
        /**
         * method to handle the thread of the class
         */
        public void run() {

            try {

                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

                dataInputStream.read(recvData, 0, recvData.length);

                System.out.println("Download Started.");

                String data=new String(recvData);
                String[] fileDetails=data.split(" ");

                peerNode.addfileNames(fileDetails[0]);
                int totalPackets=Integer.parseInt(fileDetails[1]);
                fileInPackets=new byte[totalPackets][messageSize];
                for(int i =0;i<totalPackets;i++)
                {
                    dataInputStream.read(recvData,0,recvData.length);
                    System.arraycopy(recvData, 0 ,fileInPackets[i], 0, messageSize);

                }
                fileWrite(fileDetails[0],totalPackets);

                System.out.println("Download Complete.");

                PeerNode.printOptionsMenu();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        /**
         * method to write the file
         * @param filepath-name of the file
         * @param totalPackets-total number of packets in file
         * @throws IOException
         */
        private void fileWrite(String filepath,int totalPackets) throws IOException
        {
            File file = new File(PeerNode.getMyIP());

            if(!file.exists())
                file.mkdirs();


            FileOutputStream fos = new FileOutputStream(PeerNode.getMyIP() + "/" + String.valueOf(peerNode.hash(filepath)));

            byte[] finalFile = extractFileContents(fileInPackets);
            fos.write(finalFile);

            fos.close();

        }
        /**
         * method to extract packets in file as an array
         * @param fileArray-file as packet array
         * @return-file as a single array
         */
        private byte[] extractFileContents(byte[][] fileArray) {
            List<byte[]> data = new ArrayList<>();

            for (int i = 0; i <= fileArray.length - 1; i++) {
                if(i == fileArray.length - 1) {
                    data.add(new String(fileArray[i]).trim().getBytes());
                    break;
                }
                data.add(fileArray[i]);
            }

            byte[] finalFile = new byte[0];
            for (byte[] b : data) {
                byte[] temp = new byte[finalFile.length + b.length];
                System.arraycopy(finalFile, 0, temp, 0, finalFile.length);
                System.arraycopy(b, 0, temp, finalFile.length, b.length);
                finalFile = temp;
            }
            return finalFile;
        }

    }

}


class PeerFileManager extends Thread {
    //data member of the class
    private static final int messageSize = 1024;
    private File file;
    private static String filePath;
    private long fileSize;
    private int totalPackets;
    private byte[] sendData = new byte[messageSize];
    private byte fileInBytes[];
    private byte fileInPackets[][];

    PeerNode peerNode = new PeerNode();

    Socket socket;

    String sendToIP;
    int sendPortTo = 9495;
    int sendZoneTo;

    String recvIPFrom;
    int recvPortFrom = 9485;
    int recvZoneFrom;
    /**
     * contructor of the class
     */
    public PeerFileManager() {

    }

    /**
     * method to upload the file to the chord
     * @param path-filepath of the file
     * @param share-false to upload a file
     * 			    true to share the file to its peer white join and leave
     */
    public void uploadFile(String path, boolean share) {
        filePath = path;
        if(isFileAvailable(path)){
            String arr[] = path.split("/");
            String filePath_name = arr[arr.length-1];
            sendZoneTo = peerNode.hash(filePath_name);
            System.out.println("File sending to zone :" + sendZoneTo);
            if(!share)
                readFile(!share);
            else
                readFile(share);
            sendToIP = peerNode.getZoneIP(sendZoneTo, peerNode.nearestPeer(sendZoneTo));
            makePackets();
            sendPackets();
        }
        else{
            System.out.println(filePath+" is not available please try again");
            PeerNode.printOptionsMenu();
        }

    }

    public boolean isFileAvailable(String path){
        File f = new File(path);
        if (f.exists() && !f.isDirectory())
            return true;
        return false;
    }
    /**
     * method to download a file from chord
     * @param filePath-name of the file to be downloaded
     * @throws IOException
     */
    public void downloadFile(String filePath) throws IOException {
        System.out.println("Download request formed for file :" + filePath);
//        filePath = "../"+PeerNode.getMyIP()+filePath;
        recvZoneFrom = peerNode.hash(filePath);
        System.out.println("Requesting file from zone: " + recvZoneFrom);
        recvIPFrom = peerNode.getZoneIP(recvZoneFrom, peerNode.nearestPeer(recvZoneFrom));
        socket = new Socket(recvIPFrom, recvPortFrom);

        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

        byte recvData[] = new byte[messageSize];
        File file = new File(PeerNode.getMyIP());
        if(!file.exists())
            file.mkdirs();
        sendData = filePath.getBytes();
        dataOutputStream.write(sendData);
        dataInputStream.read(recvData, 0, recvData.length);
        String data = new String(recvData);

        if (!data.contains("NoSuchFile")) {
            String[] fileDetails = data.split(" ");

            peerNode.addfileNames(fileDetails[0]);
            int totalPackets = Integer.parseInt(fileDetails[1]);
            fileInPackets = new byte[totalPackets][messageSize];
            for (int i = 0; i < totalPackets; i++) {
                dataInputStream.read(recvData, 0, recvData.length);
                System.arraycopy(recvData, 0, fileInPackets[i], 0, messageSize);

            }
            FileOutputStream fos = new FileOutputStream(String.valueOf(peerNode.hash(fileDetails[0])));
            System.out.println("File received.");
            for (int j = 0; j < totalPackets - 1; j++) {
                fos.write((fileInPackets[j]));
            }
            fos.write(new String(fileInPackets[totalPackets - 1]).trim().getBytes());
            fos.close();
            System.out.println("Download complete.");
        }
        else{
            System.out.println(filePath+" not available");
            PeerNode.printOptionsMenu();
        }
    }

    /**
     * method to send the packets to upload
     */
    private void sendPackets() {
        try {
            Socket socket = new Socket(sendToIP, sendPortTo);
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            System.out.println("totalPackets:"+totalPackets);
            String data = new String(new File(filePath).getName() + " " + totalPackets + " ");
            MakeMessage makeMessage = new MakeMessage();
            sendData = makeMessage.message_creation(sendData, messageSize, data);
            dataOutputStream.write(sendData);

            dataOutputStream.flush();
            for (int i = 0; i < totalPackets; i++) {
                dataOutputStream.write(fileInPackets[i]);
            }
            dataOutputStream.flush();
            dataOutputStream.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * method to read the file to be uploaded
     */
    private void readFile(boolean share) {
        if(share)
            file = new File(filePath);
        else
            file = new File("../../" + PeerNode.getMyIP() + "/" + peerNode.hash(filePath));
        try {
            fileInBytes = Files.readAllBytes(file.toPath());

        } catch (IOException e) {

        }
    }
    /**
     * method to make packets for the file to be sent
     */
    private void makePackets() {

        fileSize = file.length();
        totalPackets = (int) Math.ceil(fileSize / (double) messageSize);

        fileInPackets = new byte[totalPackets][messageSize];
        for (int i = 0; i < totalPackets - 1; i++)
            System.arraycopy(fileInBytes, i * messageSize, fileInPackets[i], 0, messageSize);
        System.arraycopy(fileInBytes, (totalPackets - 1) * messageSize, fileInPackets[totalPackets - 1], 0,
                (int) fileSize - (totalPackets - 1) * 1024);

    }
    /**
     * method to wrap the message to be sent to message size
     * @param message-message to be sent
     */
//    private void makeMessage(String message) {
//
//        Arrays.fill(sendData, 0, messageSize, (byte) 0);
//        byte messageByte[] = message.getBytes();
//        ByteBuffer byteBuffer = ByteBuffer.wrap(sendData);
//        byteBuffer.position(0);
//        byteBuffer.put(messageByte);
//        sendData = byteBuffer.array();
//
//    }
}



