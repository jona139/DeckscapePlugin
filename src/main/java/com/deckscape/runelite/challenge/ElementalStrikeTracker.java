package com.deckscape.runelite.challenge;

import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.GraphicChanged;

@Singleton
public final class ElementalStrikeTracker
{
    private static final int CAST_WINDOW_TICKS = 8;
    private final Client client;
    private PendingCast pendingCast;
    private Consumer<VerifiedRuneLiteChallengeEvent> completionHandler;

    @Inject
    public ElementalStrikeTracker(Client client)
    {
        this.client = client;
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
            pendingCast = new PendingCast(spell, client.getTickCount(), client.getLocalPlayer().getInteracting());
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
        
        if (damage >= 16 && completionHandler != null)
        {
            completionHandler.accept(VerifiedRuneLiteChallengeEvent.strike(cast.spell.contractName, damage));
        }
    }

    private enum ElementalSpell
    {
        WIND("WIND_STRIKE", 90),
        WATER("WATER_STRIKE", 93),
        EARTH("EARTH_STRIKE", 96),
        FIRE("FIRE_STRIKE", 99);

        private final String contractName;
        private final int castGfx;

        ElementalSpell(String contractName, int castGfx)
        {
            this.contractName = contractName;
            this.castGfx = castGfx;
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
        private final Actor target;

        private PendingCast(ElementalSpell spell, int tick, Actor target)
        {
            this.spell = spell;
            this.tick = tick;
            this.target = target;
        }
    }
}
