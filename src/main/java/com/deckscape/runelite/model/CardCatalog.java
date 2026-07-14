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
import static com.deckscape.runelite.model.DeckscapeCard.Style.*;

public final class CardCatalog
{
    private static final String ART = "/com/deckscape/runelite/cards/";
    // Gameplay metadata (style/cost/power/rules text) mirrors DeckscapeWeb's src/engine/catalog.ts
    // so the plugin renders cards exactly like the web app.
    private static volatile List<DeckscapeCard> cards = Collections.unmodifiableList(Arrays.asList(
        c("wizard_tower_wizard", "Wizard Tower Wizard", "Misthalin", COMMON, UNIT, MAGIC, 4, 4, "At the end of your turn, generate a Water rune. Ability: Boost an ally by 1 + your Water runes.", "tower_wizard.png"),
        c("dark_wizard", "Dark Wizard", "Misthalin", COMMON, UNIT, MAGIC, 4, 4, "At the end of your turn, generate a Fire rune. Ability: Damage an enemy by 1 + your Fire runes.", "dark_wizard.png"),
        c("aubury", "Aubury", "Misthalin", UNCOMMON, UNIT, MAGIC, 5, 3, "Whenever you generate runes, boost self by that amount.", "aubury.png"),
        c("archmage_sedridor", "Archmage Sedridor", "Misthalin", LEGENDARY, UNIT, MAGIC, 11, 6, "Your Wizards generate one additional rune.", "archmage_sedridor.png"),
        c("wizard_jalarast", "Wizard Jalarast", "Misthalin", COMMON, UNIT, MAGIC, 4, 5, "At the end of your turn, generate an Earth rune. Ability: Give an ally Shield equal to 1 + your Earth runes.", null),
        c("wizard_mizgog", "Wizard Mizgog", "Misthalin", RARE, UNIT, MAGIC, 7, 4, "At the end of your turn, copy the rune generation of adjacent cards. Ability: Summon a 2-power Imp; +1 power per 4 runes generated this round.", "wizard_mizgog.png"),
        c("imp", "Imp", "Universal", COMMON, UNIT, MELEE, 4, 2, "Deploy: Damage a random enemy by 2.", null),
        c("rick", "Rick", "Misthalin", UNCOMMON, UNIT, MAGIC, 6, 2, "Deploy: Return a chosen allied unit to your hand.", null),
        c("wizard_grayzag", "Wizard Grayzag", "Misthalin", COMMON, UNIT, MAGIC, 4, 5, "At the end of your turn, generate an Air rune. Ability: Deal 1 damage to random enemies 1 + Air rune times.", "wizard_grayzag.png"),
        c("apprentice_felix", "Apprentice Felix", "Misthalin", UNCOMMON, UNIT, MAGIC, 4, 5, "Deploy: If you have at least 2 runes, Bind an enemy.", null),
        c("varrock_guard_trainee", "Varrock Guard Trainee", "Misthalin", COMMON, UNIT, MELEE, 4, 5, "Deploy: Gain +1 power for every allied Civilian.", "trainee_guard.png"),
        c("varrock_guard_captain", "Varrock Guard Captain", "Misthalin", UNCOMMON, UNIT, MELEE, 4, 6, "At the end of your turn, boost adjacent Guards by 1.", "varrock_guard.png"),
        c("king_roald", "King Roald", "Misthalin", LEGENDARY, UNIT, MELEE, 12, 8, "Deploy: Every Guard deals 1 + allied Civilian count damage to a random enemy.", "king_roald.png"),
        c("protect_from_melee", "Protect from Melee", "Universal", COMMON, PRAYER, MAGIC, 5, 0, "Reduce damage from Melee attacks by 50%.", "protect_from_melee.png"),
        c("protect_from_magic", "Protect from Magic", "Universal", COMMON, PRAYER, MAGIC, 5, 0, "Reduce damage from Magic attacks by 50%.", "protect_from_magic.png"),
        c("protect_from_ranged", "Protect from Ranged", "Universal", COMMON, PRAYER, MAGIC, 5, 0, "Reduce damage from Ranged attacks by 50%.", "protect_from_ranged.png"),
        c("smite", "Smite", "Universal", COMMON, SPELL, MAGIC, 5, 0, "Turn off the opponent's active prayer.", "smite.png"),
        c("paxton_and_philbert", "Paxton and Philbert", "Misthalin", RARE, UNIT, MELEE, 7, 5, "Deploy: When placed between two Civilians, destroy the weakest enemy unit.", null),
        c("elemental_strike", "Elemental Strike", "Universal", COMMON, SPELL, MAGIC, 4, 0, "Deal 4 damage to an enemy.", "fire_strike.png"),
        c("sigmund", "Sigmund", "Misthalin", RARE, UNIT, RANGED, 7, 5, "Your protection prayers prevent 100% of matching damage.", "sigmund.png"),
        c("goblin_mail", "Goblin mail", "Misthalin", COMMON, EQUIPMENT, MAGIC, 5, 0, "Give all allied Goblins +2 power and +2 Strength.", null),
        c("cave_goblin_child", "Cave Goblin Child", "Misthalin", UNCOMMON, UNIT, RANGED, 6, 3, "Action: Deal 2 damage to all enemy Magic units.", null),
        c("cave_goblin_ranger", "Cave Goblin Ranger", "Misthalin", COMMON, UNIT, RANGED, 5, 4, "Haste. Action: Deal 1 damage to an enemy. Charges: 3.", "cave_goblin_guard.png"),
        c("zanik", "Zanik", "Misthalin", RARE, UNIT, RANGED, 8, 6, "Deploy: Resurrect the last Goblin sent to Death's Office.", "zanik.png"),
        c("juna", "Juna", "Misthalin", EPIC, UNIT, RANGED, 10, 12, "No ability", null),
        c("lumbridge_guide", "Lumbridge Guide", "Misthalin", RARE, UNIT, MAGIC, 7, 5, "Deploy: Boost all allied Civilians by 1.", "lumbridge_guide.png"),
        c("adventurer_jon", "Adventurer Jon", "Misthalin", UNCOMMON, UNIT, MELEE, 6, 4, "Deploy: Deal 1 damage to an enemy, +1 for each boosted allied Civilian.", "adventurer_jon.png"),
        c("arthur_the_clue_hunter", "Arthur the Clue Hunter", "Misthalin", COMMON, UNIT, MELEE, 6, 4, "Deploy: Play a Casket.", null),
        c("casket", "Casket", "Universal", UNCOMMON, EQUIPMENT, MAGIC, 7, 0, "Choose one: boost all allies by 2; damage all enemies by 2; or play random faction Equipment.", null),
        c("abigaila", "Abigaila", "Misthalin", COMMON, UNIT, MELEE, 5, 4, "Deploy: Look at the top two cards of your deck, play one, and place the other on the bottom.", null),
        c("oldak", "Oldak", "Misthalin", RARE, UNIT, MAGIC, 7, 5, "Deploy: Play two random Equipment cards from any faction.", "oldak.png"),
        c("thingymajig", "Thingymajig", "Misthalin", UNCOMMON, EQUIPMENT, MAGIC, 5, 0, "Give a Goblin +2 Strength and randomly boost three allies by 1.", null),
        c("yew_shortbow", "Yew Shortbow", "Universal", COMMON, EQUIPMENT, RANGED, 4, 0, "Boost all allied Ranged units by 2.", null),
        c("fire_battlestaff", "Fire battlestaff", "Universal", COMMON, EQUIPMENT, MAGIC, 4, 0, "Boost all allied Magic units by 2.", null),
        c("rune_scimitar", "Rune Scimitar", "Universal", COMMON, EQUIPMENT, MELEE, 4, 0, "Boost all allied Melee units by 2.", null),
        c("rune_kiteshield", "Rune Kiteshield", "Universal", COMMON, EQUIPMENT, MELEE, 4, 0, "Give an allied Melee unit Shield 5.", null),
        c("bind", "Bind", "Universal", COMMON, SPELL, MAGIC, 5, 0, "Deal 2 damage to an enemy and Bind it.", null),
        c("enchant_jewellery", "Enchant Jewellery", "Universal", UNCOMMON, SPELL, MAGIC, 7, 0, "Increase every bracketed number on your next two Equipment cards by 1.", null),
        c("ring_of_recoil", "Ring of Recoil", "Universal", UNCOMMON, EQUIPMENT, MAGIC, 6, 0, "When a shielded ally takes damage, deal 1 damage to a random enemy.", null),
        c("man", "Man", "Universal", COMMON, UNIT, MELEE, 4, 4, "Ability: Gain Smith 3.", "man_alternate.png"),
        c("woman", "Woman", "Universal", COMMON, UNIT, MELEE, 4, 4, "Ability: Gain Regen 2.", null),
        c("magic_ball", "Magic Ball", "Misthalin", UNCOMMON, EQUIPMENT, MAGIC, 6, 0, "Generate one of each rune.", null),
        c("bread", "Bread", "Universal", COMMON, EQUIPMENT, MAGIC, 5, 0, "Restore an allied unit to full health.", null)
    ));

    private CardCatalog() {}

    private static DeckscapeCard c(String id, String name, String faction, DeckscapeCard.Rarity rarity, DeckscapeCard.Kind kind,
                                   DeckscapeCard.Style style, int cost, int power, String text, String art)
    {
        return new DeckscapeCard(id, name, faction, rarity, kind, style, cost, power, text, art == null ? null : ART + art);
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
                // Rendering metadata falls back to the bundled card when the server row lacks it.
                DeckscapeCard.Style style = bundled == null ? MAGIC : bundled.getStyle();
                if (row.has("style")) try { style = DeckscapeCard.Style.valueOf(row.get("style").getAsString()); } catch (IllegalArgumentException ignored) { }
                int cost = row.has("cost") ? row.get("cost").getAsInt() : bundled == null ? 0 : bundled.getCost();
                int power = row.has("power") ? row.get("power").getAsInt() : bundled == null ? 0 : bundled.getPower();
                String text = row.has("text") ? row.get("text").getAsString() : bundled == null ? "" : bundled.getText();
                next.add(new DeckscapeCard(id, row.get("name").getAsString(), title(row.get("faction").getAsString()),
                    DeckscapeCard.Rarity.valueOf(row.get("rarity").getAsString()), kind, style, cost, power, text,
                    bundled == null ? null : bundled.getArtResource()));
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
