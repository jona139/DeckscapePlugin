# RuneLite XP reward economy expectations

These are long-run averages for eligible XP earned on normal main-game worlds. They exclude Stardust from opening packs, challenge rewards, website play, promotions, and any future non-XP reward source.

## Reward-roll assumptions

- One server-authoritative roll per **35,000 eligible XP**.
- **45%** core pack: General, Combat, or Skilling with equal probability (15% each per roll).
- **35%** Coins: uniformly distributed integer amount from **200 through 400**, averaging 300 when Coins roll.
- **1%**: 1 Stardust.
- **19%**: no reward.
- Faction packs, including Misthalin packs, cannot roll from XP.

Each roll therefore produces an average of **0.45 packs, 105 Coins, and 0.01 Stardust**. In XP terms, that is approximately one pack per 77,778 XP, one coin reward per 100,000 XP, and one Stardust per 3.5 million XP. Actual short sessions will vary substantially.

## Expected hourly income

Activity descriptions are illustrative; the XP/hour figure is the input that determines the expectation.

| Eligible XP/hour | Example pace | Rolls/hour | Core packs/hour | Each core pack/hour | Coins/hour | Stardust/hour |
|---:|---|---:|---:|---:|---:|---:|
| 25,000 | Low-intensity activity | 0.71 | 0.32 | 0.11 | 75 | 0.007 |
| 50,000 | Moderate activity | 1.43 | 0.64 | 0.21 | 150 | 0.014 |
| 100,000 | Efficient skilling/combat | 2.86 | 1.29 | 0.43 | 300 | 0.029 |
| 200,000 | Fast training method | 5.71 | 2.57 | 0.86 | 600 | 0.057 |
| 400,000 | High-intensity method | 11.43 | 5.14 | 1.71 | 1,200 | 0.114 |
| 800,000 | Extreme/burst XP method | 22.86 | 10.29 | 3.43 | 2,400 | 0.229 |

Formulae for any other activity rate `X`:

- rolls/hour = `X / 35,000`
- packs/hour = `X / 35,000 × 0.45`
- Coins/hour = `X / 35,000 × 0.35 × 300`
- Stardust/hour = `X / 35,000 × 0.01`

The server validates and applies rolls. Leagues, Deadman, Last Man Standing, PvP/high-risk worlds, PvP Arena, beta, tournament, speedrunning, no-save, and other excluded world types contribute no eligible XP.
