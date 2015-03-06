// Copyright (c) 2014, Christopher "blay09" Baker
// All rights reserved.

package net.blay09.mods.eirairc.net;

import java.util.HashMap;
import java.util.Map;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;

public class EiraNetHandler {

	private final Map<String, EiraPlayerInfo> playerInfoMap = new HashMap<String, EiraPlayerInfo>();
	
	public EiraPlayerInfo getPlayerInfo(String username) {
		synchronized(playerInfoMap) {
			EiraPlayerInfo playerInfo = playerInfoMap.get(username);
			if(playerInfo == null) {
				playerInfo = new EiraPlayerInfo(username);
				playerInfoMap.put(username, playerInfo);
			}
			return playerInfo;
		}
	}

	@SubscribeEvent
	public void onPlayerLogout(PlayerLoggedOutEvent event) {
		synchronized(playerInfoMap) {
			playerInfoMap.remove(event.player.getCommandSenderName());
		}
	}
	
}
