package com.wayfarer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class IconGroupTest
{
	/** Every category in the game's world-map key (cache enum 1713, dumped 2026-09-26). */
	private static final int[] WORLD_MAP_KEY = {176, 1049, 1050, 1052, 1053, 1054, 1055, 1056, 1057, 1058, 1059, 1060, 1061, 1062, 1063, 1064, 1065, 1066, 1067, 1068, 1069, 1070, 1071, 1072, 1073, 1074, 1075, 1076, 1077, 1078, 1079, 1080, 1081, 1082, 1083, 1084, 1085, 1086, 1087, 1088, 1089, 1090, 1091, 1092, 1093, 1094, 1095, 1096, 1097, 1098, 1099, 1100, 1101, 1102, 1104, 1105, 1106, 1107, 1108, 1109, 1110, 1111, 1112, 1113, 1114, 1115, 1116, 1117, 1118, 1119, 1120, 1121, 1122, 1123, 1125, 1126, 1128, 1250, 1251, 1252, 1253, 1254, 1255, 1256, 1257, 1258, 1259, 1260, 1261, 1262, 1263, 1264, 1265, 1266, 1267, 1268, 1269, 1270, 1271, 1272, 1273, 1274, 1275, 1317, 1396, 1425, 1459, 1499, 1551, 1579, 1641, 1642, 1643, 1716, 1790, 2002, 2261, 2386, 2387, 2388, 2389, 2390, 2391, 2392, 2393, 2394, 2505};

	@Test
	public void everyWorldMapKeyCategoryHasExactlyOneGroup()
	{
		assertEquals(127, WORLD_MAP_KEY.length);
		for (int category : WORLD_MAP_KEY)
		{
			assertNotNull("category " + category + " has no group", IconGroup.forCategory(category));
		}
		// And nothing extra: the groups list exactly the key, no invented ids.
		assertEquals(WORLD_MAP_KEY.length, IconGroup.categoryCount());
	}

	@Test
	public void theDefaultOnGroupsHoldTheirIcons()
	{
		assertEquals(IconGroup.BANKS, IconGroup.forCategory(1055));
		assertEquals(IconGroup.ALTARS, IconGroup.forCategory(1070));
		assertEquals(IconGroup.SHOPS, IconGroup.forCategory(1049));
		assertEquals(IconGroup.RARE_TREES, IconGroup.forCategory(1085));
		assertNull(IconGroup.forCategory(-1));
	}
}
