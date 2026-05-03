package tnpl.immersiveenchanting.client.math;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class RuneRaycaster {
    private RuneRaycaster() {}
    
    public static final float RUNE_HITBOX_SIZE = 0.3f;

    /**
     * Calculates the virtual AABB of a rune in absolute world coordinates.
     */
    public static AABB getRuneAABB(BlockPos tablePos, int runeIndex, int totalRunes, long gameTime, float partialTicks) {
        float time = gameTime + partialTicks;

        // Orbit mathematics: radius and uniform angular distribution
        double radius = 1.8;
        double angleDeg = (time * 2.0 + (360.0 / totalRunes) * runeIndex) % 360.0;
        double angleRad = Math.toRadians(angleDeg);

        // Table center. Add bobbing (sine wave) so the hitbox "breathes" in sync with the item animation
        double centerX = tablePos.getX() + 0.5;
        double centerY = tablePos.getY() + 1.2 + Math.sin(time * 0.1) * 0.1;
        double centerZ = tablePos.getZ() + 0.5;

        // Determine rune position using polar coordinates
        double runeX = centerX + Math.cos(angleRad) * radius;
        double runeZ = centerZ + Math.sin(angleRad) * radius;

        Vec3 runeCenter = new Vec3(runeX, centerY, runeZ);

        return new AABB(
                runeCenter.x - RUNE_HITBOX_SIZE, runeCenter.y - RUNE_HITBOX_SIZE, runeCenter.z - RUNE_HITBOX_SIZE,
                runeCenter.x + RUNE_HITBOX_SIZE, runeCenter.y + RUNE_HITBOX_SIZE, runeCenter.z + RUNE_HITBOX_SIZE
        );
    }

    /**
     * Casts a ray from the player's camera to detect rune intersections.
     * @return The index of the hit rune, or -1 if the ray missed.
     */
    public static int raycastActiveRunes(Player player, BlockPos tablePos, int totalRunes, float partialTicks) {
        // P0 (start) - player's eye position (camera)
        Vec3 start = player.getEyePosition(partialTicks);
        // Direction vector d
        Vec3 look = player.getViewVector(partialTicks);

        // Ray length (reach distance). Using standard 4.5 blocks.
        // For 1.20.5+, this can be fetched via: player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE)
        double reach = 4.5;
        Vec3 end = start.add(look.x * reach, look.y * reach, look.z * reach);

        double closestDistance = Double.MAX_VALUE;
        int hitRuneIndex = -1;

        // Iterate through all active runes
        for (int i = 0; i < totalRunes; i++) {
            AABB runeBox = getRuneAABB(tablePos, i, totalRunes, player.level().getGameTime(), partialTicks);

            // Native AABB intersection calculation
            Optional<Vec3> hitResult = runeBox.clip(start, end);

            if (hitResult.isPresent()) {
                // If the ray pierces multiple runes, prioritize the one closest to the camera
                double distance = start.distanceToSqr(hitResult.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    hitRuneIndex = i;
                }
            }
        }

        return hitRuneIndex;
    }
}