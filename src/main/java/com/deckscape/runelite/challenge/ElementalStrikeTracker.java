package com.deckscape.runelite.challenge;

import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.GraphicChanged;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;

@Singleton
public final class ElementalStrikeTracker
{
    private static final int CAST_WINDOW_TICKS = 8;
    private final Client client;
    private final ItemManager itemManager;
    private PendingCast pendingCast;
    private Consumer<VerifiedRuneLiteChallengeEvent> completionHandler;

    @Inject
    public ElementalStrikeTracker(Client client, ItemManager itemManager)
    {
        this.client = client;
        this.itemManager = itemManager;
    }

    public void setCompletionHandler(Consumer<VerifiedRuneLiteChallengeEvent> completionHandler)
    {
        this.completionHandler = completionHandler;
    }

    public void onGraphicChanged(GraphicChanged event)
    {
        if (event.getActor() != client.getLocalPlayer()) return;
        int graphicId = client.getLocalPlayer().getGraphic();
        ElementalSpell spell = ElementalSpell.fromGraphic(graphicId);
        if (spell != null)
        {
            pendingCast = new PendingCast(spell, client.getTickCount(), calculateMaxHit(spell.baseMaxHit), client.getLocalPlayer().getInteracting());
        }
    }

    public void onHitsplatApplied(HitsplatApplied event)
    {
        PendingCast cast = pendingCast;
        if (cast == null || client.getTickCount() - cast.tick > CAST_WINDOW_TICKS)
        {
            pendingCast = null;
            return;
        }
        if (!event.getHitsplat().isMine() || cast.target == null || event.getActor() != cast.target) return;
        
        int damage = event.getHitsplat().getAmount();
        pendingCast = null;
        
        if (damage == cast.maxHit && completionHandler != null)
        {
            completionHandler.accept(new VerifiedRuneLiteChallengeEvent(cast.spell.contractName, damage, cast.maxHit));
        }
    }

    private int calculateMaxHit(int base)
    {
        float magicDamage = 0f;
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment != null)
        {
            for (Item item : equipment.getItems())
            {
                if (item.getId() <= 0) continue;
                ItemStats stats = itemManager.getItemStats(item.getId());
                ItemEquipmentStats equipmentStats = stats == null ? null : stats.getEquipment();
                if (equipmentStats != null) magicDamage += equipmentStats.getMdmg();
            }
        }
        if (magicDamage > 1f) magicDamage /= 100f;
        return Math.max(base, (int) Math.floor(base * (1f + magicDamage)));
    }

    private enum ElementalSpell
    {
        WIND("WIND_STRIKE", 90, 2),
        WATER("WATER_STRIKE", 93, 4),
        EARTH("EARTH_STRIKE", 96, 6),
        FIRE("FIRE_STRIKE", 99, 8);

        private final String contractName;
        private final int castGfx;
        private final int baseMaxHit;

        ElementalSpell(String contractName, int castGfx, int baseMaxHit)
        {
            this.contractName = contractName;
            this.castGfx = castGfx;
            this.baseMaxHit = baseMaxHit;
        }

        private static ElementalSpell fromGraphic(int gfxId)
        {
            for (ElementalSpell spell : values())
            {
                if (spell.castGfx == gfxId) return spell;
            }
            return null;
        }
    }

    private static final class PendingCast
    {
        private final ElementalSpell spell;
        private final int tick;
        private final int maxHit;
        private final Actor target;

        private PendingCast(ElementalSpell spell, int tick, int maxHit, Actor target)
        {
            this.spell = spell;
            this.tick = tick;
            this.maxHit = maxHit;
            this.target = target;
        }
    }
}
