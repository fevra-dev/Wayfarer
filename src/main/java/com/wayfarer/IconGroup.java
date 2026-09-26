package com.wayfarer;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimap icon categories grouped into the plugin's toggles. Category ids
 * and their names come from the game's world-map key (cache enum 1713,
 * category -> name), read from a cache dump on 2026-09-26 and cross-checked
 * live: the ids logged in-game (Bank 1055, General store 1049, Anvil 1060...)
 * matched it exactly. Every category in that key is listed here once.
 */
enum IconGroup
{
	BANKS(
		1055, // Bank
		1126 // Grand Exchange
	),
	ALTARS(
		1070 // Altar
	),
	SHOPS(
		1049, 1050, 1052, 1053, 1054, 1057, 1064, 1065, 1066, 1067, 1068, 1069, 1072, 1073, 1074, 1075,
		1076, 1078, 1079, 1080, 1081, 1082, 1083, 1090, 1093, 1094, 1095, 1096, 1097, 1106, 1113, 1122,
		1123, 1125, 1128, 1266, 1267, 1269, 1270, 1272, 1273, 1274, 1790, 1071
	),
	RARE_TREES(
		1085 // Rare trees
	),
	TRANSPORT(
		1104, // Transportation
		1105, // House portal
		1117, // Agility shortcut
		2505, // Agility shortcut (one way)
		2393 // Docking point
	),
	SKILLING(
		1058, 1059, 1060, 1061, 1077, 1084, 1086, 1088, 1089, 1091, 1092, 1098, 1101, 1107, 1108, 1109,
		1110, 1111, 1115, 1118, 1396, 1425, 1641, 1642, 1643, 2002, 2261, 2386, 2388, 2390
	),
	MINIGAMES(
		1087, // Minigame
		1716, // Raid
		1499, // Distraction & Diversion
		2387, // Barracuda Trial
		1119 // Holiday event
	),
	SLAYER(
		1099 // Slayer Master
	),
	QUESTS(
		1056 // Quest
	),
	DUNGEONS(
		1062 // Dungeon
	),
	SERVICES(
		1100, 1102, 1112, 1114, 1116, 1120, 1121, 1250, 1251, 1252, 1253, 1254, 1255, 1256, 1257, 1258,
		1259, 1260, 1261, 1262, 1263, 1264, 1265, 1268, 1271, 1275, 1317, 176, 1459, 1551, 1579, 2389,
		2391, 2392, 2394, 1063
	);

	private final int[] categories;

	IconGroup(int... categories)
	{
		this.categories = categories;
	}

	private static final Map<Integer, IconGroup> BY_CATEGORY = new HashMap<>();

	static
	{
		for (IconGroup group : values())
		{
			for (int category : group.categories)
			{
				IconGroup previous = BY_CATEGORY.put(category, group);
				if (previous != null)
				{
					throw new IllegalStateException("category " + category + " in both " + previous + " and " + group);
				}
			}
		}
	}

	/** The group for a minimap icon category, or null for one the key doesn't list. */
	static IconGroup forCategory(int category)
	{
		return BY_CATEGORY.get(category);
	}

	static int categoryCount()
	{
		return BY_CATEGORY.size();
	}
}
