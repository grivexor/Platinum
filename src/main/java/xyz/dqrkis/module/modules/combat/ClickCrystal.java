package xyz.dqrkis.module.modules.combat;

import xyz.dqrkis.event.events.PacketSendListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.utils.EncryptedString;
import xyz.dqrkis.utils.SwitchUtils;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;

/**
 * Port of ClickCrystals' click-crystal: while holding an end crystal, left-clicking
 * (punching) obsidian or bedrock places the crystal instead of mining.
 */
public final class ClickCrystal extends Module implements PacketSendListener {
	private boolean busy;

	public ClickCrystal() {
		super(EncryptedString.of("Click Crystal"),
				EncryptedString.of("Binds end crystal placement to left click"),
				-1,
				Category.COMBAT);
	}

	@Override
	public void onEnable() {
		eventManager.add(PacketSendListener.class, this);
		busy = false;
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(PacketSendListener.class, this);
		super.onDisable();
	}

	@Override
	public void onPacketSend(PacketSendEvent event) {
		if (busy || mc.player == null || mc.world == null || mc.interactionManager == null)
			return;
		if (!(event.packet instanceof PlayerActionC2SPacket packet))
			return;
		if (packet.getAction() != PlayerActionC2SPacket.Action.START_DESTROY_BLOCK)
			return;

		BlockPos pos = packet.getPos();
		if (!SwitchUtils.isBlock(pos, Blocks.OBSIDIAN) && !SwitchUtils.isBlock(pos, Blocks.BEDROCK))
			return;
		if (!SwitchUtils.isHolding(Items.END_CRYSTAL))
			return;

		event.cancel();
		busy = true;
		try {
			SwitchUtils.interact(pos, packet.getDirection());
		} finally {
			busy = false;
		}
	}
}
