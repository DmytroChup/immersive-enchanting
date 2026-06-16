package tnpl.immersiveenchanting.client.mixin;

import tnpl.immersiveenchanting.client.fsm.IImmersiveRenderState;
import net.minecraft.client.renderer.blockentity.state.EnchantTableRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import tnpl.immersiveenchanting.fsm.TableState;

@Mixin(EnchantTableRenderState.class)
public class EnchantTableRenderStateMixin implements IImmersiveRenderState {

    @Unique
    private final ItemStackRenderState immersive$immersiveItemState = new ItemStackRenderState();

    @Unique
    private final ItemStackRenderState[] immersive$runeItemStates = new ItemStackRenderState[] {
            new ItemStackRenderState(),
            new ItemStackRenderState(),
            new ItemStackRenderState(),
            new ItemStackRenderState()
    };

    @Unique
    private final ItemStackRenderState[] immersive$orbItemStates = new ItemStackRenderState[] {
            new ItemStackRenderState(),
            new ItemStackRenderState(),
            new ItemStackRenderState(),
            new ItemStackRenderState()
    };

    @Unique
    private final ItemStackRenderState immersive$magicCircleState = new ItemStackRenderState();

    @Unique
    private final ItemStackRenderState immersive$beamState = new ItemStackRenderState();

    @Unique
    private final ItemStackRenderState immersive$pillarState = new ItemStackRenderState();

    @Unique
    private boolean immersive$immersiveActive = false;

    @Unique
    private float immersive$immersiveAngle = 0.0f;

    @Unique
    private float immersive$immersiveBobbing = 0.0f;

    @Unique
    private float immersive$renderTime = 0.0f;

    @Unique
    private boolean immersive$runesVisible;

    @Unique
    private int immersive$animationTick;

    @Unique
    private TableState immersive$tableState = TableState.IDLE;

    @Override
    public int getAnimationTick() { return this.immersive$animationTick; }
    @Override
    public void setAnimationTick(int tick) { this.immersive$animationTick = tick; }

    @Override
    public TableState getTableState() { return this.immersive$tableState; }
    @Override
    public void setTableState(TableState state) { this.immersive$tableState = state; }

    @Override
    public boolean areRunesVisible() { return this.immersive$runesVisible; }

    @Override
    public void setRunesVisible(boolean visible) { this.immersive$runesVisible = visible; }

    @Override
    public ItemStackRenderState getImmersiveItemState() { return this.immersive$immersiveItemState; }

    @Override
    public boolean isImmersiveActive() { return this.immersive$immersiveActive; }

    @Override
    public void setImmersiveActive(boolean active) { this.immersive$immersiveActive = active; }

    @Override
    public float getImmersiveAngle() { return this.immersive$immersiveAngle; }

    @Override
    public void setImmersiveAngle(float angle) { this.immersive$immersiveAngle = angle; }

    @Override
    public float getImmersiveBobbing() { return this.immersive$immersiveBobbing; }

    @Override
    public void setImmersiveBobbing(float bobbing) { this.immersive$immersiveBobbing = bobbing; }

    @Override
    public ItemStackRenderState getRuneItemState(int index) {
        if (index >= 0 && index < immersive$runeItemStates.length) {
            return this.immersive$runeItemStates[index];
        }
        return this.immersive$runeItemStates[0];
    }

    @Override
    public ItemStackRenderState getOrbItemState(int index) {
        if (index >= 0 && index < immersive$orbItemStates.length) {
            return this.immersive$orbItemStates[index];
        }
        return this.immersive$orbItemStates[0];
    }

    @Override
    public float getRenderTime() { return this.immersive$renderTime; }

    @Override
    public void setRenderTime(float time) { this.immersive$renderTime = time; }

    @Override
    public ItemStackRenderState getMagicCircleState() { return this.immersive$magicCircleState; }
    @Override
    public ItemStackRenderState getPillarState() { return this.immersive$pillarState; }
}