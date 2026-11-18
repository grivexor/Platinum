package xyz.dqrkis.module.modules.combat;

import xyz.dqrkis.event.events.AttackListener;
import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.utils.EncryptedString;
import xyz.dqrkis.utils.InventoryUtils;
import xyz.dqrkis.utils.ItemUtils;
import xyz.dqrkis.utils.WorldUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

/**
 * Attack-triggered mace swap: when you attack while holding a sword/axe, the hit is
 * delivered with a mace from your hotbar, then you switch back.
 */
public final class MaceSwap extends Module implements TickListener, AttackListener {
	private final BooleanSetting density = new BooleanSetting(EncryptedString.of("Density"), true)
			.setDescription(EncryptedString.of("Prefers maces enchanted with Density"));
	private final BooleanSetting breach = new BooleanSetting(EncryptedString.of("Breach"), true)
			.setDescription(EncryptedString.of("Prefers maces enchanted with Breach"));
	private final BooleanSetting windBurst = new BooleanSetting(EncryptedString.of("Wind Burst"), true)
			.setDescription(EncryptedString.of("Prefers maces enchanted with Wind Burst"));
	private final BooleanSetting swords = new BooleanSetting(EncryptedString.of("Swords"), true)
			.setDescription(EncryptedString.of("Triggers when holding a sword"));
	private final BooleanSetting axes = new BooleanSetting(EncryptedString.of("Axes"), true)
			.setDescription(EncryptedString.of("Triggers when holding an axe"));
	private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true)
			.setDescription(EncryptedString.of("Switches back to your previous slot"));
	private final NumberSetting switchBackDelay = new NumberSetting(EncryptedString.of("Switch Back Delay"), 0, 20, 0, 1);
	private final BooleanSetting debug = new BooleanSetting(EncryptedString.of("Debug"), false)
			.setDescription(EncryptedString.of("Prints what Mace Swap is doing to chat"));

	private boolean swapped;
	private int previousSlot = -1;
	private int switchClock;

	public MaceSwap() {
		super(EncryptedString.of("Mace Swap"),
				EncryptedString.of("Swaps to a mace when you attack with a sword or axe"),
				-1,
				Category.COMBAT);
		addSettings(density, breach, windBurst, swords, axes, switchBack, switchBackDelay, debug);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		eventManager.add(AttackListener.class, this);
		resetState();
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		eventManager.remove(AttackListener.class, this);
		if (swapped && previousSlot != -1 && mc.player != null)
			InventoryUtils.setInvSlot(previousSlot);
		resetState();
		super.onDisable();
	}

	private void log(String msg) {
		if (debug.getValue() && mc.player != null)
			mc.player.sendMessage(Text.literal("[MaceSwap] " + msg), false);
	}

	@Override
	public void onTick() {
		if (mc.currentScreen != null || mc.player == null || !swapped)
			return;

		if (switchBack.getValue()) {
			if (switchClock < switchBackDelay.getValueInt()) {
				switchClock++;
				return;
			}
			if (previousSlot != -1)
				InventoryUtils.setInvSlot(previousSlot);
		}

		resetState();
	}

	@Override
	public void onAttack(AttackEvent event) {
		if (mc.player == null || swapped)
			return;

		if (!weaponMatches(mc.player.getMainHandStack()))
			return;

		int slot = findMaceSlot();
		if (slot == -1) {
			log("no mace found in hotbar");
			return;
		}

		previousSlot = mc.player.getInventory().getSelectedSlot();
		InventoryUtils.setInvSlot(slot);
		swapped = true;
		switchClock = 0;
		log("swapped to mace (slot " + (slot + 1) + ")");
	}

	private boolean weaponMatches(ItemStack stack) {
		if (swords.getValue() && axes.getValue())
			return WorldUtils.isSword(stack) || WorldUtils.isAxe(stack);
		return (!swords.getValue() || WorldUtils.isSword(stack)) && (!axes.getValue() || WorldUtils.isAxe(stack));
	}

	/** First mace carrying a ticked enchantment, otherwise any hotbar mace. */
	private int findMaceSlot() {
		boolean wantsEnchant = density.getValue() || breach.getValue() || windBurst.getValue();
		int any = -1;

		for (int i = 0; i < 9; i++) {
			ItemStack stack = mc.player.getInventory().getStack(i);
			if (!stack.isOf(Items.MACE))
				continue;
			if (any == -1)
				any = i;
			if (wantsEnchant && ((density.getValue() && ItemUtils.hasEnchant(stack, "density"))
					|| (breach.getValue() && ItemUtils.hasEnchant(stack, "breach"))
					|| (windBurst.getValue() && ItemUtils.hasEnchant(stack, "wind_burst"))))
				return i;
		}
		return any;
	}

	private void resetState() {
		previousSlot = -1;
		switchClock = 0;
		swapped = false;
	}
}
