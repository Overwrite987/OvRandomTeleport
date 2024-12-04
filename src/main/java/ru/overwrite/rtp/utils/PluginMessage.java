package ru.overwrite.rtp.utils;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import ru.overwrite.rtp.Main;
import ru.overwrite.rtp.RtpManager;

public final class PluginMessage implements PluginMessageListener {

    private final Main plugin;
    private final RtpManager rtpManager;
    @Getter
    private final String serverId;

    public PluginMessage(Main plugin, String serverId) {
        this.plugin = plugin;
        this.rtpManager = plugin.getRtpManager();
        this.serverId = serverId;
    }

    public void onPluginMessageReceived(String channel, @NotNull Player player, byte[] message) {
        if (!channel.equals("BungeeCord"))
            return;
        ByteArrayDataInput input = ByteStreams.newDataInput(message);
        String subchannel = input.readUTF();
        if (subchannel.equalsIgnoreCase("ovrtp")) {
            String data = input.readUTF();
            int spaceIndex = data.indexOf(' ');
            String serverId = data.substring(0, spaceIndex);
            System.out.println(serverId);
            if (!this.serverId.equals(serverId)) {
                return;
            }
            String teleportData = data.substring(spaceIndex + 1);
            int separatorIndex = teleportData.indexOf('$');
            String playerName = teleportData.substring(0, separatorIndex);
            String teleportInfo = teleportData.substring(separatorIndex + 1);
            rtpManager.getProxyCalls().put(playerName, teleportInfo);
        }
    }

    public void sendCrossProxy(Player player, String data) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF("ovrtp");
        out.writeUTF(data);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public void connectToServer(Player player, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }
}

