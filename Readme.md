# Introduction

A simple CLI client-server file sharing system implemented in Java for a 2nd Year assignment as part of my Master Degree in Computer Science.

## Usage

The connection between the client and server is made over socket with port 5000. The server will timeout after 100 seconds if it does not receive a connection request within that time.

To start the client:

```
cd src
javac */*.java
java Client.TCPClient
```

To start the server:
```
cd src
javac */*.java
java Server.TCPServer
```

The client has a simple CLI with several basic commands
* CONN - connect to the server
* LIST - list the available files for download from the server
* DWLD - download a file (input the name of the file after calling)
* DELF - delete a file from the server (input the name of the file after calling)
* QUIT - quit the client

Files are stored in the local `files` folder within the `Client` and `Server` packages within `src`.

