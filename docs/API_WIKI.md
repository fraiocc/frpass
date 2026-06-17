# 🛠️ FrPass Developer API Wiki

Welcome to the **FrPass Developer API** documentation! FrPass provides a robust, easy-to-use API and several custom Bukkit Events, allowing you to hook into the Battlepass system seamlessly.

---

## 1. Getting Started

Before using the API, make sure to add FrPass to your project's build path, and add it as a dependency in your `plugin.yml`:

```yaml
depend: [FrPass]
```
*(Use `softdepend` if FrPass is optional for your plugin.)*

---

## 2. Accessing the API

You can access all core functions using the **`FrPassProvider`** class.

```java
import cc.fraio.frpass.api.FrPassProvider;
import cc.fraio.frpass.api.FrPassAPI;

public class MyPlugin {
    public void example() {
        // Get the API instance
        FrPassAPI api = FrPassProvider.get();
    }
}
```

---

## 3. Available API Methods

Here are the methods you can call from the `FrPassAPI` instance:

### `getPlayerLevel(UUID uuid): Int`
Returns the current Battlepass level/tier of the given player.

### `getPlayerXp(UUID uuid): Int`
Returns the current XP progress the player has towards their next level.

### `isPremium(UUID uuid): Boolean`
Checks if the player has unlocked the Premium Pass. Returns `true` if they are Premium.

### `setPremium(UUID uuid, boolean premium)`
Grants or revokes Premium Pass status for a player.
- *Example: `api.setPremium(player.getUniqueId(), true);`*

### `addXp(UUID uuid, int amount)`
Safely adds XP to a player. If they exceed the required XP for the current tier, they will automatically level up, and the level-up event will be fired.

### `addQuestProgress(Player player, QuestType type, int amount)`
Progresses any active quests the player has matching the specified `QuestType`.
- *Example: `api.addQuestProgress(player, QuestType.MINE_BLOCK, 1);`*

---

## 4. Custom Events

FrPass fires standard Bukkit events that you can listen to using the `@EventHandler` annotation.

### `FrPassLevelUpEvent`
Fired when a player gains enough XP to level up their Battlepass.
- **`getPlayer()`**: The player who leveled up.
- **`getOldLevel()`**: Their previous tier.
- **`getNewLevel()`**: Their newly achieved tier.
- *Note: This event implements `Cancellable`. If cancelled, the player will not level up and their XP will be refunded.*

```java
@EventHandler
public void onLevelUp(FrPassLevelUpEvent event) {
    if (event.getNewLevel() == 50) {
        Bukkit.broadcastMessage(event.getPlayer().getName() + " reached the max tier!");
    }
}
```

### `FrPassTierClaimEvent`
Fired when a player clicks to claim a reward from the Battlepass GUI.
- **`getPlayer()`**: The player claiming the reward.
- **`getTier()`**: The integer tier level they are claiming.
- **`isPremium()`**: `true` if they claimed the Premium reward, `false` for the Free reward.

```java
@EventHandler
public void onClaim(FrPassTierClaimEvent event) {
    event.getPlayer().sendMessage("You just claimed a " + (event.isPremium() ? "Premium" : "Free") + " reward!");
}
```

### `FrPassQuestProgressEvent`
Fired whenever a player makes progress on a quest.
- **`getPlayer()`**: The player doing the quest.
- **`getQuest()`**: The `Quest` object (contains quest ID, required amount, etc).
- **`getOldProgress()`**: Previous progress amount.
- **`getNewProgress()`**: New progress amount.
- **`isCompleted()`**: `true` if this progress just completed the quest.

```java
@EventHandler
public void onQuestProgress(FrPassQuestProgressEvent event) {
    if (event.isCompleted()) {
        event.getPlayer().sendMessage("Awesome! You completed the " + event.getQuest().getId() + " quest!");
    }
}
```

---
> *Need more hooks or specific API methods? Feel free to open an issue or submit a Pull Request on our GitHub repository!*
