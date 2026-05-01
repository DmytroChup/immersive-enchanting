package name.modid.client.fsm;

import net.minecraft.client.renderer.item.ItemStackRenderState;

public interface IImmersiveRenderState {
    ItemStackRenderState getImmersiveItemState();
    boolean isImmersiveActive();
    void setImmersiveActive(boolean active);

    float getImmersiveAngle();
    void setImmersiveAngle(float angle);

    float getImmersiveBobbing();
    void setImmersiveBobbing(float bobbing);
}