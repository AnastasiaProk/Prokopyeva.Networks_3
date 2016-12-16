import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class Data {
    private static final int BUF_SIZ = 8 * 1024 * 1024;
    private static final float BUF_LIMIT = BUF_SIZ * 0.6f;

    private ByteBuffer buffer;
    private FileOutputStream fileOut;
    private boolean isFileOpened = false;
    private long fileSize;
    private long readBytes;

    public Data() {
        buffer = ByteBuffer.allocate(BUF_SIZ);
    }

    public Server.Result readData(SocketChannel socket) {
        try {
            int readBytes = socket.read(buffer);
            System.err.println(readBytes);
            if (readBytes == -1) {
                writeData();
                if (checkFile()) {
                    return Server.Result.SUCCESS;
                } else {
                    return Server.Result.ERROR;
                }
            } else if (buffer.position() > BUF_LIMIT) { //зачем?
                writeData();
            }
        } catch (IOException e) {
            System.err.println(e.toString());
            try {
                if(fileOut != null) {
                    fileOut.close();
                }

            } catch (IOException e1) {
                System.err.println("Error closing file");
                e1.printStackTrace();
            }
            return Server.Result.ERROR;
        }

        return Server.Result.NO_ACTION;
    }

    public void writeData() throws IOException {
        int endPosition = buffer.position();
        buffer.position(0);

        if (!isFileOpened) {
            fileSize = buffer.getLong();
            int nameLength = buffer.getInt();
            byte[] tempByteName = new byte[nameLength];
            buffer.get(tempByteName);
            String tempStringName = new String(tempByteName, Charset.forName("UTF-8"));
            File dir = new File("uploads");
            if (!dir.canExecute()) { //что это для директории??
                if(!dir.mkdir()){ //кидает ли исключения?
                    throw new IOException("не удалось создать dir");
                }
            }
            File file = new File(dir, tempStringName);
            if (file.createNewFile()) {
                System.err.println("Created new file, started writing data");
            } else {
                System.err.println("File already exists and it will be renewed");
            }
            fileOut = new FileOutputStream(file);
            isFileOpened = true;
            /*
            String fileName = "./uploads/" + tempStringName;
            System.err.println("ttt");
            File file = new File(fileName);
            System.err.println(fileName);
            if (file.createNewFile()) {
                System.err.println("Created new file, started writing data");
            } else {
                System.err.println("File already exists and it will be renewed");
            }
            fileOut = new FileOutputStream(file.getName());
            isFileOpened = true;
            */
        }
        //System.err.println("start write");
        fileOut.write(buffer.array(), buffer.position(), endPosition - buffer.position());
        //System.err.println("write "+ (endPosition - buffer.position()));
        readBytes += endPosition - buffer.position();
        //System.err.println("finish write");
        buffer.clear();
    }

    public boolean checkFile() throws IOException {
        fileOut.close();
        if (readBytes != fileSize) {
            return false;
        } else {
            return true;
        }
    }
}
