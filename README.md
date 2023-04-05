# Multithreaded Client-Server File Transfer over TCP in Java

This project contains a multithreaded server that receives files from clients and stores them in a directory, and a client that sends files to the server, over TCP.
The files keep, by default, some of their attributes (such as permissions, creation time, last modification time and last accessed time) when they are transferred.
The client can be ran directly, or as a thread in a different program, as a Runnable object.

## Usage
Server:
```
java Server <root_path>
```

Client:
```
java Client <server_ip> <file_path> <server_destination_path> [optional_arguments]
```

## Optional Client Arguments
```
-p | --no-permissions - do not keep file permissions
-c | --no-creation-time - do not keep file creation time
-a | --no-access-time - do not keep file last accessed time
-m | --no-modified-time - do not keep file last modified time
```