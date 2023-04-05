import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public class Server{
    private final static int SERVICE_PORT = 50001;
    private final static int BUFFER_SIZE = 8192;

    private static String ROOT_PATH;

    public static void main(String[] args){
        if(args.length < 1){
            System.out.println("Usage: java Server <root_path>");
            return;
        }
        ROOT_PATH = args[0];

        try(ServerSocket serverSocket = new ServerSocket(SERVICE_PORT)){
            // Server is listening on the given port
            serverSocket.setReuseAddress(true);

           int clientCount = 0;

            while(true){
                // Accept client connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client #" + clientCount + " connected - " + clientSocket.getInetAddress().getHostAddress());

                // Create a new thread object and start it
                ConnectionHandler client = new ConnectionHandler(clientSocket, clientCount);
                new Thread(client).start();

                ++clientCount;
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    private static class ConnectionHandler implements Runnable{
        private final Socket clientSocket;
        private String filePathString;
        private Set<PosixFilePermission> filePermissions;
        private FileTime fileCreationTime, fileLastAccessTime, fileLastModifiedTime;
        private boolean[] dataset = {false, false, false, false};

        private final int clientId;

        public ConnectionHandler(Socket socket, int clientId){
            this.clientSocket = socket;
            this.clientId = clientId;
        }

        public void run(){
            try{

                // Receive file metadata and content
                receiveMetadata();
                receiveFileContent(filePathString);

                Path filePath = Paths.get(filePathString);

                // Set file permissions
                if(dataset[0])
                    Files.setPosixFilePermissions(filePath, filePermissions);

                // Set file attributes
                if(dataset[1])
                    Files.setAttribute(filePath, "creationTime", fileCreationTime);
                if(dataset[2])
                    Files.setAttribute(filePath, "lastAccessTime", fileLastAccessTime);
                if(dataset[3])
                    Files.setAttribute(filePath, "lastModifiedTime", fileLastModifiedTime);

                System.out.println("Received file from client #" + clientId + " - " + filePath);
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }

        private void receiveMetadata(){
            try{
                // Create object input stream from the socket's input stream
                ObjectInputStream receiveStream = new ObjectInputStream(clientSocket.getInputStream());

                // Receive new file path
                String path = (String) receiveStream.readObject();

                // Determine new file absolute path
                this.filePathString = ROOT_PATH + path;

                MetadataObject receivedMetadata;
                boolean stop = false;

                while(!stop){
                    receivedMetadata = (MetadataObject) receiveStream.readObject();

                    if(receivedMetadata.type.equals("file_perms")){
                        filePermissions = receivedMetadata.perms;
                        dataset[0] = true;
                    }
                    if(receivedMetadata.type.equals("file_creation_time")){
                        fileCreationTime = FileTime.fromMillis(receivedMetadata.time);
                        dataset[1] = true;
                    }
                    if(receivedMetadata.type.equals("file_access_time")){
                        fileLastAccessTime = FileTime.fromMillis(receivedMetadata.time);
                        dataset[2] = true;
                    }
                    if(receivedMetadata.type.equals("file_modified_time")){
                        fileLastModifiedTime = FileTime.fromMillis(receivedMetadata.time);
                        dataset[3] = true;
                    }
                    if(receivedMetadata.type.equals("metadata_sent")){
                        stop = true;
                    }
                }

            }
            catch(IOException | ClassNotFoundException e){
                e.printStackTrace();
            }
        }

        private void receiveFileContent(String path){
            try{
                // Get byte input stream from socket
                InputStream receiveStream = clientSocket.getInputStream();

                // Create directories in give path that don't exist
                Files.createDirectories(Paths.get(path.substring(0, path.lastIndexOf("/"))));

                // Create new file and get its byte output stream
                OutputStream myFileOutputStream = Files.newOutputStream(Paths.get(path));

                // Declare buffer
                byte[] buffer = new byte[BUFFER_SIZE];

                int receivedBytes;
                // Read data from socket into buffer and write it into new file until all was received
                while ((receivedBytes = receiveStream.read(buffer)) > 0) {
                    myFileOutputStream.write(buffer, 0, receivedBytes);
                }

                // Close file byte output stream
                myFileOutputStream.close();
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}