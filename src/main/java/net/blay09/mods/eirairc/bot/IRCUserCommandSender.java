package net.blay09.mods.eirairc.bot;

import net.blay09.mods.eirairc.api.irc.IRCChannel;
import net.blay09.mods.eirairc.api.irc.IRCUser;
import net.minecraft.command.CommandResultStats;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class IRCUserCommandSender implements ICommandSender {

    private final IRCChannel channel;
    private final IRCUser user;
    private final boolean broadcastResult;
    private final boolean opEnabled;
    private final String outputFilter;

    public IRCUserCommandSender(IRCChannel channel, IRCUser user, boolean broadcastResult, boolean opEnabled, String outputFilter) {
        this.channel = channel;
        this.user = user;
        this.broadcastResult = broadcastResult;
        this.opEnabled = opEnabled;
        this.outputFilter = outputFilter;
    }

    @Override
    public String getName() {
        return "[EiraIRC] " + user.getName();
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentString(getName());
    }

    @Override
    public void sendMessage(ITextComponent chatComponent) {
        String message = chatComponent.getUnformattedText();
        if (outputFilter.isEmpty() || message.matches(outputFilter)) {
            if (broadcastResult && channel != null) {
                channel.message(chatComponent.getUnformattedText());
            } else {
                user.notice(chatComponent.getUnformattedText());
            }
        }
    }

    @Override
    public boolean canUseCommand(int permLevel, String commandName) {
        return opEnabled;
    }

    @Override
    public BlockPos getPosition() {
        return new BlockPos(0, 0, 0);
    }

    @Override
    public Vec3d getPositionVector() {
        return new Vec3d(0, 0, 0);
    }

    @Override
    public World getEntityWorld() {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getEntityWorld();
    }

    @Override
    public Entity getCommandSenderEntity() {
        return null;
    }

    @Override
    public boolean sendCommandFeedback() {
        return true;
    }

    @Override
    public void setCommandStat(CommandResultStats.Type type, int amount) {
    }

    @Override
    public MinecraftServer getServer() {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getServer();
    }

}