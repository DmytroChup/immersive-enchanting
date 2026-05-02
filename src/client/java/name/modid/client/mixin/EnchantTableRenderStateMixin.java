package name.modid.client.mixin;

import name.modid.client.fsm.IImmersiveRenderState;
import net.minecraft.client.renderer.blockentity.state.EnchantTableRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EnchantTableRenderState.class)
public class EnchantTableRenderStateMixin implements IImmersiveRenderState {

    @Unique
    private final ItemStackRenderState immersiveItemState = new ItemStackRenderState();

    @Unique
    private boolean immersiveActive = false;

    @Unique
    private float immersiveAngle = 0.0f;

    @Unique
    private float immersiveBobbing = 0.0f;

    @Unique
    private final ItemStackRenderState runeItemState = new ItemStackRenderState();

    @Unique
    private float renderTime = 0.0f;

    @Override
    public ItemStackRenderState getImmersiveItemState() { return this.immersiveItemState; }

    @Override
    public boolean isImmersiveActive() { return this.immersiveActive; }

    @Override
    public void setImmersiveActive(boolean active) { this.immersiveActive = active; }

    @Override
    public float getImmersiveAngle() { return this.immersiveAngle; }

    @Override
    public void setImmersiveAngle(float angle) { this.immersiveAngle = angle; }

    @Override
    public float getImmersiveBobbing() { return this.immersiveBobbing; }

    @Override
    public void setImmersiveBobbing(float bobbing) { this.immersiveBobbing = bobbing; }

    @Override
    public ItemStackRenderState getRuneItemState() { return this.runeItemState; }

    @Override
    public float getRenderTime() { return this.renderTime; }

    @Override
    public void setRenderTime(float time) { this.renderTime = time; }
}