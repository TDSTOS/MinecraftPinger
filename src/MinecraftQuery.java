import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public record MinecraftQuery(String serverIp, int serverPort) {
    private static final int TIMEOUT = 5000;
    private static final byte HANDSHAKE = 9;
    private static final byte STAT = 0;

    public QueryResponse query() throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT);
            InetAddress address = InetAddress.getByName(serverIp);

            int challengeToken = performHandshake(socket, address);
            return performFullStat(socket, address, challengeToken);
        }
    }

    private int performHandshake(DatagramSocket socket, InetAddress address) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(7);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 0xFEFD);
        buffer.put(HANDSHAKE);
        buffer.putInt(1);

        DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.array().length, address, serverPort);
        socket.send(packet);

        byte[] receiveData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);

        ByteBuffer response = ByteBuffer.wrap(receivePacket.getData());
        response.order(ByteOrder.BIG_ENDIAN);
        response.get();
        response.getInt();

        byte[] challengeBytes = new byte[receivePacket.getLength() - 5];
        response.get(challengeBytes);

        String challengeStr = new String(challengeBytes).trim();
        return Integer.parseInt(challengeStr);
    }

    private QueryResponse performFullStat(DatagramSocket socket, InetAddress address, int challengeToken) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(15);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 0xFEFD);
        buffer.put(STAT);
        buffer.putInt(1);
        buffer.putInt(challengeToken);
        buffer.putInt(0);

        DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.array().length, address, serverPort);
        socket.send(packet);

        byte[] receiveData = new byte[4096];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);

        return parseFullStat(receivePacket.getData(), receivePacket.getLength());
    }

    private QueryResponse parseFullStat(byte[] data, int length) {
        QueryResponse response = new QueryResponse();

        int pos = 16;

        while (pos < length) {
            String key = readNullTerminatedString(data, pos);
            pos += key.length() + 1;

            if (key.isEmpty()) {
                pos += 10;
                break;
            }

            String value = readNullTerminatedString(data, pos);
            pos += value.length() + 1;

            switch (key) {
                case "hostname":
                    response.setMotd(value);
                    break;
                case "gametype":
                    response.setGameType(value);
                    break;
                case "game_id":
                    response.setGameId(value);
                    break;
                case "version":
                    response.setVersion(value);
                    break;
                case "plugins":
                    response.setPlugins(value);
                    break;
                case "map":
                    response.setMap(value);
                    break;
                case "numplayers":
                    response.setOnlinePlayers(Integer.parseInt(value));
                    break;
                case "maxplayers":
                    response.setMaxPlayers(Integer.parseInt(value));
                    break;
            }
        }

        while (pos < length) {
            String playerName = readNullTerminatedString(data, pos);
            pos += playerName.length() + 1;

            if (playerName.isEmpty()) {
                break;
            }

            response.addPlayer(playerName);
        }

        return response;
    }

    private String readNullTerminatedString(byte[] data, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < data.length && data[i] != 0; i++) {
            sb.append((char) data[i]);
        }
        return sb.toString();
    }

    public static boolean isQueryEnabled(String serverIp, int serverPort) {
        try {
            MinecraftQuery query = new MinecraftQuery(serverIp, serverPort);
            query.query();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
