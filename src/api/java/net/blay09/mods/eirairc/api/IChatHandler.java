package net.blay09.mods.eirairc.api;

import net.blay09.mods.eirairc.api.irc.IRCContext;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.ITextComponent;

public interface IChatHandler {
    void addChatMessage(ITextComponent component, IRCContext source);

    void addChatMessage(ICommandSender receiver, ITextComponent component, IRCContext source);
}
