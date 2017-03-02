package net.blay09.mods.eirairc.api.irc;

@SuppressWarnings("unused")
public interface IRCMessage {
    String getTagByKey(String key);

    String getPrefix();

    String getPrefixNick();

    String getPrefixUsername();

    String getPrefixHostname();

    String getCommand();

    String arg(int i);

    int argCount();
}
