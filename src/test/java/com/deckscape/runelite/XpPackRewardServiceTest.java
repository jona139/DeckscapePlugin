package com.deckscape.runelite;

import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class XpPackRewardServiceTest
{
    private final XpPackRewardService service = new XpPackRewardService();

    @Test
    void rollsOnceForEveryCompleteHundredXp()
    {
        XpPackRewardService.Result result = service.roll(25, 225, 100, new Random(1));
        assertEquals(50, result.getRemainder());
        assertEquals(2, result.getAwarded().size());
    }

    @Test
    void zeroChanceStillCarriesRemainder()
    {
        XpPackRewardService.Result result = service.roll(90, 25, 0, new Random(1));
        assertEquals(15, result.getRemainder());
        assertEquals(0, result.getAwarded().size());
    }
}
