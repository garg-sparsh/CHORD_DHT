import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

//The class is used to checks if the keyspace  which gets requested from a node has the
// same key space, to maintain the consistency multiple threads gets created.
public class KeySpace extends Thread {

    private ServerSocket nodePortSocket;

    ChordNode chordNode = new ChordNode();
    private boolean isNodePortSocketRunning;
    Socket socket;

    // constructor
    public KeySpace() {

        try {
            nodePortSocket = new ServerSocket(Constant.CONNECT_NODE_PORT);
            isNodePortSocketRunning = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // thread starts
    public void run() {

        while( isNodePortSocketRunning ) {

            try {
                socket = nodePortSocket.accept();
                new KeySpaceHandler(socket).start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

   //stops the nodePortSocket
    public void stopServer()
    {
        try {
            nodePortSocket.close();
        } catch (IOException e) {

        }
        isNodePortSocketRunning = false;
    }

    //helper class of KeySpace as it gets back to the node with results after investigating the keyspace
    private class KeySpaceHandler extends Thread {

        Socket socket;

        byte recvData[] = new byte[Constant.MESSAGE_SIZE];
        byte sendData[] = new byte[Constant.MESSAGE_SIZE];

        // constructor
        public KeySpaceHandler(Socket socket) {

            this.socket = socket;

        }
        // thread starts
        public void run() {

            try {

                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                dataInputStream.read(recvData, 0, recvData.length);

                int query = Integer.parseInt( new String( recvData ).trim() );

                if( query >= ChordNode.getNodeStart() && query <= ChordNode.getNodeEnd() ) {
                    MakeMessage makeMessage = new MakeMessage();
                    sendData = makeMessage.message_creation(sendData, Constant.MESSAGE_SIZE, "isInMySpace");
                    dataOutputStream.write(sendData);
                    dataOutputStream.flush();
                }
                else {

                    String nearestIP;

                    if( query >= ChordNode.getPredecessor().getNodeStart() &&
                            query <= ChordNode.getPredecessor().getNodeEnd() ) {
                        nearestIP = ChordNode.getPredecessor().getIP();
                    }
                    else {
                        nearestIP = chordNode.nearestNode(query);
                    }
                    MakeMessage makeMessage = new MakeMessage();
                    sendData = makeMessage.message_creation(sendData, Constant.MESSAGE_SIZE, nearestIP);
                    dataOutputStream.write(sendData);
                    dataOutputStream.flush();
                }


            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }

}

//The class is used to get the details of the node.
// Details like keyspace start, keyspace end, predeccessor, successor and entrypoint details
// multithreading has been implemented to maintain the consistency
class NodeDesc extends Thread {

    private ServerSocket serverSocket;
    private boolean isServerRunning;
    Socket socket;

    // constructor
    public NodeDesc() {

        try {
            isServerRunning = true;
            serverSocket = new ServerSocket(Constant.NEIGHBOUR_lISTENER);
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

    private class NodeDescHandler extends Thread {

        Socket socket;

        String clientIP;

        byte sendData[] = new byte[Constant.FILE_MSG_SIZE];

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

                for (int i = 0; i < Constant.M; i++) {
                    if (i !=Constant.M - 1) {
                        message4 = message4 + ChordNode.NodeFT.getPeerIP(i) + " ";
                    } else {
                        message4 = message4 + ChordNode.NodeFT.getPeerIP(i) + "\n";
                    }
                }
                MakeMessage makeMessage = new MakeMessage();
                sendData = makeMessage.message_creation(sendData, Constant.FILE_MSG_SIZE, message1 + message2 + message3 + message4 + message5);
                dataOutputStream.write(sendData);
                dataOutputStream.flush();

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }

}

//This class is used to perform the add and join the leave of the nodes in the chord network
class KeySpaceManager extends Thread {

    private ServerSocket serverSocket;

    private ChordNode chordNode = new ChordNode();

    private boolean isServerRunning;
    Socket socket;

    public KeySpaceManager() {

        try {
            serverSocket = new ServerSocket(Constant.KEY_SPACE_MANAGER_PORT);
            isServerRunning = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void run() {

        while (isServerRunning) {

            try {

                socket = serverSocket.accept();
                new KeySpaceManagerHandler(socket).start();

            } catch (IOException e) {

            }

        }

    }

    public void stopServer() {
        try {
            serverSocket.close();
        } catch (IOException e) {

        }
        isServerRunning = false;
    }

    private class KeySpaceManagerHandler extends Thread {
        //data member of the class
        Socket socket;

        String clientIP;

        byte sendData[] = new byte[Constant.MESSAGE_SIZE];
        byte recvData[] = new byte[Constant.MESSAGE_SIZE];

        public KeySpaceManagerHandler(Socket socket) {

            this.socket = socket;
            clientIP = socket.getInetAddress().toString();
            clientIP = clientIP.substring(1, clientIP.length());

        }

        public void run() {
            try {

                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                dataInputStream.read(recvData, 0, recvData.length);

                String message = new String(recvData).trim();

                if (message.contains("add")) {
                    add(dataOutputStream);

                    try {
                        Thread.sleep(Constant.THREAD_SLEEP_TIME);
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

        //method which checks files at the original location if they fall in the keyspace
        // of the newly created node and then share those files with that nde
        private void shareFilesToNewPeer() {
            NodeKeyManager fileManager = new NodeKeyManager();
            List<String> deleteList = new ArrayList<>();

            for(String fileName : chordNode.fileNames)
            {
                System.out.println("FILE:"+fileName);
                if(!chordNode.isInMyKeySpace(chordNode.hash(fileName))) {
                    System.out.println("FILE at Node:"+fileName);
                    fileManager.uploadFile_new_peer(fileName, true);
                    deleteList.add(fileName);
                }

            }

            chordNode.fileNames.removeAll(deleteList);

        }

        //update details when the chord leaves. Details include: Finger table, predecessor, successor and keyspace
        private void leave(DataOutputStream dataOutputStream) throws IOException {

            String NodeDesc[] = chordNode.getNodeDesc(clientIP);
            boolean isEntryPoint = Boolean.parseBoolean(NodeDesc[4]);

            if(isEntryPoint)
            {

                ChordPeerMain.isEntryPoint = true;
                ChordNode.isEntryPoint = true;
                sendToChordMain();

                String key[] = NodeDesc[2].split(" ");

                ChordNode.setMyEndpoint(  ChordNode.getNodeStart() , ChordNode.getSuccessor().getNodeEnd());
                ChordNode.setSuccessor(key[0], Integer.parseInt(key[1]), Integer.parseInt(key[2]));

                if(key[0].contains(ChordNode.getMyIP()))
                {
                    ChordNode.setSuccessor(ChordNode.getMyIP(), ChordNode.getNodeStart(), ChordNode.getNodeEnd());
                    ChordNode.setPredecessor(ChordNode.getMyIP(), ChordNode.getNodeStart(), ChordNode.getNodeEnd());

                }

            }
            else {

                String key[] = NodeDesc[1].split(" ");

                ChordNode.setMyEndpoint(  ChordNode.getPredecessor().getNodeStart() , ChordNode.getNodeEnd());
                ChordNode.setPredecessor(key[0], Integer.parseInt(key[1]), Integer.parseInt(key[2]));

                if(key[0].contains(ChordNode.getMyIP()))
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
            sendData = makeMessage.message_creation(sendData, Constant.MESSAGE_SIZE, "Confirmed");

            dataOutputStream.write(sendData);
            dataOutputStream.flush();
            dataOutputStream.close();

        }

        //send details about entry point to server
        private void sendToChordMain() throws IOException {
            Socket entrySocket = new Socket(  ChordPeerMain.serverIP, Constant.ENTRY_lISTENER_PORT );
            DataOutputStream entryOutputStream = new DataOutputStream(entrySocket.getOutputStream());

            MakeMessage makeMessage = new MakeMessage();
            sendData = makeMessage.message_creation(sendData, Constant.MESSAGE_SIZE, ChordNode.getMyIP());
            entryOutputStream.write(sendData);
            entryOutputStream.flush();
            entrySocket.close();
        }

        //joins new node to the ring
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
                sendData = makeMessage.message_creation(sendData, Constant.MESSAGE_SIZE, message1 + message2);

            } else {

                String message1 = ChordNode.getNodeStart() + " " + midPoint + " " + ChordNode.getPredecessor().getIP() + " "
                        + ChordNode.getPredecessor().getNodeStart() + " " + ChordNode.getPredecessor().getNodeEnd() + " " ;

                ChordNode.setPredecessor(clientIP, ChordNode.getNodeStart(), midPoint);
                ChordNode.setMyEndpoint((midPoint + 1), ChordNode.getNodeEnd());

                String message2 = ChordNode.getMyIP() + " " + ChordNode.getNodeStart() + " " + ChordNode.getNodeEnd();

                MakeMessage makeMessage = new MakeMessage();
                sendData = makeMessage.message_creation(sendData, Constant.MESSAGE_SIZE, message1 + message2);

                chordNode.sendNeighbourUpdate(ChordNode.getSuccessor().getIP(), 0);

            }

            dataOutputStream.write(sendData);

            dataOutputStream.flush();

        }


    }

}

// this class is responsible for downloading the requested files from the node which has that
// file . If multiple nodes have the same file then it will pick that from the nearest IP(node).
class FileDownloadListener extends Thread {

    private ServerSocket serverSocket;

    ChordNode chordNode = new ChordNode();
    private boolean isServerRunning;
    Socket socket;
    private byte fileInPackets[][];

    public FileDownloadListener() {

        try {
            serverSocket = new ServerSocket(Constant.UPLOAD_PORT);
            isServerRunning = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void run() {

        while( isServerRunning ) {

            try {

                socket = serverSocket.accept();

                new FileDownloadHandler(socket).start();

            } catch (IOException e) {

            }

        }


    }
    //stop server
    public void stopServer()
    {
        try {
            serverSocket.close();
        } catch (IOException e) {

        }
        isServerRunning = false;
    }

    private class FileDownloadHandler extends Thread {

        Socket socket;

        byte sendData[] = new byte[Constant.FILE_MSG_SIZE];
        byte recvData[] = new byte[Constant.FILE_MSG_SIZE];

        public FileDownloadHandler(Socket socket) {

            this.socket = socket;

        }

        public void run() {

            try {

                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

                dataInputStream.read(recvData, 0, recvData.length);

                System.out.println("Download Started.");

                String data=new String(recvData);
                String[] fileDetails=data.split(" ");

                chordNode.addFiles(fileDetails[0]);
                int totalPackets=Integer.parseInt(fileDetails[1]);
                fileInPackets=new byte[totalPackets][Constant.FILE_MSG_SIZE];
                for(int i =0;i<totalPackets;i++)
                {
                    dataInputStream.read(recvData,0,recvData.length);
                    System.arraycopy(recvData, 0 ,fileInPackets[i], 0, Constant.FILE_MSG_SIZE);

                }
                fileWrite(fileDetails[0],totalPackets);

                System.out.println("Download Complete.");

                ChordNode.menu();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

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

//this class is used to take care of all the requests generated for uploading and downloading the file
class NodeKeyManager extends Thread {

    private File file;
    private static String filePath;
    private long fileSize;
    private int totalPackets;
    private byte[] sendData = new byte[Constant.FILE_MSG_SIZE];
    private byte fileInBytes[];
    private byte fileInPackets[][];

    ChordNode chordNode = new ChordNode();

    Socket socket;

    String sendToIP;
    int sendPortTo = Constant.UPLOAD_PORT;
    int sendKeyValue;

    String recvIPFrom;
    int recvPortFrom = Constant.DOWNLOAD_PORT;
    int receiveKeyValueFrom;

    public NodeKeyManager() {

    }

    public void uploadFile(String path, boolean share) {
        filePath = path;
        if(isFileAvailable(path)){
            String arr[] = path.split("/");
            String filePath_name = arr[arr.length-1];
            sendKeyValue = chordNode.hash(filePath_name);
            System.out.println("File sending to node :" + sendKeyValue);
            if(!share)
                readFile(!share);
            else
                readFile(share);
            sendToIP = chordNode.getKeySpaceIP(sendKeyValue, chordNode.nearestNode(sendKeyValue));
            packetsToCreate();
            packetsToSend();
            System.out.println("Upload complete!");
        }
        else{
            System.out.println(filePath+" is not available please try again");
            ChordNode.menu();
        }

    }

    public void uploadFile_new_peer(String path, boolean share) {
        filePath = path;
        sendKeyValue = chordNode.hash(filePath);
        System.out.println("File sending to key :" + sendKeyValue);
        if(!share)
            readFile(!share);
        else
            readFile(share);
        sendToIP = chordNode.getKeySpaceIP(sendKeyValue, chordNode.nearestNode(sendKeyValue));
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

    public void downloadFile(String filePath) throws IOException {
        System.out.println("Download request formed for file :" + filePath);
        receiveKeyValueFrom = chordNode.hash(filePath);
        System.out.println("Requesting file from key: " + receiveKeyValueFrom);
        recvIPFrom = chordNode.getKeySpaceIP(receiveKeyValueFrom, chordNode.nearestNode(receiveKeyValueFrom));
        socket = new Socket(recvIPFrom, recvPortFrom);

        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

        byte recvData[] = new byte[Constant.FILE_MSG_SIZE];
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
            fileInPackets = new byte[totalPackets][Constant.FILE_MSG_SIZE];
            for (int i = 0; i < totalPackets; i++) {
                dataInputStream.read(recvData, 0, recvData.length);
                System.arraycopy(recvData, 0, fileInPackets[i], 0, Constant.FILE_MSG_SIZE);

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
            ChordNode.menu();
        }
    }

    private void packetsToSend() {
        try {
            Socket socket = new Socket(sendToIP, sendPortTo);
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            System.out.println("totalPackets:"+totalPackets);
            String data = new String(new File(filePath).getName() + " " + totalPackets + " ");
            MakeMessage makeMessage = new MakeMessage();
            sendData = makeMessage.message_creation(sendData, Constant.FILE_MSG_SIZE, data);
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

    private void packetsToCreate() {

        fileSize = file.length();
        totalPackets = (int) Math.ceil(fileSize / (double) Constant.FILE_MSG_SIZE);

        fileInPackets = new byte[totalPackets][Constant.FILE_MSG_SIZE];
        for (int i = 0; i < totalPackets - 1; i++)
            System.arraycopy(fileInBytes, i * Constant.FILE_MSG_SIZE, fileInPackets[i], 0, Constant.FILE_MSG_SIZE);
        System.arraycopy(fileInBytes, (totalPackets - 1) * Constant.FILE_MSG_SIZE, fileInPackets[totalPackets - 1], 0,
                (int) fileSize - (totalPackets - 1) * Constant.FILE_MSG_SIZE);

    }

}



