package com.example.test1;

import com.gamesparks.sdk.android.GSAndroidPlatform;
import com.gamesparks.sdk.realtime.*;

/**
 * Created by perniola on 3/13/18.
 */

public class GameRTSession implements IRTSessionListener {
    public boolean ready = false;
    public RTSession mSession;

    private long oldCycle = -1;

    public GameRTSession(String connectToken, String host, int port) {
        int index;
        String theHost;

        index = host.indexOf(":", 0);
        if (index >= 0) {
            theHost = host.substring(0, host.indexOf(":", 0));
        } else {
            theHost = host;
        }

        GSAndroidPlatform.gs().getGSPlatform().logMessage(theHost + " : " + port);

        mSession = GameSparksRT.getSession(connectToken, theHost, port, port, this);
        if (mSession != null) {
            mSession.start();
        }
    }

    public void stop() {
        ready = false;

        if (mSession != null) {
            mSession.stop();
            mSession = null;
        }
    }

    private void log(String message) {
        StringBuilder peers = new StringBuilder("|");

        for (int i = 0; i < mSession.getActivePeers().size(); i ++) {
            peers.append(mSession.getActivePeers().get(i) + "|");
        }

        GSAndroidPlatform.gs().getGSPlatform().logMessage(mSession.getPeerId() + ": " + message + " peers: " + peers);
    }

    @Override
    public void onPlayerConnect(int peerId) {
        log(" OnPlayerConnect:" + peerId);
    }

    @Override
    public void onPlayerDisconnect(int peerId) {
        log(" OnPlayerDisconnect:" + peerId);

        ready = false;
    }

    @Override
    public void onReady(boolean ready) {
        log(" OnReady:" + ready);

        this.ready = ready;
    }

    @Override
    public void onPacket(RTPacket packet) {
        log(" OnPacket:" + packet.toString());

        if (packet.opCode == 1) {
            if (packet.data == null || packet.data.getLong(1) != oldCycle + 1) {
                if (packet.data == null) {
                    GSAndroidPlatform.gs().getGSPlatform().logError(new Exception("ERROR: Cycle skipped - missing packet"));

                    oldCycle ++;
                } else {
                    GSAndroidPlatform.gs().getGSPlatform().logError(new Exception("ERROR: Cycle skipped - malformed packet (" + packet.data.getLong(1) + " expected " + (oldCycle + 1) + ")"));

                    oldCycle = packet.data.getLong(1);
                }
            } else {
                oldCycle = packet.data.getLong(1);
            }
        }
    }
}
