<div align="center">

<a href="https://fraio.cc"><img src="https://cdn.fraio.cc/fraio.svg" alt="Fraio" width="96"></a>

# FrPass

**Fast, packet-based Battlepass & Quests for your Minecraft server.**

Supports Paper, Spigot, Purpur and **Folia**.

<p align="center">
  <a href="https://modrinth.com/plugin/frpass"><img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg" alt="Available on Modrinth" height="48"></a>&nbsp;&nbsp;&nbsp;
  <a href="https://discord.gg/N9Jr94Wvcf"><img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-plural_vector.svg" alt="Join our Discord" height="48"></a>
</p>

Found a bug? Report it at [report.fraio.cc](https://report.fraio.cc)

</div>

---

## Features

- **Lag-free menus.** All GUIs are handled with packets through PacketEvents, so there is no main-thread load and no GUI item duplication.
- **Free and Premium tracks.** Rewards are claimed separately or all at once with the **Claim All** button. Players who buy Premium late can go back and claim every Premium reward they already unlocked.
- **Daily and weekly quests.** Mining, mob kills, fishing, crafting, playtime and more, with optional **Action Bar** progress updates and an **all-quests completion bonus**. Players can reroll quests directly in the Quests GUI, and a configurable timer button shows when quests reset.
- **XP Boost & Premium Multiplier.** Trigger timed server-wide XP boosts with `/frpassadmin boost <multiplier> <duration>` and grant Premium players a permanent XP multiplier.
- **Leaderboard.** `/frpass top` ranks players by tier and XP, with an optional GUI (`menus/leaderboard_menu.yml`) and `%frpass_top_<n>_name%` placeholders.
- **In-game Quest & Tier Editor.** Create, edit, duplicate and delete quests or Battlepass tiers directly in-game using `/frpassadmin editor`, complete with live previews, click-with-item icon copying (vanilla or Nexo), and custom model data.
- **Multi-language support.** Built-in English (`en`), Turkish (`tr`), German (`de`), and French (`fr`) localizations.
- **AI Quest Generator.** Generate or modify quest configurations on the fly with natural language prompts (`/frpassadmin ai create` & `/frpassadmin ai edit`).
- **Flexible rewards.** Run console or player commands, send messages, broadcast, play sounds, show titles, give money, XP or items (including Nexo items).
- **Unclaimed reward reminder.** Players with unclaimed rewards are notified shortly after joining (can be disabled in `config.yml`).
- **Premium gifting.** Players can gift Premium to others, with configurable costs (Vault money, Pass XP or Pass level).
- **Premium Ticket item.** A physical, tradeable item that unlocks Premium on right-click. If inventory is full, tickets are safely queued and delivered when space opens up.
- **Exploit protection.** Placed blocks, creative mode, vehicles and flying, self-kills and NPCs do not give quest progress, and protected regions are respected.
- **Real-time hot reload (FrSync) & auto-migration.** Config, language, quest, and menu changes apply immediately without `/reload` or server restarts, and new settings merge automatically on plugin updates.
- **Full MiniMessage, Hex, & PAPI Support.** Native Adventure Component rendering with gradients, `<click>`, `<hover>`, RGB hex (`&#RRGGBB`), and PlaceholderAPI placeholders.
- **Native Folia & High-Concurrency Support.** Thread-safe async schedulers, JDBC batch auto-saves, and SQLite/MySQL/MariaDB/PostgreSQL drivers.

## Installation

1. Install [PacketEvents](https://github.com/retrooper/packetevents) (required). [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/), [Vault](https://www.spigotmc.org/resources/vault.34315/) and [Nexo](https://nexomc.com) are optional but recommended.
2. Drop the FrPass jar into your `plugins/` folder and restart the server.
3. Edit the generated files in `plugins/FrPass/` to set up tiers, rewards and quests.
4. Run `/frpass` in-game.

## Commands

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/frpass` | Open the Battlepass menu | None |
| `/frpass top` | Open the leaderboard (or print it in chat if the menu is disabled) | None |
| `/frpass gift <player>` | Gift Premium Battlepass to a player | `frpass.gift` |
| `/frpassadmin editor [quests\|tiers]` | Open the in-game Quest & Tier Editor | `frpass.admin` |
| `/frpassadmin boost <multiplier\|off> [duration]` | Manage global XP boost (e.g. `2.0 1h`) | `frpass.admin` |
| `/frpassadmin ai create [file.yml] <prompt>` | Generate a new quest via AI | `frpass.admin` |
| `/frpassadmin ai edit <quest_id> <prompt>` | Modify an existing quest via AI | `frpass.admin` |
| `/frpassadmin ai status` | Check AI generator status | `frpass.admin` |
| `/frpassadmin addxp <player> <amount>` | Add Battlepass XP to a player | `frpass.admin` |
| `/frpassadmin setxp <player> <amount>` | Set a player's Battlepass XP | `frpass.admin` |
| `/frpassadmin setlevel <player> <level>` | Set a player's level | `frpass.admin` |
| `/frpassadmin reset <player>` | Reset a player's Battlepass data | `frpass.admin` |
| `/frpassadmin completequest <player> <quest_id>` | Complete a quest for a player | `frpass.admin` |
| `/frpassadmin setpremium <player> <true/false>` | Grant or remove Premium status | `frpass.admin` |
| `/frpassadmin giveticket <player> <amount>` | Give physical Premium Tickets | `frpass.admin` |

## Rewards

Rewards are lists of strings in `core/tiers.yml` and quest `extra-rewards`. `%player%` is replaced with the player's name.

| Type | Example |
| :--- | :--- |
| `[COMMAND]` | `[COMMAND] give %player% diamond 5` (console) |
| `[PLAYER_COMMAND]` | `[PLAYER_COMMAND] spawn` (run by the player) |
| `[MESSAGE]` | `[MESSAGE] &aWell done!` |
| `[BROADCAST]` | `[BROADCAST] &6%player% reached the max tier!` |
| `[SOUND]` | `[SOUND] ENTITY_PLAYER_LEVELUP 1.0 1.0` |
| `[TITLE]` | `[TITLE] &6Reward!|&7Enjoy` |
| `[MONEY]` | `[MONEY] 1000` (requires Vault) |
| `[XP]` | `[XP] 100` (vanilla XP) |
| `[ITEM]` | `[ITEM] DIAMOND:3` or `[ITEM] nexo:ruby_sword:1 name:"My_Sword"` |

## Placeholders

| Placeholder | Description |
| :--- | :--- |
| `%frpass_level%` / `%frpass_tier%` | Current level/tier |
| `%frpass_xp%` | Current XP towards the next tier |
| `%frpass_xp_required%` | XP required for the next tier |
| `%frpass_xp_remaining%` | XP remaining for the next tier |
| `%frpass_is_premium%` | `true` or `false` |
| `%frpass_premium_formatted%` | Formatted Premium status text |
| `%frpass_active_quests%` | Number of active quests |
| `%frpass_quest_reset%` | Time until quests reset (`-` when quest mode is `ALL`) |
| `%frpass_boost%` | Effective XP multiplier for the player (e.g. `2.0x`) |
| `%frpass_boost_remaining%` | Remaining time for the active global XP boost |
| `%frpass_top_<n>_name%` | Name of the player at rank `n` |
| `%frpass_top_<n>_tier%` | Tier of the player at rank `n` |
| `%frpass_top_<n>_xp%` | XP of the player at rank `n` |

## Developer API

Add FrPass as a dependency in your `plugin.yml`:

```yaml
depend: [FrPass]   # or softdepend
```

```java
if (FrPassProvider.isAvailable()) {
    FrPassAPI api = FrPassProvider.get(); // Also registered via Bukkit ServicesManager
    api.addXp(player.getUniqueId(), 100);
    api.addQuestProgress(player, QuestType.BREAK_BLOCK, 1);
}
```

| Method | Description |
| :--- | :--- |
| `isLoaded(UUID)` | Whether the player's data is loaded in memory |
| `getPlayerLevel(UUID)` | Current level |
| `getPlayerXp(UUID)` | XP towards the next level |
| `isPremium(UUID)` | Whether the player has Premium |
| `setPremium(UUID, boolean)` | Grant or revoke Premium |
| `addXp(UUID, int)` | Add XP, handles level ups |
| `setXp(UUID, int)` | Set XP, handles level ups |
| `setLevel(UUID, int)` | Set the level |
| `resetPlayer(UUID)` | Reset all Battlepass data |
| `giveTicket(Player, int)` | Give Premium Tickets (queued if inventory is full) |
| `getUnclaimedRewardCount(UUID)` | Number of unclaimed rewards |
| `getRequiredXpForNextLevel(UUID)` | XP required for the next level |
| `getRemainingXpForNextLevel(UUID)` | XP remaining for the next level |
| `getActiveQuests(UUID)` | Active quest IDs |
| `getQuestProgress(UUID, String)` | Progress of a quest |
| `isQuestCompleted(UUID, String)` | Whether a quest is completed |
| `completeQuest(Player, String)` | Complete a quest, `false` if already completed |
| `addQuestProgress(Player, QuestType, int)` | Progress matching quests |
| `addQuestProgress(Player, String, int)` | Progress a specific quest by ID |
| `getQuest(String)` / `getAllQuests()` | Inspect loaded quest definitions |
| `getTier(int)` / `getMaxTier()` | Inspect Battlepass tier definitions |
| `getGlobalBoostMultiplier()` / `getEffectiveBoostMultiplier(Player)` | Query active boost multipliers |
| `setGlobalBoost(double, long)` / `clearGlobalBoost()` | Manage timed global XP boosts |
| `getLeaderboard()` / `getLeaderboardEntry(int)` | Query cached leaderboard rankings |
| `openMainMenu` / `openQuestsMenu` / `openPassMenu` / `openLeaderboardMenu` | Open FrPass menus for a player |

**Quest types:** `BREAK_BLOCK`, `PLACE_BLOCK`, `KILL_PLAYER`, `KILL_MOB`, `FISH`, `CRAFT`, `PLAYTIME`, `WALK`, `CONSUME`, `BREED_ANIMAL`, `ENCHANT_ITEM`, `EXP_EARN`, `SHEAR_SHEEP`

### Events

Events are asynchronous when fired from a non-primary thread (Folia).

| Event | Fired when | Cancellable |
| :--- | :--- | :--- |
| `FrPassLevelUpEvent` | A player levels up (`getPlayer`, `getOldLevel`, `getNewLevel`) | Yes, XP is refunded |
| `FrPassTierClaimEvent` | A reward is about to be claimed (`getPlayer`, `getTierLevel`, `isPremiumReward`) | Yes, nothing is given |
| `FrPassQuestProgressEvent` | Quest progress is about to change (`getPlayer`, `getQuest`, `getOldProgress`, `getNewProgress`, `isCompleted`) | Yes, progress is not applied |

```java
@EventHandler
public void onLevelUp(FrPassLevelUpEvent event) {
    if (event.getNewLevel() == 50) {
        Bukkit.broadcastMessage(event.getPlayer().getName() + " reached the max tier!");
    }
}
```

## Contributing

Issues and pull requests are welcome.

## License

LGPL-3.0

---

<div align="center">
  <sub>Made by Fraio</sub>
</div>
