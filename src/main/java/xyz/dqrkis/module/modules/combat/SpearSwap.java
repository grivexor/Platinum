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
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.enchantment.Enchantment;

import java.util.Locale;

public final class SpearSwap extends Module implements TickListener, AttackListener {
	private final BooleanSetting onlySword = new BooleanSetting(EncryptedString.of("Only Sword"), false)
			.setDescription(EncryptedString.of("Only triggers while holding a sword"));
	private final BooleanSetting onlyAxe = new BooleanSetting(EncryptedString.of("Only Axe"), false)
			.setDescription(EncryptedString.of("Only triggers while holding an axe"));
	private final BooleanSetting lungeOnly = new BooleanSetting(EncryptedString.of("Lunge Only"), true)
			.setDescription(EncryptedString.of("Prefers spears enchanted with Lunge"));
	private final BooleanSetting preferSharpness = new BooleanSetting(EncryptedString.of("Prefer Sharpness"), false)
			.setDescription(EncryptedString.of("Prefers the weapon with the highest Sharpness"));
	private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true)
			.setDescription(EncryptedString.of("Switches back to your previous slot"));
	private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 0, 10, 1, 1);

	private boolean attackHeldLastCheck;
	private int countdown;
	private int previousSlot = -1;

	public SpearSwap() {
		super(EncryptedString.of("Spear Swap"),
				EncryptedString.of("Swaps to a lunge weapon when attacking"),
				-1,
				Category.COMBAT);
		addSettings(onlySword, onlyAxe, lungeOnly, preferSharpness, switchBack, switchDelay);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		eventManager.add(AttackListener.class, this);
		reset();
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		eventManager.remove(AttackListener.class, this);
		reset();
		super.onDisable();
	}

	private void reset() {
		previousSlot = -1;
		countdown = 0;
		attackHeldLastCheck = false;
	}

	@Override
	public void onAttack(AttackEvent event) {
		if (mc.player == null)
			return;
		swapToBest();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.options == null)
			return;

		if (countdown > 0) {
			countdown--;
			if (countdown == 0) {
				int selected = mc.player.getInventory().getSelectedSlot();
				if (switchBack.getValue() && previousSlot >= 0 && previousSlot < 9 && selected != previousSlot)
					InventoryUtils.setInvSlot(previousSlot);
				previousSlot = -1;
			}
		}

		if (mc.options.attackKey.isPressed()) {
			boolean heldBefore = attackHeldLastCheck;
			attackHeldLastCheck = true;
			if (!heldBefore && mc.currentScreen == null)
				swapToBest();
		} else {
			attackHeldLastCheck = false;
		}
	}

	private void swapToBest() {
		if (countdown > 0)
			return;

		int best = findBestWeaponSlot();
		if (best < 0)
			return;

		int selected = mc.player.getInventory().getSelectedSlot();
		if (selected == best)
			return;

		previousSlot = selected;
		InventoryUtils.setInvSlot(best);
		countdown = Math.max(1, switchDelay.getValueInt());
	}

	private int findBestWeaponSlot() {
		boolean sword = onlySword.getValue();
		boolean axe = onlyAxe.getValue();
		boolean lunge = lungeOnly.getValue();
		boolean sharp = preferSharpness.getValue();

		int bestSlot = -1;
		int bestScore = Integer.MIN_VALUE;

		for (int i = 0; i < 9; i++) {
			ItemStack stack = mc.player.getInventory().getStack(i);
			if (stack == null || stack.isEmpty())
				continue;

			String id = Registries.ITEM.getId(stack.getItem()).getPath();
			String name = stack.getName().getString().toLowerCase(Locale.ROOT);

			boolean isSword = id.endsWith("_sword");
			boolean isAxe = WorldUtils.isAxe(stack);
			boolean isSpear = id.contains("spear") || name.contains("spear");
			boolean hasLunge = ItemUtils.hasEnchant(stack, "lunge");
			boolean lungeItem = lunge && hasLunge;

			if (sword && !isSword)
				continue;
			if (axe && !isAxe)
				continue;
			if (!sword && !axe && !isSword && !isAxe && !isSpear && !lungeItem)
				continue;

			int score = 0;
			if (lungeItem)
				score += 500;
			if (isSpear)
				score += 300;
			else if (isSword)
				score += 200;
			else if (isAxe)
				score += 100;

			if (sharp)
				score += sharpnessLevel(stack) * 60;

			if (score > bestScore) {
				bestScore = score;
				bestSlot = i;
			}
		}

		return bestSlot;
	}

	private static int sharpnessLevel(ItemStack stack) {
		ItemEnchantmentsComponent enchants = stack.getEnchantments();
		for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
			if (entry.getIdAsString().contains("sharpness"))
				return enchants.getLevel(entry);
		}
		return 0;
	}
}
