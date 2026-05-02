package tnpl.immersiveenchanting.fsm;

public enum TableState {
    IDLE,             // The table is empty, waiting for an item
    ITEM_INSERTED,    // An item on the altar; the runes haven't appeared yet
    RUNE_SELECTION,   // Runes are loaded (rendered), waiting for clicks (raycast)
    CRAFTING,         // The enchantment animation plays
    FAILED            // Error (invalid combination or insufficient resources)
}
