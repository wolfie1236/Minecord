package com.tisawesomeness.minecord.command.player;

import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONObject;

import com.tisawesomeness.minecord.Config;
import com.tisawesomeness.minecord.command.Command;
import com.tisawesomeness.minecord.util.DateUtils;
import com.tisawesomeness.minecord.util.MessageUtils;
import com.tisawesomeness.minecord.util.NameUtils;
import com.tisawesomeness.minecord.util.RequestUtils;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class HistoryCommand extends Command {
	
	public CommandInfo getInfo() {
		return new CommandInfo(
			"history",
			"Gets the name history of a player.",
			"<username|uuid> [date]",
			null,
			2000,
			false,
			false,
			true
		);
	}
	
	public Result run(String[] args, MessageReceivedEvent e) {
		
		//No arguments message
		if (args.length == 0) {
			String m = ":warning: Incorrect arguments." +
				"\n" + Config.getPrefix() + "uuid <username|uuid> [date] " +
				"\n" + "In [date], you may define a date, time, and timezone." +
				"\n" + "Date Examples:" +
				"\n" + "`9/25`" +
				" | " + "`2/29/2012`" +
				" | " + "`5/15 8:30`" +
				" | " + "`3/2/06 2:47:32`" +
				" | " + "`9:00 PM`" +
				" | " + "`12/25/12 12:00 AM EST`" +
				" | " + "`5:22 CST`";
			return new Result(Outcome.WARNING, m, 5);
		}

		String player = args[0];	
		if (!player.matches(NameUtils.uuidRegex)) {
			String uuid = null;
			
			//Parse date argument
			if (args.length > 1) {
				long timestamp = DateUtils.getTimestamp(Arrays.copyOfRange(args, 1, args.length));
				if (timestamp == -1) {
					String m = ":x: Improperly formatted date. " +
						"At least a date or time is required. " +
						"Do `" + Config.getPrefix() + "uuid` for more info.";
					return new Result(Outcome.ERROR, m);
				}
				
			//Get the UUID
				uuid = NameUtils.getUUID(player, timestamp);
			} else {
				uuid = NameUtils.getUUID(player);
			}
			
			//Check for errors
			if (uuid == null) {
				String m = ":x: The Mojang API could not be reached." +
					"\n" + "Are you sure that username exists?";
				return new Result(Outcome.ERROR, m, 1.5);
			} else if (!uuid.matches(NameUtils.uuidRegex)) {
				String m = ":x: The API responded with an error:\n" + uuid;
				return new Result(Outcome.ERROR, m, 3);
			}
			
			player = uuid;
		}

		//Fetch name history
		String url = "https://api.mojang.com/user/profiles/" + player + "/names";
		String request = RequestUtils.get(url, "application/json");
		if (request == null) {
			return new Result(Outcome.ERROR, ":x: The Mojang API could not be reached.");
		}
		
		//Loop over each name change
		String m = "";
		JSONArray names = new JSONArray(request);
		for (int i = 0; i < names.length(); i++) {
			
			//Get info
			JSONObject change = names.getJSONObject(i);
			String name = change.getString("name");
			String date;
			if (change.has("changedToAt")) {
				date = DateUtils.getString(change.getLong("changedToAt"));
			} else {
				date = "Original";
			}
			
			//Add to lines
			m = name + " | " + date + "\n" + m;
		}
		m.substring(0, m.length() - 1);
		
		player = names.getJSONObject(names.length() - 1).getString("name");
		
		//Get NameMC url
		url = "https://namemc.com/profile/" + player;
		
		//PROPER APOSTROPHE GRAMMAR THANK THE LORD
		if (player.endsWith("s")) {
			player = player + "' Name History";
		} else {
			player = player + "'s Name History";
		}
		
		MessageEmbed me = MessageUtils.wrapMessage(player, url, m, MessageUtils.randomColor());
		
		return new Result(Outcome.SUCCESS, new EmbedBuilder(me).build());
	}
	
}