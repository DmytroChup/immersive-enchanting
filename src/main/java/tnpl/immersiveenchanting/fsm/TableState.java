package tnpl.immersiveenchanting.fsm;

public enum TableState {
    IDLE,               // The table is empty, waiting for an item
    ITEM_INSERTED,      // An item on the altar; the runes haven't appeared yet
    READY_TO_ENCHANT,   // Runes are loaded (rendered)
    CRAFTING            // The enchantment animation plays
}
