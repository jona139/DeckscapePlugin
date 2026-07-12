package com.deckscape.runelite.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import static com.deckscape.runelite.model.DeckscapeCard.Kind.*;
import static com.deckscape.runelite.model.DeckscapeCard.Rarity.*;

public final class CardCatalog
{
    private static final String ART = "/com/deckscape/runelite/cards/";
    private static volatile List<DeckscapeCard> cards = Collections.unmodifiableList(Arrays.asList(
        c("wizard_tower_wizard", "Wizard Tower Wizard", "Misthalin", COMMON, UNIT, "tower_wizard.png"),
        c("dark_wizard", "Dark Wizard", "Misthalin", COMMON, UNIT, "dark_wizard.png"),
        c("aubury", "Aubury", "Misthalin", UNCOMMON, UNIT, "aubury.png"),
        c("archmage_sedridor", "Archmage Sedridor", "Misthalin", LEGENDARY, UNIT, "archmage_sedridor.png"),
        c("wizard_jalarast", "Wizard Jalarast", "Misthalin", COMMON, UNIT, null),
        c("wizard_mizgog", "Wizard Mizgog", "Misthalin", RARE, UNIT, "wizard_mizgog.png"),
        c("imp", "Imp", "Universal", COMMON, UNIT, null),
        c("rick", "Rick", "Misthalin", UNCOMMON, UNIT, null),
        c("wizard_grayzag", "Wizard Grayzag", "Misthalin", COMMON, UNIT, "wizard_grayzag.png"),
        c("apprentice_felix", "Apprentice Felix", "Misthalin", UNCOMMON, UNIT, null),
        c("varrock_guard_trainee", "Varrock Guard Trainee", "Misthalin", COMMON, UNIT, "trainee_guard.png"),
        c("varrock_guard_captain", "Varrock Guard Captain", "Misthalin", UNCOMMON, UNIT, "varrock_guard.png"),
        c("king_roald", "King Roald", "Misthalin", LEGENDARY, UNIT, "king_roald.png"),
        c("protect_from_melee", "Protect from Melee", "Universal", COMMON, PRAYER, null),
        c("protect_from_magic", "Protect from Magic", "Universal", COMMON, PRAYER, null),
        c("protect_from_ranged", "Protect from Ranged", "Universal", COMMON, PRAYER, null),
        c("smite", "Smite", "Universal", COMMON, SPELL, null),
        c("paxton_and_philbert", "Paxton and Philbert", "Misthalin", RARE, UNIT, null),
        c("elemental_strike", "Elemental Strike", "Universal", COMMON, SPELL, "fire_strike.png"),
        c("sigmund", "Sigmund", "Misthalin", RARE, UNIT, "sigmund.png"),
        c("goblin_mail", "Goblin mail", "Misthalin", COMMON, EQUIPMENT, null),
        c("cave_goblin_child", "Cave Goblin Child", "Misthalin", UNCOMMON, UNIT, null),
        c("cave_goblin_ranger", "Cave Goblin Ranger", "Misthalin", COMMON, UNIT, "cave_goblin_guard.png"),
        c("zanik", "Zanik", "Misthalin", RARE, UNIT, "zanik.png"),
        c("juna", "Juna", "Misthalin", EPIC, UNIT, null),
        c("lumbridge_guide", "Lumbridge Guide", "Misthalin", RARE, UNIT, "lumbridge_guide.png"),
        c("adventurer_jon", "Adventurer Jon", "Misthalin", UNCOMMON, UNIT, "adventurer_jon.png"),
        c("arthur_the_clue_hunter", "Arthur the Clue Hunter", "Misthalin", COMMON, UNIT, null),
        c("casket", "Casket", "Universal", UNCOMMON, EQUIPMENT, null),
        c("abigaila", "Abigaila", "Misthalin", COMMON, UNIT, null),
        c("oldak", "Oldak", "Misthalin", RARE, UNIT, "oldak.png"),
        c("thingymajig", "Thingymajig", "Misthalin", UNCOMMON, EQUIPMENT, null),
        c("yew_shortbow", "Yew Shortbow", "Universal", COMMON, EQUIPMENT, null),
        c("fire_battlestaff", "Fire battlestaff", "Universal", COMMON, EQUIPMENT, null),
        c("rune_scimitar", "Rune Scimitar", "Universal", COMMON, EQUIPMENT, null),
        c("rune_kiteshield", "Rune Kiteshield", "Universal", COMMON, EQUIPMENT, null),
        c("bind", "Bind", "Universal", COMMON, SPELL, null),
        c("enchant_jewellery", "Enchant Jewellery", "Universal", UNCOMMON, SPELL, null),
        c("ring_of_recoil", "Ring of Recoil", "Universal", UNCOMMON, EQUIPMENT, null),
        c("man", "Man", "Universal", COMMON, UNIT, "man_alternate.png"),
        c("woman", "Woman", "Universal", COMMON, UNIT, null),
        c("magic_ball", "Magic Ball", "Misthalin", UNCOMMON, EQUIPMENT, null),
        c("bread", "Bread", "Universal", COMMON, EQUIPMENT, null)
    ));

    private CardCatalog() {}

    private static DeckscapeCard c(String id, String name, String faction, DeckscapeCard.Rarity rarity, DeckscapeCard.Kind kind, String art)
    {
        return new DeckscapeCard(id, name, faction, rarity, kind, art == null ? null : ART + art);
    }

    public static List<DeckscapeCard> all() { return cards; }

    public static DeckscapeCard byId(String id)
    {
        return cards.stream().filter(card -> card.getId().equals(id)).findFirst().orElse(null);
    }

    public static List<DeckscapeCard> openPack(PackType type, Random random)
    {
        List<DeckscapeCard> pool = cards.stream().filter(card -> eligible(type, card)).collect(Collectors.toList());
        if (pool.isEmpty()) pool = new ArrayList<>(cards);
        List<DeckscapeCard> result = new ArrayList<>();
        for (int i = 0; i < 5; i++) result.add(weighted(pool, random));
        return result;
    }

    /** Replaces bundled metadata with the server catalog so newly deployed cards appear immediately. */
    public static void replaceFromServer(JsonArray catalog)
    {
        List<DeckscapeCard> next = new ArrayList<>();
        for (JsonElement element : catalog)
        {
            JsonObject row = element.getAsJsonObject();
            try
            {
                String id = row.get("id").getAsString();
                DeckscapeCard bundled = byId(id);
                String type = row.get("kind").getAsString();
                DeckscapeCard.Kind kind = "UNIT".equals(type) ? UNIT : "EQUIPMENT".equals(type) ? EQUIPMENT : "PRAYER".equals(type) ? PRAYER : SPELL;
                next.add(c(id, row.get("name").getAsString(), title(row.get("faction").getAsString()),
                    DeckscapeCard.Rarity.valueOf(row.get("rarity").getAsString()), kind,
                    bundled == null || bundled.getArtResource() == null ? null : bundled.getArtResource().substring(ART.length())));
            }
            catch (RuntimeException ignored) { }
        }
        if (!next.isEmpty()) cards = Collections.unmodifiableList(next);
    }

    private static String title(String value)
    {
        if (value == null || value.isEmpty()) return "Universal";
        String lower = value.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static boolean eligible(PackType type, DeckscapeCard card)
    {
        if (type == PackType.COMBAT) return card.getKind() == UNIT;
        if (type == PackType.SKILLING) return card.getKind() != UNIT;
        if (type.name().startsWith("FACTION_")) return title(type.name().substring("FACTION_".length())).equals(card.getFaction());
        return true;
    }

    private static DeckscapeCard weighted(List<DeckscapeCard> pool, Random random)
    {
        int roll = random.nextInt(100);
        DeckscapeCard.Rarity rarity = roll < 65 ? COMMON : roll < 87 ? UNCOMMON : roll < 96 ? RARE : roll < 99 ? EPIC : LEGENDARY;
        List<DeckscapeCard> matching = pool.stream().filter(card -> card.getRarity() == rarity).collect(Collectors.toList());
        if (matching.isEmpty()) matching = pool;
        return matching.get(random.nextInt(matching.size()));
    }
}
