// Copyright (c) 2015 Christopher "BlayTheNinth" Baker

package net.blay09.mods.eirairc.config.settings;

import net.blay09.mods.eirairc.config.property.ConfigProperty;
import net.minecraft.util.text.TextFormatting;

public class ThemeSettings extends AbstractSettings {

    private static final String THEME = "theme";

    public final ConfigProperty<TextFormatting> mcNameColor = new ConfigProperty<>(manager, category, "mcNameColor", TextFormatting.WHITE);
    public final ConfigProperty<TextFormatting> mcOpNameColor = new ConfigProperty<>(manager, category, "mcOpNameColor", TextFormatting.RED);
    public final ConfigProperty<TextFormatting> ircNameColor = new ConfigProperty<>(manager, category, "ircNameColor", TextFormatting.GRAY);
    public final ConfigProperty<TextFormatting> ircOpNameColor = new ConfigProperty<>(manager, category, "ircOpNameColor", TextFormatting.GOLD);
    public final ConfigProperty<TextFormatting> ircVoiceNameColor = new ConfigProperty<>(manager, category, "ircVoiceNameColor", TextFormatting.GRAY);
    public final ConfigProperty<TextFormatting> ircPrivateNameColor = new ConfigProperty<>(manager, category, "ircPrivateNameColor", TextFormatting.GRAY);
    public final ConfigProperty<TextFormatting> ircNoticeTextColor = new ConfigProperty<>(manager, category, "ircNoticeTextColor", TextFormatting.RED);
    public final ConfigProperty<TextFormatting> emoteTextColor = new ConfigProperty<>(manager, category, "emoteTextColor", TextFormatting.GOLD);

    public ThemeSettings(ThemeSettings parent) {
        super(parent, THEME);
    }

}
