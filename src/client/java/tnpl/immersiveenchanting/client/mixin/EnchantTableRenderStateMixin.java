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
    private final ItemStackRenderState immersiveItemState = new ItemStackRenderState();

    @Unique
    private final ItemStackRenderState[] runeItemStates = new ItemStackRenderState[] {
            new ItemStackRenderState(),
            new ItemStackRenderState(),
            new ItemStackRenderState(),
            new ItemStackRenderState()
    };

    @Unique
    private final ItemStackRenderState[] orbItemStates = new ItemStackRenderState[] {
            new ItemStackRenderState(),
            new ItemStackRenderState(),
            new ItemStackRenderState(),
            new ItemStackRenderState()
    };

    @Unique
    private final ItemStackRenderState magicCircleState = new ItemStackRenderState();

    @Unique
    private final ItemStackRenderState beamState = new ItemStackRenderState();

    @Unique
    private final ItemStackRenderState pillarState = new ItemStackRenderState();

    @Unique
    private boolean immersiveActive = false;

    @Unique
    private float immersiveAngle = 0.0f;

    @Unique
    private float immersiveBobbing = 0.0f;

    @Unique
    private float renderTime = 0.0f;

    @Unique
    private boolean runesVisible;

    @Unique
    private int animationTick;

    @Unique
    private TableState tableState = TableState.IDLE;

    @Override
    public int getAnimationTick() { return this.animationTick; }
    @Override
    public void setAnimationTick(int tick) { this.animationTick = tick; }

    @Override
    public TableState getTableState() { return this.tableState; }
    @Override
    public void setTableState(TableState state) { this.tableState = state; }

    @Override
    public boolean areRunesVisible() { return this.runesVisible; }

    @Override
    public void setRunesVisible(boolean visible) { this.runesVisible = visible; }

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
    public ItemStackRenderState getRuneItemState(int index) {
        if (index >= 0 && index < runeItemStates.length) {
            return this.runeItemStates[index];
        }
        return this.runeItemStates[0];
    }

    @Override
    public ItemStackRenderState getOrbItemState(int index) {
        if (index >= 0 && index < orbItemStates.length) {
            return this.orbItemStates[index];
        }
        return this.orbItemStates[0];
    }

    @Override
    public float getRenderTime() { return this.renderTime; }

    @Override
    public void setRenderTime(float time) { this.renderTime = time; }

    @Override
    public ItemStackRenderState getMagicCircleState() { return this.magicCircleState; }
    @Override
    public ItemStackRenderState getPillarState() { return this.pillarState; }
}