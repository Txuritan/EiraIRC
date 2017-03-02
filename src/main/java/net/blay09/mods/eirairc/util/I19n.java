// Copyright (c) 2015, Christopher "BlayTheNinth" Baker

package net.blay09.mods.eirairc.util;

import net.minecraft.util.text.translation.I18n;

public class I19n {

    public static String format(String key, Object... params) {
        return I18n.translateToLocalFormatted(key, params);
    }

}
