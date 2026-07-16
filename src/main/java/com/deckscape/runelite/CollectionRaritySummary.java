package com.deckscape.runelite;

import com.deckscape.runelite.model.CardCatalog;
import com.deckscape.runelite.model.DeckscapeCard;
import com.deckscape.runelite.model.DeckscapeState;
import java.util.EnumMap;
import java.util.Map;

/** Builds the local !deckscape collection readout without sending account data anywhere. */
public final class CollectionRaritySummary
{
    private static final Map<DeckscapeCard.Rarity, String> COLORS = new EnumMap<>(DeckscapeCard.Rarity.class);

    static
    {
        COLORS.put(DeckscapeCard.Rarity.COMMON, "b8b8b8");
        COLORS.put(DeckscapeCard.Rarity.UNCOMMON, "5fc56e");
        COLORS.put(DeckscapeCard.Rarity.RARE, "5b9dff");
        COLORS.put(DeckscapeCard.Rarity.EPIC, "c978ff");
        COLORS.put(DeckscapeCard.Rarity.LEGENDARY, "ffb347");
    }

    private CollectionRaritySummary() { }

    public static String build(DeckscapeState state)
    {
        Map<DeckscapeCard.Rarity, Integer> ownedUnique = new EnumMap<>(DeckscapeCard.Rarity.class);
        Map<DeckscapeCard.Rarity, Integer> available = new EnumMap<>(DeckscapeCard.Rarity.class);
        int copies = 0;
        for (DeckscapeCard card : CardCatalog.all())
        {
            available.merge(card.getRarity(), 1, Integer::sum);
            int owned = Math.max(0, state.getCollection().getOrDefault(card.getId(), 0));
            copies += owned;
            if (owned > 0) ownedUnique.merge(card.getRarity(), 1, Integer::sum);
        }

        StringBuilder result = new StringBuilder("<col=ffd45e>Deckscape collection</col>: ");
        DeckscapeCard.Rarity[] rarities = DeckscapeCard.Rarity.values();
        for (int i = 0; i < rarities.length; i++)
        {
            DeckscapeCard.Rarity rarity = rarities[i];
            if (i > 0) result.append(" | ");
            result.append("<col=").append(COLORS.get(rarity)).append('>')
                .append(title(rarity.name())).append(' ')
                .append(ownedUnique.getOrDefault(rarity, 0)).append('/')
                .append(available.getOrDefault(rarity, 0)).append("</col>");
        }
        return result.append(" | ").append(copies).append(" total copies").toString();
    }

    private static String title(String value)
    {
        String lower = value.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
