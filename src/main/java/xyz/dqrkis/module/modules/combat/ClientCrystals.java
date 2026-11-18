package xyz.dqrkis.module.modules.combat;

import xyz.dqrkis.event.events.PacketSendListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.utils.EncryptedString;
import xyz.dqrkis.utils.WorldUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

/**
 * Port of ClickCrystals' client-crystals: removes an end crystal client-side the moment you
 * punch it, as long as you are actually able to break it (weakness / strength / tool rules).
 */
public final class ClientCrystals extends Module implements PacketSendListener {
	public ClientCrystals() {
		super(EncryptedString.of("Client Crystals"),
				EncryptedString.of("Removes crystals client-side the moment you punch them"),
				-1,
				Category.COMBAT);
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

	private boolean canBreakCrystals() {
		if (mc.player == null)
			return false;
		StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);
		StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
		if (weakness == null)
			return true;
		if (strength != null && strength.getAmplifier() > weakness.getAmplifier())
			return true;
		return WorldUtils.isTool(mc.player.getMainHandStack());
	}

	@Override
	public void onPacketSend(PacketSendEvent event) {
		if (mc.player == null || mc.world == null || mc.isInSingleplayer())
			return;
		if (!(event.packet instanceof PlayerInteractEntityC2SPacket interactPacket))
			return;

		interactPacket.handle(new PlayerInteractEntityC2SPacket.Handler() {
			@Override
			public void interact(Hand hand) {}

			@Override
			public void interactAt(Hand hand, Vec3d pos) {}

			@Override
			public void attack() {
				if (!(mc.crosshairTarget instanceof EntityHitResult hit)
						|| mc.crosshairTarget.getType() != HitResult.Type.ENTITY)
					return;
				if (!(hit.getEntity() instanceof EndCrystalEntity crystal))
					return;
				if (!canBreakCrystals())
					return;

				crystal.discard();
				crystal.setRemoved(Entity.RemovalReason.KILLED);
				crystal.onRemoved();
			}
		});
	}
}
