import java.io.Serializable;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public class MetadataObject implements Serializable{
    Set<PosixFilePermission> perms;
    long time;
    String type;
    public MetadataObject(Set<PosixFilePermission> perms, String type){
        this.perms = perms;
        this.type = type;
    }
    public MetadataObject(long time, String type){
        this.time = time;
        this.type = type;
    }
    public MetadataObject(String type){
        this.type = type;
    }
}
