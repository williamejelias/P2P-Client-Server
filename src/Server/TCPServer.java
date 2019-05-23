package Server;

import java.net.*;
import java.io.*;

public class TCPServer extends Thread {
	
	private ServerSocket serverSocket;
	
	private DataInputStream in;
	private DataOutputStream out;
	
	private Socket server;
   
	public TCPServer(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		// Set timeout to 100 seconds
		serverSocket.setSoTimeout(100000);
	}
	
	public static void main(String[] args) {
		int port = 5000;
		try {
			Thread t = new TCPServer(port);
			t.start();
		} catch (BindException e) {
			// address already in use
			System.out.println("Address is already in use...");
		} catch (IOException e) {
			// client dropped connection unexpectedly
			e.printStackTrace();
		}
	}

	/* 
	 * start server listening
	 */
	public void run() {
		while(true) {
			try {
				// wait until client is accepted
				System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "...");
				server = serverSocket.accept();
            
	            System.out.println("Just connected to " + server.getRemoteSocketAddress());
	            
	            in = new DataInputStream(server.getInputStream());
	            out = new DataOutputStream(server.getOutputStream());
	            
	            System.out.println(in.readUTF() + "\n");
	            sendMessage("Thank you for connecting to " + server.getLocalSocketAddress());
	            
	            connection:
	            	while (true) {
	            		switch (in.readUTF()) {
	            		case "UPLD":
	            			// UPLD
	            			System.out.println("UPLD COMMAND RECIEVED");
	            			receiveFile();
	            			break;
	            		case "DWLD":
	            			// DWLD
	            			System.out.println("DWLD COMMAND RECIEVED");
	            			sendFile();
	            			break;
	            		case "DELF":
	            			// DELF
	            			System.out.println("DELF COMMAND RECIEVED");
	            			deleteFile();
	            			break;
	            		case "LIST":
	            			// LIST
	            			System.out.println("LIST COMMAND RECIEVED");
	            			listFiles();
	            			break;
	            		case "QUIT":
	            			// QUIT
	            			System.out.println("Client closing socket connection...");
	            			System.out.println("Connection terminated.\n");
		            		quit();
		    	            break connection;
		    	        default:
		    	        		// SHOULDNT HAPPEN
		    	        		System.out.println("UNKNOWN COMMAND RECIEVED");
		            		System.out.println();
	            		}
	            	}
	            
	            // close socket connection
	            server.close();  
			} catch (SocketTimeoutException s) {
				System.out.println("Socket timed out!");
				break;
			} catch (IOException e) {
				e.printStackTrace();
				//break;
			}
		}
	}
	
	/*
	 * sends the list of files
	 */
	public void listFiles() {
		try {
			System.out.println("Getting list bytestream...");
			byte[] stream = getFilesList().getBytes();
			int size = stream.length;
			// Extract size of data to be sent back
			// send the size as a 32bit int
			System.out.println("Sending size int...");
			out.writeInt(size);

			// send the bytes of the directory listing
			System.out.println("Sending listing...");
			System.out.println("End of method.\n");
			String t  = getFilesList();
			System.out.println(t);
			out.writeBytes(t);
		} catch (IOException e) {
			e.printStackTrace();
			//break;
		}
	}
	
	/*
	 * gets a list of files in the files directory as a string
	 */
	public String getFilesList() {
		System.out.println("Working Directory = " +
				System.getProperty("user.dir"));
		try {
			System.out.println(new File(".").getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		File folder = new File("./Server/files/");
		File[] listOfFiles = folder.listFiles();
		StringBuilder list = new StringBuilder();
		if (listOfFiles != null) {
			list.append("Files found: ").append(listOfFiles.length).append("\n");
			for (File listOfFile : listOfFiles) {
				list.append(listOfFile.getName()).append("\n");
			}
			return list.toString();
		} else {
			return "Files found: 0\n";
		}
	}

	/*
	 * receive an uploaded file from the client and write to a new file
	 */
	public void receiveFile() {
		try {
			// receive int from client
			int size = in.readInt();
			
			// receive filename from client
			String filename = in.readUTF();
			
			System.out.println("Filename recieved: " + filename);
			System.out.println("Filename size recieved: " + size);
			// check int received is equal to length of the filename
			if (size == filename.length()) {
				System.out.println("Sending ACK...");
				// if this is the case, send an ACK
				sendMessage("ACK");
				
				// receive size of file to be received
				int filesize = in.readInt();
				System.out.println("Size of file to be received: " + filesize + " bytes");
				
				// receive file
				byte[] mybytearray = new byte[filesize];
			    FileOutputStream fos = new FileOutputStream("./Server/files/" + filename);
			    BufferedOutputStream bos = new BufferedOutputStream(fos);
			    System.out.println("Recieving file...");
			    InputStream input = server.getInputStream();
			    int bytesRead = input.read(mybytearray, 0, mybytearray.length);
			    int current = bytesRead;
			    
			    do {
		    		bytesRead = input.read(mybytearray, current, (mybytearray.length - current));
		    		if (bytesRead >= 0) {
		    			current += bytesRead;
		    		}
			    } while (current < filesize);
			    
			    // write file
			    System.out.println("Writing file...");
			    bos.write(mybytearray, 0, current);
			    bos.close();
			    
			    System.out.println("Upload complete.");
			} else {
				System.out.println("Sending ERR...");
				sendMessage("ERR");
			}
			
			// receive the file from the client
			// display upload stats
			System.out.println("End of method.\n");
			// go back to listening state
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * receive a filename from the client and delete it if the file exists
	 */
	public void deleteFile() {
		try {
			// receive int from client
			int size = in.readInt();
			
			// receive filename from client
			String filename = in.readUTF();
			
			System.out.println("Filename recieved: " + filename);
			System.out.println("Filename size revieved: " + size);
			
			// check if file exists
			File fileToDelete = new File("./Server/files/" + filename);
			boolean exists = fileToDelete.exists();
			
			if (exists) {
				System.out.println("File exists - sending 1...");
				// if exists, send 1
				out.writeInt(1);
				
				// receive yes or no from client to confirm delete
				String clientResponse = in.readUTF();
				if (clientResponse.equals("Yes")) {
					// delete is confirmed
					System.out.println("Client confirmed delete");
					
					// send delete success or failure message to client 
					if (fileToDelete.delete()){
		    				System.out.println("Successfully deleted " + fileToDelete.getName() +  "!");
		    				out.writeInt(1);
		    			} else {
		    				System.out.println("Delete operation failed.");
		    				out.writeInt(-1);
		    			}
					// back to listening state
				} else if (clientResponse.equals("No")) {
					// delete is abandoned
					System.out.println("Client abandoned delete...");
					// return to waiting state
				}
			} else {
				// if doesnt exist send -1
				System.out.println("File does not exist - sending -1...");
				out.writeInt(-1);
			}
			
			System.out.println("End of method.\n");
			// go back to listening state
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * send a requested file back to the client
	 */
	public void sendFile() {
		try {
			// receive int from client
			int size = in.readInt();
			
			// receive filename from client
			String filename = in.readUTF();
			
			System.out.println("Filename received: " + filename);
			System.out.println("Filename size received: " + size);
			
			// check if file exists
			File fileToSend = new File("./Server/files/" + filename);
			boolean exists = fileToSend.exists();
			
			if (exists) {
				// if file exists, send size of file
				System.out.println("File exists - sending file size...");
				out.writeInt((int)fileToSend.length());
				
				// send file
				byte[] mybytearray = new byte[(int) fileToSend.length()];
				System.out.println("Transferring " + mybytearray.length + " bytes.");

				// transfer 
				long startTime = System.nanoTime();
				
				BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileToSend));
				int read = bis.read(mybytearray, 0, mybytearray.length);
				out.write(mybytearray, 0, mybytearray.length);
				out.flush();
				bis.close();

				long endTime = System.nanoTime();
				long duration = (endTime - startTime) / 1000000;
				System.out.println("Transferred " + mybytearray.length + " bytes in " + duration + " ms.");

				System.out.println("File sent.");
				// display stats
			} else {
				System.out.println("File does not exist - sending -1...");
				out.writeInt(-1);
				// back to listening state
			}
			System.out.println("End of method.\n");
			// go back to listening state
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Function to quit the server
	 */
	public void quit() {
		try {  
			in.close();
			out.close();
	        server.close(); 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * writes a utf string to the server - used for calling the required operation on the server
	 */
	public void sendMessage(String message) {
		try {
			out.writeUTF(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}