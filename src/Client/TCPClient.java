package Client;

import java.net.*;
import java.util.Scanner;
import java.io.*;

public class TCPClient {
	
	private String serverName;
	private int port;
	private Socket client;

	private OutputStream outToServer;
	private DataOutputStream out;
	
	private InputStream inFromServer;
	private DataInputStream in;
	
	private Scanner selectScanner;
	private Scanner uploadScanner;
	private Scanner deleteScanner;
	
	public TCPClient(String serverName, int port) {
		// Host 127.0.0.1
		this.serverName = serverName;
		// Port 5000
		this.port = port;	
	}
	
	public static void main(String[] args) {
		TCPClient client = new TCPClient("127.0.0.1", 5000);
		Scanner select = new Scanner(System.in);
		System.out.println("Welcome to the Client Application.");
		System.out.println("Connect to the server to list, upload, download and delete files.\n");
		client.printCommands();
		
		menu:
		while (true) {
			switch (select.next()) {
			case "CONN": 
    				// CONN SELECTED 
				client.CONN();
				select.close();
				break menu;
			case "QUIT":
				// QUIT SELECTED
	    			System.out.println("Closing Client Application...");
	    			select.close();
				break menu;
			default:
				System.out.println("Unrecognized command!");
				client.printCommands();
			}
		}
	}
	
	public void sendMessage(String message) {
		try {
			out.writeUTF(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Function to make connection to server localhost at port 5000
	 */
	public void CONN() {
		try {
			// Initialise socket
			System.out.println("\nConnecting to " + serverName + " on port " + port + "...");
			InetAddress address = InetAddress.getByName(serverName);
			client = new Socket(address, port);
	 			
			// Initialise output stream
			outToServer = client.getOutputStream();
			out = new DataOutputStream(outToServer);
			
			// Initialise input stream from server
			inFromServer = client.getInputStream();
			in = new DataInputStream(inFromServer);

			// send Hello message to server
			sendMessage("Hello from " + client.getLocalSocketAddress());
			
			// receive thank you message from server
			System.out.println(in.readUTF() + "\n");
			System.out.println("Connected to " + client.getRemoteSocketAddress());

			selectScanner = new Scanner(System.in);
			connMenu:
			while (true) {
				printConnCommands();
				switch(selectScanner.next().trim()) {
				case "UPLD":
					uploadFile();
					break;
				case "LIST":
					listFiles();
					break;
				case "DWLD":
					downloadFile();
					break;
				case "DELF":
					deleteFile();
					break;
				case "QUIT":
					sendMessage("QUIT");
					quit();
					selectScanner.close();
					break connMenu;
				default: 
					System.out.println("\nCommand not recognised.\n");
				}
			} 
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	/*
	 * Upload a file to the server
	 */
	public void uploadFile() {
		try {
			System.out.println("Enter name of file to be uploaded: ");
			
			// get filename of file to be uploaded
			uploadScanner = new Scanner(System.in);
			String filename = uploadScanner.next().trim();
			
			// get length of filename
			int filenameLength = filename.length();

			// check that the file exists - if it does send the encoded filename and length
			File uploadFile = new File("./Client/files/" + filename);
			if (uploadFile.exists() && !uploadFile.isDirectory()) { 

				sendMessage("UPLD");
			    // file exists and isn't a directory, so upload
				// send filename length as a 32 bit int
				out.writeInt(filenameLength);
				
				// send the filename
				out.writeUTF(filename);
				
				// receive ACK or ERR from server
				String response = in.readUTF();
				if (response.equals("ACK")) {
					System.out.println("Server ACK Received");
					// upload file using fileoutputstream
					// create bytearray to upload
					int filesize = (int)uploadFile.length();
					out.writeInt(filesize);
					System.out.println("Filesize of upload: " + filesize);
					
					byte[] mybytearray = new byte[filesize];
					System.out.println("Transferring " + mybytearray.length + " bytes.");
					
					// transfer 
					long startTime = System.nanoTime();
					
					BufferedInputStream bis = new BufferedInputStream(new FileInputStream(uploadFile));
					bis.read(mybytearray, 0, mybytearray.length);
					OutputStream os = client.getOutputStream();
					os.write(mybytearray, 0, mybytearray.length);
					os.flush();
					bis.close();

					long endTime = System.nanoTime();
					long duration = (endTime - startTime) / 1000000;
					System.out.println("Transferred " + mybytearray.length + " bytes in " + duration + " ms.");

					System.out.println("Upload complete.\n");
						
				} else if (response.equals("ERR")) {
					System.out.println("Server ERR Received.");
					System.out.println("Server could not decode filename and filename size...\n");
					// back to listening state
				}
			} else {
				// file does not exist, so back to listening state
				// tell server to quit upload
				System.out.println("File does not exist...\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * Delete selected file from the server
	 */
	public void deleteFile() {
		sendMessage("DELF");
		try {
			System.out.println("Enter name of file to be deleted: ");
			
			// get filename of file to be uploaded
			deleteScanner = new Scanner(System.in);
			String filename = deleteScanner.next().trim();
			
			// get length of filename
			int filenameLength = filename.length();
			
			// send filename length as a 32 bit int
			out.writeInt(filenameLength);
			
			// send the filename
			out.writeUTF(filename);
			
			// get 1 or -1 response from server
			int response = in.readInt();
			if (response == 1) {
				// file exists
				yesOrNo:
				while (true) {
					System.out.println("Confirm delete - Yes/No");
					switch (deleteScanner.next().trim()) {
					case "Yes":
					case "yes":
					case "y":
					case "Y":
						// confirm to server
						sendMessage("Yes");
						
						// receive 1 or -1 to for successful/failed delete
						int successFail = in.readInt();
						if (successFail == 1 ) {
							// delete success
							System.out.println("File successfully deleted from server!\n");
						} else if (successFail == -1) {
							// delete failure
							System.out.println("Server error - delete operation failed...\n");
						}
						
						// back to listening state
						break yesOrNo;
					case "No":
					case "no":
					case "n":
					case "N":
						// confirm to server
						sendMessage("No");
						
						// delete abandoned
						System.out.println("Delete abandoned by the user!\n");
						
						// back to listening state
						break yesOrNo;
					default:
						System.out.println("Unrecognized command!\n");
					}
				}
			} else if (response == -1) {
				// file does not exist
				System.out.println("The file does not exist on the server.\n");
				// back to listening state
			} else {
				System.out.println("Unknown response received...\n");
				// should not happen
				// back to listening state
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * List all files on the server
	 */
	public void listFiles() {
		try {
			sendMessage("LIST");
			
			BufferedReader d = new BufferedReader(new InputStreamReader(inFromServer));
			
			// extract size of listing to be received
			int size = in.readInt();
			System.out.println("Size of listing text in bytes: " + size);
			System.out.println("Receiving files list...");
			
			// the listing string
			String listing = d.readLine();
			System.out.println(listing);
			int numberOfFiles = Integer.parseInt("" + listing.charAt(listing.length() - 1));
			for (int i = 0; i < numberOfFiles; i ++) {
				System.out.println(d.readLine());
			}
			
			System.out.println();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Download the selected file from the server
	 */
	public void downloadFile() {
		sendMessage("DWLD");
		try {
			System.out.println("Enter name of file to be downloaded: ");
			
			// get filename of file to be downloaded
			uploadScanner = new Scanner(System.in);
			String filename = uploadScanner.next().trim();
			
			// get length of filename
			int filenameLength = filename.length();
			
			// send filename length as a 32 bit int
			out.writeInt(filenameLength);
			
			// send the filename
			out.writeUTF(filename);
			
			// receive size of file if exists, else -1 as an int from server
			int size = in.readInt();
			if (size == -1) {
				System.out.println("File does not exist.");
				// server found that file does not exist
			
				// back to listening state
			} else {
				System.out.println("File exists with size: " + size + " bytes.");
				// file exists, decode size and receive/ write file with same name
				
				// receive file
				byte[] mybytearray = new byte[size];
				
				long startTime = System.nanoTime();

			    FileOutputStream fos = new FileOutputStream("./Client/files/" + filename);
			    
			    BufferedOutputStream bos = new BufferedOutputStream(fos);
			    System.out.println("Receiving file...");
			    InputStream input = client.getInputStream();

				int bytesRead = input.read(mybytearray, 0, mybytearray.length);
			    int current = bytesRead;
			    do {
		    		bytesRead = input.read(mybytearray, current, (mybytearray.length - current));
		    		if (bytesRead >= 0) {
		    			current += bytesRead;
		    		}
			    } while (current < size);
			    
			    // write file
			    System.out.println("Writing file...");
			    bos.write(mybytearray, 0, current);
			    bos.close();
			    
			    long endTime = System.nanoTime();
				long duration = (endTime - startTime) / 1000000;
				System.out.println("Received " + size + " bytes in " + duration + " ms.");
				
			    System.out.println("Download complete.\n");
				// back to listening state
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Function to quit the client and terminate the connection to the server
	 */
	public void quit() throws IOException {
		try {
			sendMessage("QUIT");
			System.out.println("Closing socket connection...");
			try {
				uploadScanner.close();
			} catch (NullPointerException e) {
				// ignore - scanner wasn't initialized
			}
			try {
				deleteScanner.close();
			} catch (NullPointerException e) {
				// ignore - scanner wasn't initialized
			}
			client.close();
			out.close();
			in.close();
			System.out.println("Connection Terminated\n");
		} catch (IOException ex) {
			// Ignore as client connection is being terminated
		}
	}

	/*
	 * list commands available at client initialization, connect/quit
	 */
	public void printCommands() {
		System.out.println("Commands: ");
		System.out.println("CONN");
		System.out.println("QUIT\n");
	}
	
	/*
	 * list command available after connection, upload, delete, download, list, quit
	 */
	public void printConnCommands() {
		System.out.println("Commands: ");
		System.out.println("UPLD");
		System.out.println("LIST");
		System.out.println("DWLD");
		System.out.println("DELF");
		System.out.println("QUIT\n");
	}
}