import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class Server {
    public static int PORT = 14204;
    private static Selector selector;
    int port;

    public Server(int port){
        this.port = port;
    }

    // структура для передачи результата клиенту;
    // NO_ACTION - не передается клиенту, а используется в случае,
    // когда еще не произошло сверки принятого и посчитанного размеров файла
    public enum Result {
        ERROR, SUCCESS, NO_ACTION
    }

    public void mainFunction() {
//        if (args.length != 1) {
//            System.err.println("Enter port");
//            System.exit(-1);
//        }

       // int port = Integer.parseInt(args[0]);

        try {
            selector = Selector.open();

            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            for (;;) {
                selector.select();

                Set keys = selector.selectedKeys();
                Iterator it = keys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = (SelectionKey) it.next();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        acceptConnection(key);
                    } else if (key.isReadable()) {
                        readFromChannel(key);
                    } else if (key.isWritable()) {
                        writeToChannel(key);
                    }
                }
                keys.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void acceptConnection(SelectionKey key) {
        ServerSocketChannel serverSocket = (ServerSocketChannel) key.channel();

        try {
            SocketChannel clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getRemoteAddress());
            clientSocket.configureBlocking(false);
            clientSocket.register(selector, SelectionKey.OP_READ).attach(new Data());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readFromChannel(SelectionKey key) {
        if (key.attachment() != null) {
            try {
                Result result = ((Data)key.attachment()).readData((SocketChannel) key.channel());
                if (result != Result.NO_ACTION) {
                    ByteBuffer buffer = ByteBuffer.allocate(4);
                    if (result == Result.SUCCESS) {
                        buffer.putInt(0);
                    } else if (result == Result.ERROR) {
                        buffer.putInt(1);
                    }

                    buffer.position(0);
                    key.channel().register(selector, SelectionKey.OP_WRITE).attach(buffer);
                }
            } catch (ClosedChannelException e) {
                System.err.println("Error sending the result to client");
                key.cancel();
                try {
                    key.channel().close();
                } catch (IOException e1) {
                    System.err.println("Error closing client channel");
                }
            }
        }
    }

    private void writeToChannel(SelectionKey key) {
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        try {
            ((SocketChannel) key.channel()).write(buffer);
        } catch (IOException e) {
            System.err.println("Error sending the result to client");
        }

        if (buffer.position() == 4) {
            try {
                (key.channel()).close();
            } catch (IOException e) {
                System.err.println("Error closing channel");
            }
            key.cancel();
        }
    }

    public static void main(String[] args) {

        Server server = new Server(Server.PORT);
        server.mainFunction();
    }
}
