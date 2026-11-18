package xyz.dqrkis.utils;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import static xyz.dqrkis.Dqrkis.mc;

/** Shared helpers for Obsidian Switch / Crystal Switch. */
public final class SwitchUtils {
	private SwitchUtils() {}

	public static boolean isHolding(Item item) {
		return mc.player != null && mc.player.getMainHandStack().isOf(item);
	}

	public static boolean holdingSword() {
		return mc.player != null && WorldUtils.isSword(mc.player.getMainHandStack());
	}

	public static boolean holdingTotem() {
		return isHolding(Items.TOTEM_OF_UNDYING);
	}

	/** Selects the first hotbar slot holding the item and syncs the slot to the server. */
	public static boolean selectHotbar(Item item) {
		if (mc.player == null || mc.interactionManager == null) return false;
		for (int i = 0; i < 9; i++) {
			if (mc.player.getInventory().getStack(i).isOf(item)) {
				InventoryUtils.setInvSlot(i);
				return true;
			}
		}
		return false;
	}

	public static boolean isBlock(BlockPos pos, net.minecraft.block.Block block) {
		return mc.world != null && mc.world.getBlockState(pos).isOf(block);
	}

	/** Right-clicks the given block face with whatever is in the main hand. */
	public static void interact(BlockPos pos, Direction side) {
		if (mc.player == null || mc.interactionManager == null) return;

		Vec3d hit = pos.toCenterPos();
		if (mc.crosshairTarget instanceof BlockHitResult bhr
				&& mc.crosshairTarget.getType() == HitResult.Type.BLOCK
				&& bhr.getBlockPos().equals(pos)) {
			hit = bhr.getPos();
		}

		mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(hit, side, pos, false));
	}
}
