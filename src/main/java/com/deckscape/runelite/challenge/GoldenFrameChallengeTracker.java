package com.deckscape.runelite.challenge;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.VarbitID;

/**
 * Correlates inventory/equipment changes with game-owned XP, chat, and varbit evidence.
 * Rewards are still validated, de-duplicated, and granted by the Deckscape service.
 */
@Singleton
public final class GoldenFrameChallengeTracker
{
    private static final int EVIDENCE_WINDOW_TICKS = 2;

    private static final Set<Integer> MAX_CAPES = setOf(
        ItemID.MAX_CAPE, ItemID.FIRE_MAX_CAPE, ItemID.SARADOMIN_MAX_CAPE, ItemID.ZAMORAK_MAX_CAPE,
        ItemID.GUTHIX_MAX_CAPE, ItemID.ACCUMULATOR_MAX_CAPE, ItemID.ARDOUGNE_MAX_CAPE,
        ItemID.INFERNAL_MAX_CAPE, ItemID.IMBUED_SARADOMIN_MAX_CAPE, ItemID.IMBUED_ZAMORAK_MAX_CAPE,
        ItemID.IMBUED_GUTHIX_MAX_CAPE, ItemID.ASSEMBLER_MAX_CAPE, ItemID.MYTHICAL_MAX_CAPE,
        ItemID.MASORI_ASSEMBLER_MAX_CAPE, ItemID.DIZANAS_MAX_CAPE);
    private static final Set<Integer> DIZANAS_QUIVERS = setOf(
        ItemID.DIZANAS_QUIVER_UNCHARGED, ItemID.DIZANAS_QUIVER_L_UNCHARGED,
        ItemID.DIZANAS_QUIVER, ItemID.DIZANAS_QUIVER_L,
        ItemID.BLESSED_DIZANAS_QUIVER, ItemID.BLESSED_DIZANAS_QUIVER_L);
    private static final Set<Integer> QUEST_CAPES = setOf(ItemID.QUEST_POINT_CAPE, ItemID.QUEST_POINT_CAPE_T);

    private final Client client;
    private final Set<String> observedOnce = new HashSet<>();
    private Consumer<VerifiedRuneLiteChallengeEvent> handler;
    private int lastPureEssence = -1;
    private int lastRuneKiteshields = -1;
    private int pendingPureEssence;
    private int pendingPureTick = -100;
    private int pendingRuneKiteshields;
    private int pendingRuneTick = -100;
    private int miningEvidenceTick = -100;
    private int smithingEvidenceTick = -100;

    @Inject
    public GoldenFrameChallengeTracker(Client client)
    {
        this.client = client;
    }

    public void setCompletionHandler(Consumer<VerifiedRuneLiteChallengeEvent> handler)
    {
        this.handler = handler;
    }

    public void resetSession()
    {
        observedOnce.clear();
        lastPureEssence = -1;
        lastRuneKiteshields = -1;
        pendingPureEssence = 0;
        pendingRuneKiteshields = 0;
        miningEvidenceTick = smithingEvidenceTick = -100;
    }

    public void onStatChanged(Skill skill, int gained)
    {
        if (gained <= 0) return;
        int tick = client.getTickCount();
        if (skill == Skill.MINING)
        {
            miningEvidenceTick = tick;
            flushPureEssence(tick);
        }
        else if (skill == Skill.SMITHING)
        {
            smithingEvidenceTick = tick;
            flushRuneKiteshields(tick);
        }
    }

    public void onChatMessage(ChatMessage event)
    {
        String message = event.getMessage() == null ? "" : event.getMessage().toLowerCase();
        int tick = client.getTickCount();
        if (message.contains("mine some pure essence"))
        {
            miningEvidenceTick = tick;
            flushPureEssence(tick);
        }
        if (message.contains("rune kiteshield") && (message.contains("hammer") || message.contains("smith")))
        {
            smithingEvidenceTick = tick;
            flushRuneKiteshields(tick);
        }
    }

    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (event.getContainerId() == InventoryID.EQUIPMENT.getId())
        {
            inspectEquipment(event.getItemContainer());
            return;
        }
        if (event.getContainerId() != InventoryID.INVENTORY.getId()) return;

        ItemContainer inventory = event.getItemContainer();
        int tick = client.getTickCount();
        int pure = inventory.count(ItemID.PURE_ESSENCE);
        int kiteshields = inventory.count(ItemID.RUNE_KITESHIELD);
        if (lastPureEssence >= 0 && pure > lastPureEssence)
        {
            pendingPureEssence += pure - lastPureEssence;
            pendingPureTick = tick;
        }
        if (lastRuneKiteshields >= 0 && kiteshields > lastRuneKiteshields)
        {
            pendingRuneKiteshields += kiteshields - lastRuneKiteshields;
            pendingRuneTick = tick;
        }
        lastPureEssence = pure;
        lastRuneKiteshields = kiteshields;
        flushPureEssence(tick);
        flushRuneKiteshields(tick);
    }

    public void onGameTick()
    {
        int tick = client.getTickCount();
        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        if (inventory != null && lastPureEssence < 0)
        {
            lastPureEssence = inventory.count(ItemID.PURE_ESSENCE);
            lastRuneKiteshields = inventory.count(ItemID.RUNE_KITESHIELD);
        }
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment != null) inspectEquipment(equipment);
        if (Math.abs(tick - pendingPureTick) > EVIDENCE_WINDOW_TICKS) pendingPureEssence = 0;
        if (Math.abs(tick - pendingRuneTick) > EVIDENCE_WINDOW_TICKS) pendingRuneKiteshields = 0;
        if (client.getVarbitValue(VarbitID.KANDARIN_DIARY_ELITE_COMPLETE) > 0)
            emitOnce(RuneLiteChallenge.KANDARIN_ELITE);
    }

    private void inspectEquipment(ItemContainer equipment)
    {
        if (equipment == null) return;
        Set<Integer> equipped = new HashSet<>();
        for (Item item : equipment.getItems()) if (item.getId() > 0) equipped.add(item.getId());
        if (equipped.contains(ItemID.LEDERHOSEN_TOP)
            && equipped.contains(ItemID.LEDERHOSEN_SHORTS)
            && equipped.contains(ItemID.LEDERHOSEN_HAT)) emitOnce(RuneLiteChallenge.LEDERHOSEN);
        if (containsAny(equipped, MAX_CAPES)) emitOnce(RuneLiteChallenge.MAX_CAPE);
        if (containsAny(equipped, DIZANAS_QUIVERS)) emitOnce(RuneLiteChallenge.DIZANAS_QUIVER);
        if (containsAny(equipped, QUEST_CAPES)) emitOnce(RuneLiteChallenge.QUEST_CAPE);
    }

    private void flushPureEssence(int tick)
    {
        if (pendingPureEssence > 0 && Math.abs(tick - pendingPureTick) <= EVIDENCE_WINDOW_TICKS
            && Math.abs(tick - miningEvidenceTick) <= EVIDENCE_WINDOW_TICKS)
        {
            emit(VerifiedRuneLiteChallengeEvent.quantity(RuneLiteChallenge.PURE_ESSENCE, pendingPureEssence));
            pendingPureEssence = 0;
        }
    }

    private void flushRuneKiteshields(int tick)
    {
        if (pendingRuneKiteshields > 0 && Math.abs(tick - pendingRuneTick) <= EVIDENCE_WINDOW_TICKS
            && Math.abs(tick - smithingEvidenceTick) <= EVIDENCE_WINDOW_TICKS)
        {
            emit(VerifiedRuneLiteChallengeEvent.quantity(RuneLiteChallenge.RUNE_KITESHIELD, pendingRuneKiteshields));
            pendingRuneKiteshields = 0;
        }
    }

    private void emitOnce(RuneLiteChallenge challenge)
    {
        if (observedOnce.add(challenge.getKind())) emit(VerifiedRuneLiteChallengeEvent.observed(challenge));
    }

    private void emit(VerifiedRuneLiteChallengeEvent event)
    {
        if (handler != null) handler.accept(event);
    }

    private static boolean containsAny(Set<Integer> equipped, Set<Integer> expected)
    {
        for (Integer itemId : expected) if (equipped.contains(itemId)) return true;
        return false;
    }

    private static Set<Integer> setOf(Integer... values)
    {
        return new HashSet<>(Arrays.asList(values));
    }
}
