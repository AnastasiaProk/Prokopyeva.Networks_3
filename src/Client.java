import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by Anastasia on 17.11.16.
 */
public class Client {
    private static final int NAME_LENGTH = 1024;
    private static final int BUF_SIZE = 1024;
    String fileName;
    int port;
    InetAddress ip;

    Client(InetAddress ip, int port, String fileName){
        this.ip = ip;
        this.port = port;
        this.fileName = fileName;
    }
    public void mainFunction() throws IOException {
//
//        if (args.length != 3) {
//            System.err.println("Enter file name, ip and port");
//            System.exit(-1);
//        }
//
//        String fileName = args[0];
//        String ip = args[1];
//        int port = Integer.parseInt(args[2]);

        Socket socket = new Socket(ip, port);

        File file = new File(fileName);
        long fileSize = file.length();
        //System.out.println(fileSize);
        InputStream fileIn = new FileInputStream(file);

        ByteBuffer buffer = ByteBuffer.allocate(NAME_LENGTH);
        buffer.putLong(fileSize);
        buffer.putInt(file.getName().getBytes(Charset.forName("UTF-8")).length);
        buffer.put(file.getName().getBytes(Charset.forName("UTF-8")));

        InputStream serverIn = socket.getInputStream();
        OutputStream serverOut = socket.getOutputStream();
        serverOut.write(buffer.array(), 0, buffer.position());

        byte[] tempBuff = new byte[BUF_SIZE];
        int length = fileIn.read(tempBuff);

        while (length != -1) {
            serverOut.write(tempBuff, 0, length);
            length = fileIn.read(tempBuff);
        }

        socket.shutdownOutput();

        // ждем от сервера ответ о результате передачи файла;
        // так как сообщение может прийти не полностью
        // (например, если соединение оборвется),
        // ответ может быть воспринят клиентом неверно,
        // поэтому ждем, пока придет весь int до конца, то есть 4 байта
        int readBytes = 0;
        ByteBuffer resultBuffer = ByteBuffer.allocate(4);
        while (readBytes < 4) {
            length = serverIn.read(resultBuffer.array());
            if (length == -1) {
                System.err.println("Error getting resultBuffer");
                System.exit(-1);
            }
            readBytes += length;
        }

        // теперь можем читать int, убедившись, что он полностью передан
        int result = resultBuffer.getInt();
        if (result == 0) {
            System.out.println("File was successfully sent");
        } else {
            System.err.println("Error sending file: " + result);
        }

        socket.close();
    }

    public static void main(String[] args) {
        try {
            Client client = new Client(InetAddress.getLocalHost(), Server.PORT, "file");
            client.mainFunction();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

