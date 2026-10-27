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
- **Free and Premium tracks.** Rewards are claimed separately. Players who buy Premium late can go back and claim every Premium reward they already unlocked.
- **Daily and weekly quests.** Mining, mob kills, fishing, crafting, playtime and more. Players can reroll quests directly in the Quests GUI.
- **In-game Quest Editor.** Create and edit quests directly in-game using `/frpassadmin editor`.
- **AI Quest Generator.** Generate or modify quest configurations on the fly with natural language prompts (`/frpassadmin ai create` & `/frpassadmin ai edit`).
- **Premium gifting.** Players can gift Premium to others, with configurable costs (Vault money, Pass XP or Pass level).
- **Premium Ticket item.** A physical, tradeable item that unlocks Premium on right-click. If inventory is full, tickets are safely queued and delivered when space opens up.
- **Real-time hot reload (FrSync).** Config, language, quest, and menu changes apply immediately without `/reload` or server restarts.
- **MiniMessage, Hex, & PAPI Support.** Full text formatting support with MiniMessage tags, RGB hex (`&#RRGGBB`), and PlaceholderAPI placeholders.
- **Native Folia Support.** Fully thread-safe asynchronous schedulers and database transactions.

## Installation

1. Install [PacketEvents](https://github.com/retrooper/packetevents) (required). [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) and [Vault](https://www.spigotmc.org/resources/vault.34315/) are optional but recommended.
2. Drop the FrPass jar into your `plugins/` folder and restart the server.
3. Edit the generated files in `plugins/FrPass/` to set up tiers, rewards and quests.
4. Run `/frpass` in-game.

## Commands

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/frpass` | Open the Battlepass menu | None |
| `/frpass gift <player>` | Gift Premium Battlepass to a player | None |
| `/frpassadmin editor` | Open the in-game Quest Editor | `frpass.admin` |
| `/frpassadmin ai create [file.yml] <prompt>` | Generate a new quest via AI | `frpass.admin` |
| `/frpassadmin ai edit <quest_id> <prompt>` | Modify an existing quest via AI | `frpass.admin` |
| `/frpassadmin ai status` | Check AI generator status | `frpass.admin` |
| `/frpassadmin addxp <player> <amount>` | Add Battlepass XP to a player | `frpass.admin` |
| `/frpassadmin setpremium <player> <true/false>` | Grant or remove Premium status | `frpass.admin` |
| `/frpassadmin giveticket <player> <amount>` | Give physical Premium Tickets | `frpass.admin` |

## Placeholders

| Placeholder | Description |
| :--- | :--- |
| `%frpass_level%` | Current level/tier |
| `%frpass_xp%` | Current XP towards the next tier |
| `%frpass_is_premium%` | `true` or `false` |
| `%frpass_premium_status%` | Formatted Premium status text |
| `%frpass_active_quests%` | Number of active quests |

## Developer API

Add FrPass as a dependency in your `plugin.yml`:

```yaml
depend: [FrPass]   # or softdepend
```

```java
FrPassAPI api = FrPassProvider.get();
api.addXp(player.getUniqueId(), 100);
api.addQuestProgress(player, QuestType.BREAK_BLOCK, 1);
```

| Method | Description |
| :--- | :--- |
| `getPlayerLevel(UUID)` | Current level |
| `getPlayerXp(UUID)` | XP towards the next level |
| `isPremium(UUID)` | Whether the player has Premium |
| `setPremium(UUID, boolean)` | Grant or revoke Premium |
| `addXp(UUID, int)` | Add XP, handles level ups |
| `addQuestProgress(Player, QuestType, int)` | Progress matching quests |

**Quest types:** `BREAK_BLOCK`, `PLACE_BLOCK`, `KILL_PLAYER`, `KILL_MOB`, `FISH`, `CRAFT`, `PLAYTIME`, `WALK`, `CONSUME`, `BREED_ANIMAL`, `ENCHANT_ITEM`, `EXP_EARN`, `SHEAR_SHEEP`

### Events

| Event | Fired when | Cancellable |
| :--- | :--- | :--- |
| `FrPassLevelUpEvent` | A player levels up (`getPlayer`, `getOldLevel`, `getNewLevel`) | Yes, XP is refunded |
| `FrPassTierClaimEvent` | A reward is claimed (`getPlayer`, `getTier`, `isPremium`) | No |
| `FrPassQuestProgressEvent` | Quest progress changes (`getPlayer`, `getQuest`, `getOldProgress`, `getNewProgress`, `isCompleted`) | No |

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
