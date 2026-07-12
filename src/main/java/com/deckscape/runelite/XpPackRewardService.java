package com.deckscape.runelite;

import com.deckscape.runelite.model.PackType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class XpPackRewardService
{
    public Result roll(int currentRemainder, int gained, int chancePercent, Random random)
    {
        int total = Math.max(0, currentRemainder) + Math.max(0, gained);
        int chance = Math.max(0, Math.min(100, chancePercent));
        List<PackType> awarded = new ArrayList<>();
        while (total >= 100)
        {
            total -= 100;
            if (random.nextInt(100) < chance)
            {
                awarded.add(PackType.values()[random.nextInt(PackType.values().length)]);
            }
        }
        return new Result(total, awarded);
    }

    public static final class Result
    {
        private final int remainder;
        private final List<PackType> awarded;

        private Result(int remainder, List<PackType> awarded)
        {
            this.remainder = remainder;
            this.awarded = Collections.unmodifiableList(new ArrayList<>(awarded));
        }

        public int getRemainder() { return remainder; }
        public List<PackType> getAwarded() { return awarded; }
    }
}
