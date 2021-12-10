package com.example.snakegame.net;

import com.example.snakegame.GameInfo;
import com.example.snakegame.MoveDirection;
import com.example.snakegame.ui.GameController;
import me.ippolitov.fit.snakes.SnakesProto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class NormalNode extends AbstractNode {
    private NodeInfo master;
    private SnakesProto.GameState lastState;

    public NormalNode() throws IOException {
        name = "myname";
    }


    public Map<SocketAddress, GameInfo> getGames() {
        return gamesInfo;
    }

    public void startListenGames() {
        service.scheduleAtFixedRate(() -> {
            byte[] buf = new byte[65536];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                multicastSocket.receive(packet);
                byte[] buf2 = Arrays.copyOf(packet.getData(), packet.getLength());
                SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.parseFrom(buf2);
                SnakesProto.GameMessage.AnnouncementMsg announcementMsg = gameMessage.getAnnouncement();

                SnakesProto.GamePlayers players = announcementMsg.getPlayers();
                List<SnakesProto.GamePlayer> gamePlayerList = players.getPlayersList();
                List<NodeInfo> nodeInfos = gamePlayerList.stream().map(p -> new NodeInfo(p.getName(),
                        p.getId(),
                        packet.getAddress(),
                        packet.getPort(),
                        getNodeRole(p.getRole()))).collect(Collectors.toList());
                gamesInfo.compute(packet.getSocketAddress(), (g, k) -> new GameInfo(nodeInfos, System.currentTimeMillis(), announcementMsg.getConfig()));
                gamesInfo.get(packet.getSocketAddress()).setMasterAddress(packet.getSocketAddress());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void setController(GameController controller) {
        this.gameController = controller;
    }

    public void joinGame(SocketAddress socketAddress, GameController controller) {
        SnakesProto.GameMessage.JoinMsg joinMsg = SnakesProto.GameMessage.JoinMsg.newBuilder().setName("myname").build();
        SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder().setJoin(joinMsg).setMsgSeq(seq.getAndIncrement()).build();
        DatagramPacket packet = new DatagramPacket(gameMessage.toByteArray(), gameMessage.toByteArray().length, socketAddress);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        GameInfo gameInfo = gamesInfo.get(socketAddress);
        config = gameInfo.getConfig();
        for (NodeInfo nodeInfo : gameInfo.getNods()) {
            if (nodeInfo.getNodeRole() == NodeRole.MASTER) {
                config = gameInfo.getConfig();
                System.out.println(config);
                master = nodeInfo;
                master.setLastSeenOnline(System.currentTimeMillis());
                nodes.put(new InetSocketAddress(master.getAddress(), master.getPort()), master);
                break;
            }
        }
    }


    @Override
    public void run() {
        startAcknowledgeService();
        listen();
    }

    @Override
    protected void startAcknowledgeService() {
        service.scheduleAtFixedRate(() -> {
            try {
                System.out.println(role);
                if ((System.currentTimeMillis() - master.getLastSeenOnline() > config.getNodeTimeoutMs()) && role == NodeRole.DEPUTY) {
                    try {
                        shutdown();
                        new MasterNode(this, lastState).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (System.currentTimeMillis() - master.getLastSeenOnline() > config.getPingDelayMs()) {
                    SnakesProto.GameMessage.PingMsg pingMsg = SnakesProto.GameMessage.PingMsg.newBuilder().build();
                    SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder().setPing(pingMsg).setMsgSeq(seq.getAndIncrement()).build();
                    DatagramPacket packet = new DatagramPacket(gameMessage.toByteArray(), gameMessage.toByteArray().length, master.getAddress(), master.getPort());
                    AcknowledgeWait acknowledgeWait = new AcknowledgeWait(System.currentTimeMillis(), packet, gameMessage);
                    master.getAcknowledgeWaits().add(acknowledgeWait);
                    try {
                        socket.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                AcknowledgeWait acknowledgeWait = master.getAcknowledgeWaits().peek();
                if (acknowledgeWait != null) {
                    if (System.currentTimeMillis() - acknowledgeWait.getTime() > config.getPingDelayMs()) {
                        try {
                            acknowledgeWait.getPacket().setSocketAddress(new InetSocketAddress(master.getAddress(),master.getPort()));
                            socket.send(acknowledgeWait.getPacket());
                            acknowledgeWait.setTime(System.currentTimeMillis());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, config.getPingDelayMs(), config.getPingDelayMs(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void setTurn(MoveDirection direction) {
        SnakesProto.GameMessage.SteerMsg steerMsg = SnakesProto.GameMessage.SteerMsg.newBuilder().setDirection(toProtoDirection(direction)).build();
        SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder().setSteer(steerMsg).setMsgSeq(seq.getAndIncrement()).build();
        DatagramPacket packet = new DatagramPacket(gameMessage.toByteArray(), gameMessage.toByteArray().length, master.getAddress(),master.getPort());
        AcknowledgeWait acknowledgeWait = new AcknowledgeWait(System.currentTimeMillis(), packet, gameMessage);
        master.getAcknowledgeWaits().add(acknowledgeWait);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected DatagramPacket processSteer(SnakesProto.GameMessage message, DatagramPacket packet) {
        return null;
    }

    @Override
    protected DatagramPacket processState(SnakesProto.GameMessage message, DatagramPacket packet) {
        master.setLastSeenOnline(System.currentTimeMillis());
        SnakesProto.GameState state = message.getState().getState();
        lastState = state;
        gameController.update(state);
        SnakesProto.GameMessage.AckMsg ackMsg = SnakesProto.GameMessage.AckMsg.newBuilder().build();
        SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder().setAck(ackMsg).setMsgSeq(message.getMsgSeq()).build();
        return new DatagramPacket(gameMessage.toByteArray(), gameMessage.toByteArray().length, master.getAddress(),master.getPort());
    }

    @Override
    protected DatagramPacket processJoin(SnakesProto.GameMessage message, DatagramPacket packet) {
        return null;
    }

    @Override
    protected DatagramPacket processError(SnakesProto.GameMessage message, DatagramPacket packet) {
        master.setLastSeenOnline(System.currentTimeMillis());
        return null;
    }

    @Override
    protected DatagramPacket processRole(SnakesProto.GameMessage message, DatagramPacket packet) {
        master.setLastSeenOnline(System.currentTimeMillis());
        SnakesProto.GameMessage.RoleChangeMsg roleChangeMsg = message.getRoleChange();
        if (getNodeRole(roleChangeMsg.getSenderRole()) == NodeRole.MASTER) {
            if (getNodeRole(roleChangeMsg.getReceiverRole()) == NodeRole.NORMAL) {
                for (SnakesProto.GamePlayer player : lastState.getPlayers().getPlayersList()) {
                    if (player.getId() == message.getSenderId()) {
                        System.out.println("set new master");
                        master = new NodeInfo(player.getName(), message.getSenderId(), packet.getAddress(), packet.getPort(), NodeRole.MASTER);
                        nodes.put(packet.getSocketAddress(),master);
                        master.setLastSeenOnline(System.currentTimeMillis());
                        break;
                    }
                }
            } else if (getNodeRole(roleChangeMsg.getReceiverRole()) == NodeRole.DEPUTY) {
                role = NodeRole.DEPUTY;
            } else if (getNodeRole(roleChangeMsg.getReceiverRole()) == NodeRole.VIEWER) {
                role = NodeRole.VIEWER;
            }
        }
        SnakesProto.GameMessage.AckMsg ackMsg = SnakesProto.GameMessage.AckMsg.newBuilder().build();
        SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder().setAck(ackMsg).setMsgSeq(message.getMsgSeq()).build();
        return new DatagramPacket(gameMessage.toByteArray(), gameMessage.toByteArray().length, master.getAddress(),master.getPort());
    }
}
