package com.deckscape.runelite.challenge;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Canonical RuneLite gold-trim tasks mirrored from DeckscapeWeb. */
public enum RuneLiteChallenge
{
    PURE_ESSENCE(
        "PURE_ESSENCE_MINED", "osrs_pure_essence_mined", "sedridor_pure_essence:gold",
        "Essence of the Archmage", "Mine 10,000 pure essence.", "Archmage Sedridor gold trim", 10_000, true),
    LEDERHOSEN(
        "LEDERHOSEN_OUTFIT_EQUIPPED", "osrs_lederhosen_equipped", "pheasant_lederhosen:gold",
        "A Most Impeccable Disguise", "Equip the full lederhosen outfit.", "Pheasant gold trim", 1, false),
    STRIKE_HIT(
        "STRIKE_HIT_16", "osrs_strike_hit_16", "elemental_strike_16:gold",
        "Elemental Impact", "Hit at least 16 in one hit with a Strike spell.", "Elemental Strike gold trim", 1, false),
    MAX_CAPE(
        "MAX_CAPE_EQUIPPED", "osrs_max_cape_equipped", "max_cape_equipped:gold",
        "Maxed Out", "Equip a Max Cape.", "Max Cape gold trim", 1, false),
    DIZANAS_QUIVER(
        "DIZANAS_QUIVER_EQUIPPED", "osrs_dizanas_quiver_equipped", "sol_quiver_equipped:gold",
        "The Sun's Champion", "Equip Dizana's Quiver.", "Sol Heredit gold trim", 1, false),
    KANDARIN_ELITE(
        "KANDARIN_ELITE_DIARY_COMPLETED", "osrs_kandarin_elite_diary", "penance_kandarin_elite:gold",
        "Elite Penance", "Complete the Kandarin Elite Diary.", "Penance Queen gold trim", 1, false),
    QUEST_CAPE(
        "QUEST_CAPE_EQUIPPED", "osrs_quest_cape_equipped", "wise_old_man_quest_cape:gold",
        "A Life of Adventure", "Equip a Quest Cape.", "Wise Old Man gold trim", 1, false),
    RUNE_KITESHIELD(
        "RUNE_KITESHIELD_SMITHED", "osrs_rune_kiteshield_smithed", "rune_kiteshield_smithed:gold",
        "Made of Rune", "Smith a Rune Kiteshield.", "Rune Kiteshield gold trim", 1, true);

    public static final List<RuneLiteChallenge> ALL = Collections.unmodifiableList(Arrays.asList(values()));

    private final String kind;
    private final String metric;
    private final String tierKey;
    private final String title;
    private final String description;
    private final String reward;
    private final int target;
    private final boolean quantity;

    RuneLiteChallenge(String kind, String metric, String tierKey, String title, String description,
                      String reward, int target, boolean quantity)
    {
        this.kind = kind;
        this.metric = metric;
        this.tierKey = tierKey;
        this.title = title;
        this.description = description;
        this.reward = reward;
        this.target = target;
        this.quantity = quantity;
    }

    public String getKind() { return kind; }
    public String getMetric() { return metric; }
    public String getTierKey() { return tierKey; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getReward() { return reward; }
    public int getTarget() { return target; }
    public boolean isQuantity() { return quantity; }

    public static RuneLiteChallenge byKind(String kind)
    {
        for (RuneLiteChallenge challenge : values()) if (challenge.kind.equals(kind)) return challenge;
        return null;
    }
}
