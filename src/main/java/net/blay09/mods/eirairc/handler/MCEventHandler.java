// Copyright (c) 2015, Christopher "BlayTheNinth" Baker

package net.blay09.mods.eirairc.handler;

import net.blay09.mods.eirairc.ConnectionManager;
import net.blay09.mods.eirairc.EiraIRC;
import net.blay09.mods.eirairc.api.EiraIRCAPI;
import net.blay09.mods.eirairc.api.event.ClientChatEvent;
import net.blay09.mods.eirairc.api.irc.IRCChannel;
import net.blay09.mods.eirairc.api.irc.IRCConnection;
import net.blay09.mods.eirairc.api.irc.IRCContext;
import net.blay09.mods.eirairc.api.irc.IRCUser;
import net.blay09.mods.eirairc.command.base.IRCCommandHandler;
import net.blay09.mods.eirairc.config.*;
import net.blay09.mods.eirairc.config.settings.BotSettings;
import net.blay09.mods.eirairc.config.settings.GeneralSettings;
import net.blay09.mods.eirairc.net.NetworkHandler;
import net.blay09.mods.eirairc.net.message.MessageRedirect;
import net.blay09.mods.eirairc.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.server.CommandBroadcast;
import net.minecraft.command.server.CommandEmote;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AchievementEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("unused")
public class MCEventHandler {

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        for (ServerConfig serverConfig : ConfigurationHandler.getServerConfigs()) {
            IRCConnection connection = ConnectionManager.getConnection(serverConfig.getIdentifier());
            if (connection != null) {
                for (ChannelConfig channelConfig : serverConfig.getChannelConfigs()) {
                    IRCChannel channel = connection.getChannel(channelConfig.getName());
                    if (channel != null) {
                        GeneralSettings generalSettings = ConfigHelper.getGeneralSettings(channel);
                        BotSettings botSettings = ConfigHelper.getBotSettings(channel);
                        String ircMessage = MessageFormat.formatMessage(botSettings.getMessageFormat().ircPlayerJoin, channel, event.player, "", MessageFormat.Target.IRC, MessageFormat.Mode.Message);
                        if (!generalSettings.readOnly.get() && botSettings.relayMinecraftJoinLeave.get()) {
                            channel.message(ircMessage);
                        }
                        if (channel.getTopic() != null) {
                            ITextComponent chatComponent = MessageFormat.formatChatComponent(ConfigHelper.getBotSettings(channel).getMessageFormat().mcTopic, connection, channel, null, channel.getTopic(), MessageFormat.Target.Minecraft, MessageFormat.Mode.Message);
                            event.player.sendMessage(chatComponent);
                        }
                        if (generalSettings.autoWho.get()) {
                            Utils.sendUserList(event.player, connection, channel);
                        }
                    }
                }
            }
        }

        // Send redirect configurations to client
        if (FMLCommonHandler.instance().getMinecraftServerInstance() != null && FMLCommonHandler.instance().getMinecraftServerInstance().isDedicatedServer()) {
            for (ServerConfig serverConfig : ConfigurationHandler.getServerConfigs()) {
                if (serverConfig.isRedirect()) {
                    NetworkHandler.instance.sendTo(new MessageRedirect(serverConfig.toJsonObject().toString()), (EntityPlayerMP) event.player);
                }
            }
        }

    }

    @SubscribeEvent
    public void onServerCommand(CommandEvent event) {
        //if (!event.getCommand().canCommandSenderUseCommand(event.getSender())) {
        //    return;
        //}
        if (event.getCommand() instanceof CommandEmote) {
            if (event.getSender() instanceof EntityPlayer) {
                String emote = StringUtils.join(event.getParameters(), " ", 0, event.getParameters().length).trim();
                if (emote.length() == 0) {
                    return;
                }
                ITextComponent chatComponent = new TextComponentTranslation("* ");
                chatComponent.appendSibling(event.getSender().getDisplayName());
                chatComponent.appendText(" ");
                chatComponent.appendSibling(MessageFormat.createChatComponentForMessage(emote));
                TextFormatting emoteColor = SharedGlobalConfig.theme.emoteTextColor.get();
                if (emoteColor != null) {
                    chatComponent.getStyle().setColor(emoteColor);
                }
                Utils.addMessageToChat(chatComponent);
                if (!FMLCommonHandler.instance().getMinecraftServerInstance().isSinglePlayer()) {
                    relayChatServer(event.getSender(), emote, true, false, null);
                }
                event.setCanceled(true);
            }
        } else if (event.getCommand() instanceof CommandBroadcast) {
            for (ServerConfig serverConfig : ConfigurationHandler.getServerConfigs()) {
                IRCConnection connection = ConnectionManager.getConnection(serverConfig.getIdentifier());
                if (connection != null) {
                    for (ChannelConfig channelConfig : serverConfig.getChannelConfigs()) {
                        IRCChannel channel = connection.getChannel(channelConfig.getName());
                        if (channel != null) {
                            GeneralSettings generalSettings = ConfigHelper.getGeneralSettings(channel);
                            BotSettings botSettings = ConfigHelper.getBotSettings(channel);
                            String ircMessage = MessageFormat.formatMessage(botSettings.getMessageFormat().ircBroadcastMessage, channel, event.getSender(), StringUtils.join(event.getParameters(), " ", 0, event.getParameters().length), MessageFormat.Target.IRC, MessageFormat.Mode.Message);
                            if (!generalSettings.readOnly.get() && botSettings.relayBroadcasts.get()) {
                                channel.message(ircMessage);
                            }
                        }
                    }
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onClientChat(ClientChatEvent event) {
        if (event.message.startsWith("/")) {
            if (event.message.startsWith("/me ") && event.message.length() > 4) {
                onClientEmote(event);
            }
            return;
        }
        EntityPlayer sender = Minecraft.getMinecraft().player;
        if (ConnectionManager.getConnectionCount() > 0 && IRCCommandHandler.onChatCommand(sender, event.message, false)) {
            event.setCanceled(true);
            return;
        }
        if (ClientGlobalConfig.clientBridge.get()) {
            relayChatClient(event.message, false, false, null);
            event.setCanceled(true);
            return;
        }
        IRCContext chatTarget = EiraIRC.instance.getChatSessionHandler().getChatTarget();
        if (chatTarget == null) {
            return;
        }
        IRCUser ircSender = chatTarget.getConnection().getBotUser();
        ITextComponent chatComponent;
        if (chatTarget instanceof IRCChannel) {
            BotSettings botSettings = ConfigHelper.getBotSettings(chatTarget);
            chatComponent = MessageFormat.formatChatComponent(botSettings.getMessageFormat().mcSendChannelMessage, chatTarget.getConnection(), chatTarget, ircSender, event.message, MessageFormat.Target.Minecraft, MessageFormat.Mode.Message);
        } else if (chatTarget instanceof IRCUser) {
            BotSettings botSettings = ConfigHelper.getBotSettings(chatTarget);
            chatComponent = MessageFormat.formatChatComponent(botSettings.getMessageFormat().mcSendPrivateMessage, chatTarget.getConnection(), chatTarget, ircSender, event.message, MessageFormat.Target.Minecraft, MessageFormat.Mode.Message);
        } else {
            return;
        }
        relayChatClient(event.message, false, false, chatTarget);
        EiraIRCAPI.getChatHandler().addChatMessage(chatComponent, chatTarget);
        event.setCanceled(true);
    }

    @SideOnly(Side.CLIENT)
    public void onClientEmote(ClientChatEvent event) {
        String text = event.message.substring(4);
        EntityPlayer sender = Minecraft.getMinecraft().player;
        if (ClientGlobalConfig.clientBridge.get()) {
            relayChatClient(text, true, false, null);
            event.setCanceled(true);
            return;
        }
        IRCContext chatTarget = EiraIRC.instance.getChatSessionHandler().getChatTarget();
        if (chatTarget == null) {
            return;
        }
        IRCUser ircSender = chatTarget.getConnection().getBotUser();
        TextFormatting emoteColor;
        ITextComponent chatComponent;
        if (chatTarget instanceof IRCChannel) {
            emoteColor = ConfigHelper.getTheme(chatTarget).emoteTextColor.get();
            BotSettings botSettings = ConfigHelper.getBotSettings(chatTarget);
            chatComponent = MessageFormat.formatChatComponent(botSettings.getMessageFormat().mcSendChannelEmote, chatTarget.getConnection(), chatTarget, ircSender, text, MessageFormat.Target.IRC, MessageFormat.Mode.Emote);
        } else if (chatTarget instanceof IRCUser) {
            emoteColor = ConfigHelper.getTheme(chatTarget).emoteTextColor.get();
            BotSettings botSettings = ConfigHelper.getBotSettings(chatTarget);
            chatComponent = MessageFormat.formatChatComponent(botSettings.getMessageFormat().mcSendPrivateEmote, chatTarget.getConnection(), chatTarget, ircSender, text, MessageFormat.Target.IRC, MessageFormat.Mode.Emote);
        } else {
            return;
        }
        relayChatClient(text, true, false, chatTarget);
        if (emoteColor != null) {
            chatComponent.getStyle().setColor(emoteColor);
        }
        EiraIRCAPI.getChatHandler().addChatMessage(chatComponent, chatTarget);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        ITextComponent senderComponent = event.getPlayer().getDisplayName();
        TextFormatting nameColor = IRCFormatting.getColorFormattingForPlayer(event.getPlayer());
        if (nameColor != null && nameColor != TextFormatting.WHITE) {
            senderComponent.getStyle().setColor(nameColor);
        }
        event.setComponent(new TextComponentTranslation("chat.type.text", senderComponent, MessageFormat.createChatComponentForMessage(event.getMessage())));
        if (!FMLCommonHandler.instance().getMinecraftServerInstance().isSinglePlayer()) {
            if (IRCCommandHandler.onChatCommand(event.getPlayer(), event.getMessage(), true)) {
                event.setCanceled(true);
                return;
            }
            relayChatServer(event.getPlayer(), event.getMessage(), false, false, null);
        }
    }

    @SideOnly(Side.CLIENT)
    public void relayChatClient(String message, boolean isEmote, boolean isNotice, IRCContext target) {
        if (target != null) {
            if (!ConfigHelper.getGeneralSettings(target).readOnly.get()) {
                if (isEmote) {
                    if (isNotice) {
                        target.ctcpNotice("ACTION " + message);
                    } else {
                        target.ctcpMessage("ACTION " + message);
                    }
                } else {
                    if (isNotice) {
                        target.notice(message);
                    } else {
                        target.message(message);
                    }
                }
            } else {
                ChatComponentBuilder.create().color('c').lang("eirairc:general.readOnly", target.getName()).send();
            }
        } else {
            if (ClientGlobalConfig.clientBridge.get()) {
                String ircMessage = message;
                boolean isCtcp = false;
                if (isEmote) {
                    isCtcp = true;
                    ircMessage = "ACTION " + ircMessage;
                }
                for (ServerConfig serverConfig : ConfigurationHandler.getServerConfigs()) {
                    IRCConnection connection = ConnectionManager.getConnection(serverConfig.getIdentifier());
                    if (connection != null) {
                        for (ChannelConfig channelConfig : serverConfig.getChannelConfigs()) {
                            IRCChannel channel = connection.getChannel(channelConfig.getName());
                            if (channel != null) {
                                if (!ConfigHelper.getGeneralSettings(channel).readOnly.get()) {
                                    if (isCtcp) {
                                        if (isNotice) {
                                            channel.ctcpNotice(ircMessage);
                                        } else {
                                            channel.ctcpMessage(ircMessage);
                                        }
                                    } else {
                                        if (isNotice) {
                                            channel.notice(ircMessage);
                                        } else {
                                            channel.message(ircMessage);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                IRCContext chatTarget = EiraIRC.instance.getChatSessionHandler().getChatTarget();
                if (chatTarget != null) {
                    if (!ConfigHelper.getGeneralSettings(chatTarget).readOnly.get()) {
                        if (isEmote) {
                            if (isNotice) {
                                chatTarget.ctcpNotice("ACTION " + message);
                            } else {
                                chatTarget.ctcpMessage("ACTION " + message);
                            }
                        } else {
                            if (isNotice) {
                                chatTarget.notice(message);
                            } else {
                                chatTarget.message(message);
                            }
                        }
                    } else {
                        ChatComponentBuilder.create().color('c').lang("eirairc:general.readOnly", chatTarget.getName()).send();
                    }
                }
            }
        }
    }

    public void relayChatServer(ICommandSender sender, String message, boolean isEmote, boolean isNotice, IRCContext target) {
        if (target != null) {
            if (!ConfigHelper.getGeneralSettings(target).readOnly.get()) {
                if (!isEmote && !isNotice && ConfigurationHandler.passesRemoteCommand(sender, message)) {
                    target.message(message);
                    return;
                }
                String format = MessageFormat.getMessageFormat(target, isEmote);
                String ircMessage = MessageFormat.formatMessage(format, target, sender, message, MessageFormat.Target.IRC, (isEmote ? MessageFormat.Mode.Emote : MessageFormat.Mode.Message));
                if (isEmote) {
                    if (isNotice) {
                        target.ctcpNotice("ACTION " + ircMessage);
                    } else {
                        target.ctcpMessage("ACTION " + ircMessage);
                    }
                } else {
                    if (isNotice) {
                        target.notice(ircMessage);
                    } else {
                        target.message(ircMessage);
                    }
                }
            }
        } else {
            for (ServerConfig serverConfig : ConfigurationHandler.getServerConfigs()) {
                IRCConnection connection = ConnectionManager.getConnection(serverConfig.getIdentifier());
                if (connection != null) {
                    for (ChannelConfig channelConfig : serverConfig.getChannelConfigs()) {
                        IRCChannel channel = connection.getChannel(channelConfig.getName());
                        if (channel != null) {
                            String format = MessageFormat.getMessageFormat(channel, isEmote);
                            String ircMessage = MessageFormat.formatMessage(format, channel, sender, message, MessageFormat.Target.IRC, (isEmote ? MessageFormat.Mode.Emote : MessageFormat.Mode.Message));
                            if (!ConfigHelper.getGeneralSettings(channel).readOnly.get()) {
                                if (isEmote) {
                                    if (isNotice) {
                                        channel.ctcpNotice("ACTION " + ircMessage);
                                    } else {
                                        channel.ctcpMessage("ACTION " + ircMessage);
                                    }
                                } else {
                                    if (isNotice) {
                                        channel.notice(ircMessage);
                                    } else {
                                        channel.message(ircMessage);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            for (ServerConfig serverConfig : ConfigurationHandler.getServerConfigs()) {
                IRCConnection connection = ConnectionManager.getConnection(serverConfig.getIdentifier());
                if (connection != null) {
                    for (ChannelConfig channelConfig : serverConfig.getChannelConfigs()) {
                        IRCChannel channel = connection.getChannel(channelConfig.getName());
                        if (channel != null) {
                            GeneralSettings generalSettings = ConfigHelper.getGeneralSettings(channel);
                            BotSettings botSettings = ConfigHelper.getBotSettings(channel);
                            String name = Utils.getNickIRC((EntityPlayer) event.getEntityLiving(), channel);
                            String ircMessage = event.getEntityLiving().getCombatTracker().getDeathMessage().getUnformattedText();
                            ircMessage = ircMessage.replace(event.getEntityLiving().getName(), name);
                            ircMessage = IRCFormatting.toIRC(ircMessage, !botSettings.convertColors.get());
                            if (!generalSettings.readOnly.get() && botSettings.relayDeathMessages.get()) {
                                channel.message(ircMessage);
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onAchievement(AchievementEvent event) {
        if (((EntityPlayerMP) event.getEntityPlayer()).getStatFile().hasAchievementUnlocked(event.getAchievement())) {
            // This is necessary because the Achievement event fires even if an achievement is already unlocked.
            return;
        }
        if (!((EntityPlayerMP) event.getEntityPlayer()).getStatFile().canUnlockAchievement(event.getAchievement())) {
            // This is necessary because the Achievement event fires even if an achievement can not be unlocked yet.
            return;
        }
        for (ServerConfig serverConfig : ConfigurationHandler.getServerConfigs()) {
            IRCConnection connection = ConnectionManager.getConnection(serverConfig.getIdentifier());
            if (connection != null) {
                for (ChannelConfig channelConfig : serverConfig.getChannelConfigs()) {
                    IRCChannel channel = connection.getChannel(channelConfig.getName());
                    if (channel != null) {
                        GeneralSettings generalSettings = ConfigHelper.getGeneralSettings(channel);
                        BotSettings botSettings = ConfigHelper.getBotSettings(channel);
                        String ircMessage = MessageFormat.formatMessage(botSettings.getMessageFormat().ircAchievement, channel, event.getEntityPlayer(), event.getAchievement().getStatName().getUnformattedText(), MessageFormat.Target.IRC, MessageFormat.Mode.Emote);
                        if (!generalSettings.readOnly.get() && botSettings.relayAchievements.get()) {
                            channel.message(ircMessage);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        for (ServerConfig serverConfig : ConfigurationHandler.getServerConfigs()) {
            IRCConnection connection = ConnectionManager.getConnection(serverConfig.getIdentifier());
            if (connection != null) {
                for (ChannelConfig channelConfig : serverConfig.getChannelConfigs()) {
                    IRCChannel channel = connection.getChannel(channelConfig.getName());
                    if (channel != null) {
                        GeneralSettings generalSettings = ConfigHelper.getGeneralSettings(channel);
                        BotSettings botSettings = ConfigHelper.getBotSettings(channel);
                        String ircMessage = MessageFormat.formatMessage(botSettings.getMessageFormat().ircPlayerLeave, channel, event.player, "", MessageFormat.Target.IRC, MessageFormat.Mode.Message);
                        if (!generalSettings.readOnly.get() && botSettings.relayMinecraftJoinLeave.get()) {
                            channel.message(ircMessage);
                        }
                    }
                }
            }
        }
    }

    public void onPlayerNickChange(EntityPlayer player, String oldNick) {
        String format = SharedGlobalConfig.botSettings.getMessageFormat().ircPlayerNickChange;
        format = format.replace("{OLDNICK}", oldNick);
        ITextComponent chatComponent = MessageFormat.formatChatComponent(format, null, player, "", MessageFormat.Target.Minecraft, MessageFormat.Mode.Emote);
        Utils.addMessageToChat(chatComponent);
        for (ServerConfig serverConfig : ConfigurationHandler.getServerConfigs()) {
            IRCConnection connection = ConnectionManager.getConnection(serverConfig.getIdentifier());
            if (connection != null) {
                for (ChannelConfig channelConfig : serverConfig.getChannelConfigs()) {
                    IRCChannel channel = connection.getChannel(channelConfig.getName());
                    if (channel != null) {
                        if (!ConfigHelper.getGeneralSettings(channel).readOnly.get()) {
                            format = ConfigHelper.getBotSettings(channel).getMessageFormat().ircPlayerNickChange;
                            format = format.replace("{OLDNICK}", oldNick);
                            channel.message(MessageFormat.formatMessage(format, channel, player, "", MessageFormat.Target.IRC, MessageFormat.Mode.Emote));
                        }
                    }
                }
            }
        }
    }

}
