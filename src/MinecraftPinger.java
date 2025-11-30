import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MinecraftPinger {
    private String serverIp;
    private int serverPort;
    private static final int PROTOCOL_VERSION = 47;
    private static final int TIMEOUT = 5000;

    public MinecraftPinger(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public String ping() throws IOException {
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(TIMEOUT);
            socket.connect(new InetSocketAddress(serverIp, serverPort), TIMEOUT);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            sendHandshake(out);
            sendStatusRequest(out);

            return readResponse(in);
        }
    }

    private void sendHandshake(DataOutputStream out) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream handshake = new DataOutputStream(buffer);

        writeVarInt(handshake, 0x00);
        writeVarInt(handshake, PROTOCOL_VERSION);
        writeString(handshake, serverIp);
        handshake.writeShort(serverPort);
        writeVarInt(handshake, 1);

        writeVarInt(out, buffer.size());
        out.write(buffer.toByteArray());
        out.flush();
    }

    private void sendStatusRequest(DataOutputStream out) throws IOException {
        writeVarInt(out, 1);
        writeVarInt(out, 0x00);
        out.flush();
    }

    private String readResponse(DataInputStream in) throws IOException {
        int length = readVarInt(in);
        int packetId = readVarInt(in);

        if (packetId != 0x00) {
            throw new IOException("Invalid packet ID: " + packetId);
        }

        int jsonLength = readVarInt(in);
        byte[] jsonBytes = new byte[jsonLength];
        in.readFully(jsonBytes);

        return new String(jsonBytes, "UTF-8");
    }

    private void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & 0xFFFFFF80) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value & 0x7F);
    }

    private int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            currentByte = in.readByte();
            value |= (currentByte & 0x7F) << position;

            if ((currentByte & 0x80) == 0) {
                break;
            }

            position += 7;

            if (position >= 32) {
                throw new IOException("VarInt is too big");
            }
        }

        return value;
    }

    private void writeString(DataOutputStream out, String string) throws IOException {
        byte[] bytes = string.getBytes("UTF-8");
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }
}
