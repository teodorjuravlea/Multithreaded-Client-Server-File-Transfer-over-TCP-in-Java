import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

public class Client implements Runnable{
    private final static int SERVICE_PORT = 50001;
    private final static int BUFFER_SIZE = 8192;

    private final String[] args;
    private boolean[] dataset = {true, true, true, true};

    public Client(String[] args){
        this.args = args;
    }

    // Method to run the client directly
    public static void main(String[] args){
        if(args.length < 3){
            System.out.println("Usage: java Client <server_ip> <file_path> <server_destination_path> [options]");
            return;
        }

        Client client = new Client(args);
        client.run();
    }

    // Method to run the client in a thread
    public void run(){
        // Establish connection
        try(Socket socket = new Socket(args[0], SERVICE_PORT)){

            getDataset();
            // Send file data to server
            sendMetadata(socket);
            sendFileContent(socket);

            System.out.println("File sent.");
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    private void getDataset(){
        for(int i = 3; i < args.length; ++i){
            if(args[i].equals("-p") || args[i].equals("--no-permissions"))
                dataset[0] = false;
            if(args[i].equals("-c") || args[i].equals("--no-creation-time"))
                dataset[1] = false;
            if(args[i].equals("-a") || args[i].equals("--no-access-time"))
                dataset[2] = false;
            if(args[i].equals("-m") || args[i].equals("--no-modified-time"))
                dataset[3] = false;
        }
    }

    private String parseDestinationPath(){
        return args[2] + args[1].substring(args[1].lastIndexOf("/") + 1);
    }

    private void sendMetadata(Socket socket){
        // Create object output stream from the socket's output stream
        ObjectOutputStream sendStream;
        try{
            sendStream = new ObjectOutputStream(socket.getOutputStream());
        }
        catch(IOException e){
            throw new RuntimeException(e);
        }

        // Send file path
        try{
            sendStream.writeObject(parseDestinationPath());
        }
        catch(IOException e){
            throw new RuntimeException(e);
        }

        MetadataObject sendMetadata;

        // Send file permissions
        if(dataset[0]){
            try{
                sendMetadata = new MetadataObject(Files.getPosixFilePermissions(Paths.get(args[1])), "file_perms");
                sendStream.writeObject(sendMetadata);
            }
            catch(IOException e){
                System.out.println("Couldn't send file permissions.");
            }
        }

        // Read file attributes
        BasicFileAttributes fileAttributes = null;
        try{
            fileAttributes = Files.readAttributes(Paths.get(args[1]), BasicFileAttributes.class);
        }
        catch(IOException e){
            System.out.println("Couldn't read file attributes.");
            dataset[1] = false;
            dataset[2] = false;
            dataset[3] = false;
        }

        // Send file attributes
        if(dataset[1]){
            sendMetadata = new MetadataObject(fileAttributes.creationTime().toMillis(), "file_creation_time");
            try{
                sendStream.writeObject(sendMetadata);
            }
            catch(IOException e){
                System.out.println("Couldn't send file creation time.");
            }
        }

        if(dataset[2]){
            sendMetadata = new MetadataObject(fileAttributes.lastAccessTime().toMillis(), "file_access_time");
            try{
                sendStream.writeObject(sendMetadata);
            }
            catch(IOException e){
                System.out.println("Couldn't send file last access time.");
            }
        }

        if(dataset[3]){
            sendMetadata = new MetadataObject(fileAttributes.lastModifiedTime().toMillis(), "file_modified_time");
            try{
                sendStream.writeObject(sendMetadata);
            }
            catch(IOException e){
                System.out.println("Couldn't send file last modified time.");
            }
        }

        sendMetadata = new MetadataObject("metadata_sent");
        try{
            sendStream.writeObject(sendMetadata);
        }
        catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    private void sendFileContent(Socket socket){
        // Establish connection by providing address and port
        try{
            // Get the socket's output stream
            OutputStream sendStream = socket.getOutputStream();

            // Create an input stream from file given as argument
            try(InputStream myFileInputStream = Files.newInputStream(Paths.get(args[1]))){

                // Declare buffer
                byte[] buffer = new byte[BUFFER_SIZE];

                int readBytes;
                // Read file data into buffer and send it to server until all is sent
                while((readBytes = myFileInputStream.read(buffer)) > 0){
                    sendStream.write(buffer, 0, readBytes);
                }
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }
}