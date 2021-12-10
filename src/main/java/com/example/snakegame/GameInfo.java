package com.example.snakegame;

import com.example.snakegame.net.NodeInfo;
import me.ippolitov.fit.snakes.SnakesProto;

import java.net.SocketAddress;
import java.util.List;

public class GameInfo {
    private List<NodeInfo> nods;
    private long lastTimeMsg;
    private SnakesProto.GameConfig config;
    private SocketAddress masterAddress;
    public GameInfo(List<NodeInfo> nods, long lastTimeMsg, SnakesProto.GameConfig config) {
        this.nods = nods;
        this.lastTimeMsg = lastTimeMsg;
        this.config = config;
    }

    public List<NodeInfo> getNods() {
        return nods;
    }

    public void setNods(List<NodeInfo> nods) {
        this.nods = nods;
    }

    public long getLastTimeMsg() {
        return lastTimeMsg;
    }

    public SnakesProto.GameConfig getConfig() {
        return config;
    }


    public SocketAddress getMasterAddress() {
        return masterAddress;
    }

    public void setMasterAddress(SocketAddress masterAddress) {
        this.masterAddress = masterAddress;
    }

    public void setLastTimeMsg(long lastTimeMsg) {
        this.lastTimeMsg = lastTimeMsg;
    }
}
