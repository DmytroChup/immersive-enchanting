# Immersive Enchanting

**Immersive Enchanting** is a client-friendly Fabric mod that completely overhauls the vanilla enchanting experience. It transforms the boring, static GUI process into a dynamic, cinematic 3D in-world event.

![Showcase GIF](https://github.com/user-attachments/assets/25b9b7b7-5bc6-4ea5-acfe-82cd048a13f8)

## ✨ Features

* **Cinematic Crafting Animation:** When you enchant an item, it doesn't just instantly appear in a slot. The item levitates above the table, surrounded by orbiting elemental runes, a magical energy sphere, and dynamic lighting effects.
* **Fast Pickup System:** No need to reopen the GUI after crafting! Once the cinematic animation finishes and the enchanted item gently hovers over the table, simply **Right-Click** the table to instantly pop the item back into your inventory.
* **Multiplayer Synchronized:** Built with a robust internal State Machine, the animations and item states are perfectly synced between the server and all clients. If you walk away and a chunk unloads, the animation will seamlessly resume when you return.
* **Safe & Optimized:** Carefully engineered to avoid TPS drops. Global block updates are minimal, and the mod ensures items safely drop into the world if the table is broken or blown up during an animation.

## 📥 Installation & Requirements

* **Minecraft:** 1.21.x
* **Mod Loader:** [Fabric](https://fabricmc.net/)
* **Dependencies:** [Fabric API](https://modrinth.com/mod/fabric-api) is strictly required.

Simply drop the `.jar` file into your `mods` folder along with the Fabric API. This mod needs to be installed on **both the Client and the Server** to function correctly in multiplayer.

## 🤝 Compatibility & Modpacks

* **Modpacks:** You are 100% free to include Immersive Enchanting in your modpacks. No need to ask for permission (though credit is always appreciated!).

## 🐛 Bug Reports & Source Code

Found a bug or want to see how the magic works under the hood? Check out the [GitHub Repository](https://github.com/DmytroChup/immersive-enchanting/issues).

**License:** MIT