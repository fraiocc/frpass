<div align="center">

# ✨ FrPass

**A fast, lightweight, and modern Battlepass & Quests system for your Minecraft server.**

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blueviolet.svg?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)
[![Paper](https://img.shields.io/badge/Paper-1.21-black.svg?style=for-the-badge&logo=paper)](https://papermc.io)
[![Folia](https://img.shields.io/badge/Folia-Support-4CAF50.svg?style=for-the-badge)](https://papermc.io)
[![PacketEvents](https://img.shields.io/badge/Powered_by-PacketEvents-ff69b4.svg?style=for-the-badge)](https://github.com/retrooper/packetevents)

<br>

FrPass is designed to give your players the best Battlepass experience without causing any server lag. It uses **PacketEvents** to handle all GUI interactions, making it incredibly fast and completely safe from item duplication bugs.

</div>

---

## ⚡ Features

### 🛡️ Lag-Free & Safe GUIs
All menus in FrPass are handled through network packets. This means players can navigate the Battlepass smoothly, and your server won't suffer from performance drops or traditional inventory glitches.

### 💎 Free & Premium Rewards
FrPass features a clean, modern menu layout. Players can easily view and claim their Free and Premium rewards separately. If a player reaches a high tier and decides to buy the Premium pass later, they can easily go back and claim all the past Premium rewards they unlocked!

### 📜 Built-in Quests System
Keep your players engaged with our integrated questing system! You can set up custom quests (like mining diamonds, killing mobs, or playtime) that reward players with Battlepass XP when completed.

### 🔌 Developer API
FrPass comes with a comprehensive API and custom Bukkit Events. Other developers can easily hook into FrPass to give custom items when players level up, or trigger global announcements when a quest is completed!

### 🎟️ Premium Ticket & Auto-Queue
Players can receive a physical "Premium Pass Ticket" item to consume. If their inventory is full, the system safely queues the ticket and automatically gives it when space opens up!

### ⚙️ Auto-Reload (Zero Lag)
No more typing `/reload`. FrPass uses an invisible asynchronous background watcher. Whenever you save a `.yml` file, the plugin instantly hot-reloads it in real-time without skipping a single tick!

---

## 🚀 Quick Setup

1. **Install Dependencies:** You must install the `PacketEvents` plugin on your server. We also highly recommend `PlaceholderAPI` and `Vault`.
2. **Install FrPass:** Place `FrPass-0.2.0.jar` into your `plugins` folder.
3. **Restart your server:** Start your server so the default files are generated.
4. **Configure:** Set up your tiers, rewards, and quests in the configuration files.
5. **Done:** Use `/frpass` in-game to open the Battlepass!

---

## 🕹️ Commands & Permissions

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/frpass` | *None* | Opens the Battlepass menu. |
| `/frpassadmin` | `frpass.admin` | View admin commands. |
| `/frpassadmin addxp <player> <amount>` | `frpass.admin` | Add or set XP for a player. |
| `/frpassadmin setpremium <player> <true/false>` | `frpass.admin` | Give or remove Premium Pass status. |
| `/frpassadmin giveticket <player> <amount>` | `frpass.admin` | Give a physical Premium Ticket. |

---

## 🧩 Placeholders (PAPI)

You can use these placeholders anywhere (scoreboards, chat, etc.):
- `%frpass_level%` - Current Battlepass Tier/Level
- `%frpass_xp%` - Current XP
- `%frpass_is_premium%` - 'Yes' or 'No'
- `%frpass_premium_status%` - Formatted text for Premium status

---

<div align="center">
  <sub>Built with ❤️ by Fraio.</sub>
</div>
