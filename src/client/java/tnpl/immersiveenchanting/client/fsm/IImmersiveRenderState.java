package tnpl.immersiveenchanting.client.fsm;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import tnpl.immersiveenchanting.fsm.TableState;

public interface IImmersiveRenderState {
    ItemStackRenderState getImmersiveItemState();
    boolean isImmersiveActive();
    void setImmersiveActive(boolean active);

    float getImmersiveAngle();
    void setImmersiveAngle(float angle);

    float getImmersiveBobbing();
    void setImmersiveBobbing(float bobbing);

    // --- Rune Rendering Properties ---
    ItemStackRenderState getRuneItemState(int index);
    ItemStackRenderState getOrbItemState(int index);
    float getRenderTime();
    void setRenderTime(float time);

    boolean areRunesVisible();
    void setRunesVisible(boolean visible);

    int getAnimationTick();
    void setAnimationTick(int tick);

    TableState getTableState();
    void setTableState(TableState state);

    ItemStackRenderState getMagicCircleState();
    ItemStackRenderState getPillarState();
}