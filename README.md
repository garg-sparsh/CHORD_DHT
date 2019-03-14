# CHORD_DHT
Implementation of DHT using CHORD protocol and some concepts of CAN

Final Project
  
CHORD-DHT A LIBRARY SYSTEM

Steps to run the program:
1. Find the environment with multiple machines or create it on aws/gcp
2. Install jre on each machine: sudo yum install java-1.8.0-openjdk
3. Install jdk on each machine: sudo yum install java-1.8.0-openjdk-devel
4. Compile all the classes in all the machines by using the command

`sudo javac /home/sparshgarg56/CHORD_DHT/Client/src/*.java`

`sudo javac /home/sparshgarg56/CHORD_DHT/server/src/*.java`

5. Run Server: java ChordMain
6. Run peer nodes on other available machines: java ChordPeerMain <IP of server>
7. Choose one of the 5 actions to perform on each of the node:
   
   `1 - Finger Table Details`
  
   `2 - Leave chord`
   
   `3 - Upload File`
   
   `4 - Download File`
   
   `5 - Files present at this node`

9. if user selects 1: Displays finger table for the current node
        
   if user selects 2: Node will be removed from the chord and all the files it possess will be transferred to it’s predecessor
        However, if the node is an entry point, leader election occurs and its predecessor is assigned as chord’s new entry         point
   
   if user selects 3: User will be prompted to upload a file from the node [Example: /home/WebServices-Tutorial.pdf]
   
   if user selects 4: User will be prompted to enter file name that user wants to be downloaded in the current node
        [Example: WebServices- Tutorial.pdf]
        
   if user selects 5: Lists all the files available at the current node.


References:

[1]Ion Stoicay, Robert Morrisz, David Liben-Nowellz, David R.Kargerz, M.Frans Kaashoekz, Frank Dabekz, Hari Balakrishnanz, “Chord:A Scalable Peer to peer Lookup Protocol for Internet Applications”, M.S. thesis, University of Massachusetts,June 2002

[2]https://medium.com/@siddontang/build-up-a-high-availability-distributed-key-value-store-b4e02bc46e9e

[3]https://ayende.com/blog/3827/rhino-dht-concurrency-handling-example-the-phone-billing-system

[4]https://www.geeksforgeeks.org/socket-programming-in-java/

[5]https://github.com/srinivasmaram/KeyValueStore1

[6]https://github.com/krishprasadar/CHORD_DHT

[7]https://github.com/savoirfairelinux/opendht

[8]https://docs.oracle.com/javase/8/docs/api/

[9]https://www.usenix.org/legacy/publications/library/proceedings/osdi2000/full_papers/gribble/gribble_html/node4.html

[10]https://hub.packtpub.com/distributed-hash-tables/

