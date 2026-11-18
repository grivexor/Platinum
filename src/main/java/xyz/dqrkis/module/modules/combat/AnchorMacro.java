package xyz.dqrkis.module.modules.combat;

import xyz.dqrkis.event.events.ItemUseListener;
import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.KeybindSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.utils.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

//not mine
public final class AnchorMacro extends Module implements TickListener, ItemUseListener {
	private final BooleanSetting whileUse = new BooleanSetting(EncryptedString.of("While Use"), true).setDescription(EncryptedString.of("If it should trigger while eating/using shield"));
	private final BooleanSetting stopOnKill = new BooleanSetting(EncryptedString.of("Stop on Kill"), false).setDescription(EncryptedString.of("Doesn't anchor if body nearby"));
	private final BooleanSetting clickSimulation = new BooleanSetting(EncryptedString.of("Click Simulation"), false).setDescription(EncryptedString.of("Makes the CPS hud think you're legit"));
	private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 0, 20, 0, 1);
	private final NumberSetting switchChance = new NumberSetting(EncryptedString.of("Switch Chance"), 0, 100, 100, 1);
	private final NumberSetting placeChance = new NumberSetting(EncryptedString.of("Place Chance"), 0, 100, 100, 1).setDescription(EncryptedString.of("Randomization"));
	private final NumberSetting glowstoneDelay = new NumberSetting(EncryptedString.of("Glowstone Delay"), 0, 20, 0, 1);
	private final NumberSetting glowstoneChance = new NumberSetting(EncryptedString.of("Glowstone Chance"), 0, 100, 100, 1);
	private final NumberSetting explodeDelay = new NumberSetting(EncryptedString.of("Explode Delay"), 0, 20, 0, 1);
	private final NumberSetting explodeChance = new NumberSetting(EncryptedString.of("Explode Chance"), 0, 100, 100, 1);
	private final NumberSetting explodeSlot = new NumberSetting(EncryptedString.of("Explode Slot"), 1, 9, 1, 1);
	private final BooleanSetting onlyOwn = new BooleanSetting(EncryptedString.of("Only Own"), false);
	private final BooleanSetting onlyCharge = new BooleanSetting(EncryptedString.of("Only Charge"), false);

	private int switchClock = 0;
	private int glowstoneClock = 0;
	private int explodeClock = 0;
	/** keeps the macro armed for a few ticks after a right click, so a quick tap is not missed */
	private int useLatch = 0;
	/** ticks to wait after glowstone/explode so the server's block update can arrive */
	private int actionCooldown = 0;

	//hashset cuz in a hashset stuff cant repeat iirc
	private final Set<BlockPos> ownedAnchors = new HashSet<>();

	public AnchorMacro() {
		super(EncryptedString.of("Anchor Macro"),
				EncryptedString.of("Automatically blows up respawn anchors for you"),
				-1,
				Category.COMBAT);
		addSettings(whileUse, stopOnKill, clickSimulation, placeChance, switchDelay, switchChance, glowstoneDelay, glowstoneChance, explodeDelay, explodeChance, explodeSlot, onlyOwn, onlyCharge);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		eventManager.add(ItemUseListener.class, this);
		switchClock = 0;
		glowstoneClock = 0;
		explodeClock = 0;
		useLatch = 0;
		actionCooldown = 0;
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		eventManager.remove(ItemUseListener.class, this);
		super.onDisable();
	}

	private static boolean roll(int chance) {
		return chance >= 100 || (chance > 0 && MathUtils.randomInt(1, 100) <= chance);
	}

	private static boolean isUsing(net.minecraft.item.ItemStack stack) {
		return stack.getItem() instanceof ShieldItem || stack.contains(DataComponentTypes.FOOD);
	}

	@Override
	public void onTick() {
		if (mc.currentScreen != null || mc.player == null || mc.world == null)
			return;

		if (actionCooldown > 0)
			actionCooldown--;

		boolean rightHeld = KeyUtils.isKeyPressed(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
		boolean armed = rightHeld || useLatch > 0;
		if (useLatch > 0)
			useLatch--;

		// cheapest checks first: bail out before doing any world lookups
		if (!armed || actionCooldown > 0)
			return;
		if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
			return;

		BlockPos pos = hit.getBlockPos();
		BlockState state = mc.world.getBlockState(pos);
		if (!state.isOf(Blocks.RESPAWN_ANCHOR))
			return;

		if (whileUse.getValue() && rightHeld && (isUsing(mc.player.getMainHandStack()) || isUsing(mc.player.getOffHandStack())))
			return;
		if (onlyOwn.getValue() && !ownedAnchors.contains(pos))
			return;
		if (stopOnKill.getValue() && WorldUtils.isDeadBodyNearby())
			return;

		mc.options.useKey.setPressed(false);

		int charges = state.get(RespawnAnchorBlock.CHARGES);
		PlayerInventory inv = mc.player.getInventory();

		if (charges == 0) {
			if (!roll(placeChance.getValueInt()))
				return;

			if (!mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
				if (switchClock < switchDelay.getValueInt()) {
					switchClock++;
					return;
				}
				if (!roll(switchChance.getValueInt()) || !SwitchUtils.selectHotbar(Items.GLOWSTONE))
					return;
				switchClock = 0;
			}

			if (glowstoneClock < glowstoneDelay.getValueInt()) {
				glowstoneClock++;
				return;
			}
			if (!roll(glowstoneChance.getValueInt()))
				return;

			glowstoneClock = 0;
			click(hit);
		} else {
			int slot = explodeSlot.getValueInt() - 1;

			if (inv.getSelectedSlot() != slot) {
				if (switchClock < switchDelay.getValueInt()) {
					switchClock++;
					return;
				}
				if (!roll(switchChance.getValueInt()))
					return;
				switchClock = 0;
				InventoryUtils.setInvSlot(slot);
			}

			if (explodeClock < explodeDelay.getValueInt()) {
				explodeClock++;
				return;
			}
			if (!roll(explodeChance.getValueInt()) || onlyCharge.getValue())
				return;

			explodeClock = 0;
			click(hit);
			ownedAnchors.remove(pos);
		}
	}

	private void click(BlockHitResult hit) {
		if (clickSimulation.getValue())
			MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
		WorldUtils.placeBlock(hit, true);
		actionCooldown = 3;
	}

	@Override
	public void onItemUse(ItemUseEvent event) {
		useLatch = 6;
		if (mc.player == null || mc.world == null)
			return;
		if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
			return;

		BlockPos pos = hit.getBlockPos();

		if (mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) {
			ownedAnchors.add(mc.world.getBlockState(pos).isReplaceable() ? pos : pos.offset(hit.getSide()));
			// keep the set from growing forever
			if (ownedAnchors.size() > 64)
				ownedAnchors.clear();
		}

		if (BlockUtils.isAnchorCharged(pos))
			ownedAnchors.remove(pos);
	}
}