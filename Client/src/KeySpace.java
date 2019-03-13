import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * The class is checks whether the requested zone number from a peer
 * is this peer's zone number. The class is a thread which can
 * support multiple clients at a given point.
 */
public class KeySpace extends Thread {

    private static final int messageSize = 64;

    private ServerSocket serverSocket;

    ChordNode chordNode = new ChordNode();
    private boolean isServerRunning;
    Socket socket;

    // constructor
    public KeySpace() {

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

                new KeySpaceHandler(socket).start();

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
     * The class is a supporter class for checkKeySpace. The class gives response
     * to the peer by checking its zone.
     *
     *
     */

    private class KeySpaceHandler extends Thread {

        Socket socket;

        byte sendData[] = new byte[messageSize];
        byte recvData[] = new byte[messageSize];

        // constructor
        public KeySpaceHandler(Socket socket) {

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

                System.out.println("ChordNode.getNodeStart():"+ ChordNode.getNodeStart()+" "+ ChordNode.getNodeEnd());

                if( zoneQuery >= ChordNode.getNodeStart() && zoneQuery <= ChordNode.getNodeEnd() ) {
                    MakeMessage makeMessage = new MakeMessage();
                    sendData = makeMessage.message_creation(sendData, messageSize, "isMyZone", messagePos);
                    dataOutputStream.write(sendData);
                    dataOutputStream.flush();
                }
                else {

                    String nearestIP;

                    if( zoneQuery >= ChordNode.getPredecessor().getNodeStart() &&
                            zoneQuery <= ChordNode.getPredecessor().getNodeEnd() ) {
                        nearestIP = ChordNode.getPredecessor().getIP();
                    }
                    else {
                        nearestIP = chordNode.nearestNode(zoneQuery);
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


    }

}

class NodeDesc extends Thread {

    private static final int messageSize = 1024;

    private ServerSocket serverSocket;
    private boolean isServerRunning;
    Socket socket;

    // constructor
    public NodeDesc() {

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

                new NodeDescHandler(socket).start();

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
     * The class is a supporter class for NodeDesc. The class gives response
     * to the peer by giving details of this peer
     *
     */
    private class NodeDescHandler extends Thread {

        Socket socket;

        String clientIP;

        byte sendData[] = new byte[messageSize];

        // // constructor
        public NodeDescHandler(Socket socket) {

            this.socket = socket;
            clientIP = socket.getInetAddress().toString();
            clientIP = clientIP.substring(1, clientIP.length());

        }

        // thread starts
        public void run() {
            try {

                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                String message1 = ChordNode.getNodeStart() + " " + ChordNode.getNodeEnd() + "\n";
                String message2 = ChordNode.getPredecessor().getIP() + " " + ChordNode.getPredecessor().getNodeStart() + " " +
                        ChordNode.getPredecessor().getNodeEnd() + "\n";
                String message3 = ChordNode.getSuccessor().getIP() + " " + ChordNode.getSuccessor().getNodeStart() + " " +
                        ChordNode.getSuccessor().getNodeEnd() + "\n";
                String message4 = new String("");
                String message5 = ChordNode.isEntryPoint.toString() + "\n";

                for (int i = 0; i < ChordPeerMain.m; i++) {
                    if (i != ChordPeerMain.m - 1) {
                        message4 = message4 + ChordNode.NodeFT.getPeerIP(i) + " ";
                    } else {
                        message4 = message4 + ChordNode.NodeFT.getPeerIP(i) + "\n";
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


    }

}

class KeySpaceManager extends Thread {
    //data members of the class
    private static final int messageSize = 64;

    private ServerSocket serverSocket;

    private ChordNode chordNode = new ChordNode();

    private boolean isServerRunning;
    Socket socket;
    /**
     * constructor of the class to accept server request
     */
    public KeySpaceManager() {

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
                new KeySpaceManagerHandler(socket).start();

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
     *
     */
    private class KeySpaceManagerHandler extends Thread {
        //data member of the class
        Socket socket;

        String clientIP;

        byte sendData[] = new byte[messageSize];
        byte recvData[] = new byte[messageSize];
        /**
         * constructor to handle the accepted threads
         * @param socket
         */
        public KeySpaceManagerHandler(Socket socket) {

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
                    System.out.println("Sharing files");
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
            System.out.println("Files are going to shared with new peer!!!!!!");
            NodeKeyManager fileManager = new NodeKeyManager();
            List<String> deleteList = new ArrayList<>();

            for(String fileName : chordNode.fileNames)
            {
                System.out.println("FILE:"+fileName);
                if(!chordNode.isInMyKeySpace(chordNode.hash(fileName))) {
                    System.out.println("FILE In ZONE:"+fileName);
                    fileManager.uploadFile_new_peer(fileName, true);
                    deleteList.add(fileName);
                }

            }

            chordNode.fileNames.removeAll(deleteList);

        }
        /**
         * method to initiate for node leave
         * @param dataOutputStream
         * @throws IOException
         */
        private void leave(DataOutputStream dataOutputStream) throws IOException {

            String NodeDesc[] = chordNode.getNodeDesc(clientIP);
            boolean isEntryPoint = Boolean.parseBoolean(NodeDesc[4]);

            if(isEntryPoint)
            {

                ChordPeerMain.isEntryPoint = true;
                ChordNode.isEntryPoint = true;
                sendToChordMain();

                String zone[] = NodeDesc[2].split(" ");

                ChordNode.setMyEndpoint(  ChordNode.getNodeStart() , ChordNode.getSuccessor().getNodeEnd());
                ChordNode.setSuccessor(zone[0], Integer.parseInt(zone[1]), Integer.parseInt(zone[2]));

                if(zone[0].contains(ChordNode.getMyIP()))
                {
                    ChordNode.setSuccessor(ChordNode.getMyIP(), ChordNode.getNodeStart(), ChordNode.getNodeEnd());
                    ChordNode.setPredecessor(ChordNode.getMyIP(), ChordNode.getNodeStart(), ChordNode.getNodeEnd());

                }

            }
            else {

                String zone[] = NodeDesc[1].split(" ");

                ChordNode.setMyEndpoint(  ChordNode.getPredecessor().getNodeStart() , ChordNode.getNodeEnd());
                ChordNode.setPredecessor(zone[0], Integer.parseInt(zone[1]), Integer.parseInt(zone[2]));

                if(zone[0].contains(ChordNode.getMyIP()))
                {

                    ChordNode.setSuccessor(ChordNode.getMyIP(), ChordNode.getNodeStart(), ChordNode.getNodeEnd());
                    ChordNode.setPredecessor(ChordNode.getMyIP(), ChordNode.getNodeStart(), ChordNode.getNodeEnd());

                }

            }

            chordNode.sendNeighbourUpdate(ChordNode.getPredecessor().getIP(), 1);
            chordNode.sendNeighbourUpdate(ChordNode.getSuccessor().getIP(), 0);

            ChordNode.NodeFT.runFT();

            ChordNode.NodeFT.updateMyFT();

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
        private void sendToChordMain() throws UnknownHostException, IOException {
            Socket entrySocket = new Socket(  ChordPeerMain.serverIP, 8881 );
            DataOutputStream entryOutputStream = new DataOutputStream(entrySocket.getOutputStream());

            MakeMessage makeMessage = new MakeMessage();
            sendData = makeMessage.message_creation(sendData, messageSize, ChordNode.getMyIP());
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
            int midPoint = ChordNode.getNodeStart() + ((ChordNode.getNodeEnd() - ChordNode.getNodeStart()) / 2);

            if ( ChordNode.getMyIP().equals( ChordNode.getPredecessor().getIP() )
                    && ChordNode.getMyIP().equals( ChordNode.getSuccessor().getIP() ) ) {

                String message1 = ChordNode.getNodeStart() + " " + midPoint + " ";

                ChordNode.setPredecessor(clientIP, ChordNode.getNodeStart(), midPoint);
                ChordNode.setSuccessor(clientIP, ChordNode.getNodeStart(), midPoint);

                ChordNode.setMyEndpoint((midPoint + 1), ChordNode.getNodeEnd());

                String message2 = ChordNode.getMyIP() + " " + ChordNode.getNodeStart() + " " + ChordNode.getNodeEnd() + " " +
                        ChordNode.getMyIP() + " " + ChordNode.getNodeStart() + " " + ChordNode.getNodeEnd();

                MakeMessage makeMessage = new MakeMessage();
                sendData = makeMessage.message_creation(sendData, messageSize, message1 + message2);

            } else {

                String message1 = ChordNode.getNodeStart() + " " + midPoint + " " + ChordNode.getPredecessor().getIP() + " "
                        + ChordNode.getPredecessor().getNodeStart() + " " + ChordNode.getPredecessor().getNodeEnd() + " " ;

                ChordNode.setPredecessor(clientIP, ChordNode.getNodeStart(), midPoint);
                ChordNode.setMyEndpoint((midPoint + 1), ChordNode.getNodeEnd());

                String message2 = ChordNode.getMyIP() + " " + ChordNode.getNodeStart() + " " + ChordNode.getNodeEnd();

                MakeMessage makeMessage = new MakeMessage();
                sendData = makeMessage.message_creation(sendData, messageSize, message1 + message2);

                chordNode.sendNeighbourUpdate(ChordNode.getSuccessor().getIP(), 0);

            }

            dataOutputStream.write(sendData);

            dataOutputStream.flush();

        }


    }

}

class FileDownloadListener extends Thread {
    //data members of the class
    private static final int messageSize = 1024;

    private ServerSocket serverSocket;

    ChordNode chordNode = new ChordNode();
    private boolean isServerRunning;
    Socket socket;
    private byte fileInPackets[][];
    /**
     * constructor to handle the server request
     */
    public FileDownloadListener() {

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

                chordNode.addFiles(fileDetails[0]);
                int totalPackets=Integer.parseInt(fileDetails[1]);
                fileInPackets=new byte[totalPackets][messageSize];
                for(int i =0;i<totalPackets;i++)
                {
                    dataInputStream.read(recvData,0,recvData.length);
                    System.arraycopy(recvData, 0 ,fileInPackets[i], 0, messageSize);

                }
                fileWrite(fileDetails[0],totalPackets);

                System.out.println("Download Complete.");

                ChordNode.printOptionsMenu();

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
            File file = new File(ChordNode.getMyIP());

            if(!file.exists())
                file.mkdirs();


            FileOutputStream fos = new FileOutputStream(ChordNode.getMyIP() + "/" + String.valueOf(chordNode.hash(filepath)));

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


class NodeKeyManager extends Thread {
    //data member of the class
    private static final int messageSize = 1024;
    private File file;
    private static String filePath;
    private long fileSize;
    private int totalPackets;
    private byte[] sendData = new byte[messageSize];
    private byte fileInBytes[];
    private byte fileInPackets[][];

    ChordNode chordNode = new ChordNode();

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
    public NodeKeyManager() {

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
            sendZoneTo = chordNode.hash(filePath_name);
            System.out.println("File sending to zone :" + sendZoneTo);
            if(!share)
                readFile(!share);
            else
                readFile(share);
            sendToIP = chordNode.getKeySpaceIP(sendZoneTo, chordNode.nearestNode(sendZoneTo));
            packetsToCreate();
            packetsToSend();
            System.out.println("Upload complete!");
        }
        else{
            System.out.println(filePath+" is not available please try again");
            ChordNode.printOptionsMenu();
        }

    }

    public void uploadFile_new_peer(String path, boolean share) {
        filePath = path;
        sendZoneTo = chordNode.hash(filePath);
        System.out.println("File sending to zone :" + sendZoneTo);
        if(!share)
            readFile(!share);
        else
            readFile(share);
        sendToIP = chordNode.getKeySpaceIP(sendZoneTo, chordNode.nearestNode(sendZoneTo));
        packetsToCreate();
        packetsToSend();
        System.out.println("Upload complete!");
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
//        filePath = "../"+ChordNode.getMyIP()+filePath;
        recvZoneFrom = chordNode.hash(filePath);
        System.out.println("Requesting file from zone: " + recvZoneFrom);
        recvIPFrom = chordNode.getKeySpaceIP(recvZoneFrom, chordNode.nearestNode(recvZoneFrom));
        socket = new Socket(recvIPFrom, recvPortFrom);

        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

        byte recvData[] = new byte[messageSize];
        File file = new File(ChordNode.getMyIP());
        if(!file.exists())
            file.mkdirs();
        sendData = filePath.getBytes();
        dataOutputStream.write(sendData);
        dataInputStream.read(recvData, 0, recvData.length);
        String data = new String(recvData);

        if (!data.contains("NoSuchFile")) {
            String[] fileDetails = data.split(" ");

            chordNode.addFiles(fileDetails[0]);
            int totalPackets = Integer.parseInt(fileDetails[1]);
            fileInPackets = new byte[totalPackets][messageSize];
            for (int i = 0; i < totalPackets; i++) {
                dataInputStream.read(recvData, 0, recvData.length);
                System.arraycopy(recvData, 0, fileInPackets[i], 0, messageSize);

            }
            FileOutputStream fos = new FileOutputStream(String.valueOf(chordNode.hash(fileDetails[0])));
            System.out.println("File received.");

            for (int j = 0; j <= totalPackets - 1; j++) {
                try{
                    byte[] decodedBytes = java.util.Base64.getDecoder().decode(new String(fileInPackets[j], "UTF-8").trim().getBytes());
                    fos.write((decodedBytes));
                }
                catch (IllegalArgumentException e){
                    fos.write((fileInPackets[j]));
                }
            }
            fos.close();
            System.out.println("Download complete.");
        }
        else{
            System.out.println(filePath+" not available");
            ChordNode.printOptionsMenu();
        }
    }

    /**
     * method to send the packets to upload
     */
    private void packetsToSend() {
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

            if (share) {
                try {
                    boolean numeric = true;
                    numeric = filePath.matches("\\d+(\\.\\d+)?");
                    if (numeric || filePath.contains("/")) {
                        file = new File(filePath);
                    }
                    else {
                        if (isFileAvailable(String.valueOf(chordNode.hash(filePath)))) {
                            file = new File(String.valueOf(chordNode.hash(filePath)));
                        }
                        else {
                            file = new File(ChordNode.getMyIP() + "/" + chordNode.hash(filePath));
                        }
                    }
                }
                catch (Exception e){
                    System.out.println("File is not available");
                }
            }
            else
                file = new File(ChordNode.getMyIP() + "/" + chordNode.hash(filePath));
            try {
                fileInBytes = Files.readAllBytes(file.toPath());

            } catch (IOException e) {
                System.out.println("Exception occured");
            }
    }
    /**
     * method to make packets for the file to be sent
     */
    private void packetsToCreate() {

        fileSize = file.length();
        totalPackets = (int) Math.ceil(fileSize / (double) messageSize);

        fileInPackets = new byte[totalPackets][messageSize];
        for (int i = 0; i < totalPackets - 1; i++)
            System.arraycopy(fileInBytes, i * messageSize, fileInPackets[i], 0, messageSize);
        System.arraycopy(fileInBytes, (totalPackets - 1) * messageSize, fileInPackets[totalPackets - 1], 0,
                (int) fileSize - (totalPackets - 1) * 1024);

    }

}



