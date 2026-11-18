package xyz.dqrkis.module.modules.combat;

import xyz.dqrkis.event.events.PacketSendListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.utils.EncryptedString;
import xyz.dqrkis.utils.InventoryUtils;
import xyz.dqrkis.utils.SwitchUtils;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;

/**
 * Port of ClickCrystals' crystal-switch: punching obsidian or bedrock switches to an end crystal
 * and places it on the block.
 */
public final class CrystalSwitch extends Module implements PacketSendListener {
	private boolean busy;
	private final BooleanSetting onCrystal = new BooleanSetting(EncryptedString.of("On Crystal"), false)
			.setDescription(EncryptedString.of("Triggers when holding a crystal"));
	private final BooleanSetting onObsidian = new BooleanSetting(EncryptedString.of("On Obsidian"), true)
			.setDescription(EncryptedString.of("Triggers when holding obsidian"));
	private final BooleanSetting onSword = new BooleanSetting(EncryptedString.of("On Sword"), true)
			.setDescription(EncryptedString.of("Triggers when holding a sword"));
	private final BooleanSetting onTotem = new BooleanSetting(EncryptedString.of("On Totem"), true)
			.setDescription(EncryptedString.of("Triggers when holding a totem"));
	private final BooleanSetting onGlowstone = new BooleanSetting(EncryptedString.of("On Glowstone"), true)
			.setDescription(EncryptedString.of("Triggers when holding glowstone"));
	private final BooleanSetting onAnchor = new BooleanSetting(EncryptedString.of("On Anchor"), true)
			.setDescription(EncryptedString.of("Triggers when holding a respawn anchor"));
	private final BooleanSetting excludeBedrock = new BooleanSetting(EncryptedString.of("Exclude Bedrock"), false)
			.setDescription(EncryptedString.of("Prevents triggers when targeting bedrock"));

	public CrystalSwitch() {
		super(EncryptedString.of("Crystal Switch"),
				EncryptedString.of("Punch obsidian or bedrock to switch to a crystal and place it"),
				-1,
				Category.COMBAT);
		addSettings(onCrystal, onObsidian, onSword, onTotem, onGlowstone, onAnchor, excludeBedrock);
	}

	@Override
	public void onEnable() {
		eventManager.add(PacketSendListener.class, this);
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(PacketSendListener.class, this);
		super.onDisable();
	}

	@Override
	public void onPacketSend(PacketSendEvent event) {
		if (mc.player == null || mc.world == null || mc.interactionManager == null || busy)
			return;
		if (!(event.packet instanceof PlayerActionC2SPacket packet))
			return;
		if (packet.getAction() != PlayerActionC2SPacket.Action.START_DESTROY_BLOCK)
			return;
		if (!InventoryUtils.hasItemInHotbar(Items.END_CRYSTAL))
			return;

		BlockPos pos = packet.getPos();
		if (canUse() && isCrystallable(pos)) {
			busy = true;
			if (!SwitchUtils.selectHotbar(Items.END_CRYSTAL)) {
				busy = false;
				return;
			}
			event.cancel();
			try {
				SwitchUtils.interact(pos, packet.getDirection());
			} finally {
				busy = false;
			}
		}
	}

	private boolean canUse() {
		return (SwitchUtils.holdingSword() && onSword.getValue())
				|| (SwitchUtils.isHolding(Items.END_CRYSTAL) && onCrystal.getValue())
				|| (SwitchUtils.holdingTotem() && onTotem.getValue())
				|| (SwitchUtils.isHolding(Items.GLOWSTONE) && onGlowstone.getValue())
				|| (SwitchUtils.isHolding(Items.RESPAWN_ANCHOR) && onAnchor.getValue())
				|| (SwitchUtils.isHolding(Items.OBSIDIAN) && onObsidian.getValue());
	}

	private boolean isCrystallable(BlockPos pos) {
		return SwitchUtils.isBlock(pos, Blocks.OBSIDIAN)
				|| (!excludeBedrock.getValue() && SwitchUtils.isBlock(pos, Blocks.BEDROCK));
	}
}
