package xyz.dqrkis.mixin;

import xyz.dqrkis.event.EventManager;
import xyz.dqrkis.event.events.PacketReceiveListener;
import xyz.dqrkis.event.events.PacketSendListener;
import xyz.dqrkis.event.events.ShieldDisabledListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ClientConnection.class)
public class ClientConnectionMixin {

	@Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
	private static <T extends PacketListener> void onPacketReceive(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
		PacketReceiveListener.PacketReceiveEvent event = new PacketReceiveListener.PacketReceiveEvent(packet);

		EventManager.fire(event);
		if (event.isCancelled()) ci.cancel();

		// status 30 = shield break sound: an opponent's shield was just disabled
		if (packet instanceof EntityStatusS2CPacket status && status.getStatus() == 30) {
			MinecraftClient mc = MinecraftClient.getInstance();
			mc.execute(() -> {
				if (mc.world == null || mc.player == null)
					return;
				Entity entity = status.getEntity(mc.world);
				if (entity instanceof PlayerEntity && entity != mc.player && entity.squaredDistanceTo(mc.player) < 64)
					EventManager.fire(new ShieldDisabledListener.ShieldDisabledEvent());
			});
		}
	}

	@Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
	private void onPacketSend(Packet<?> packet, CallbackInfo ci) {
		PacketSendListener.PacketSendEvent event = new PacketSendListener.PacketSendEvent(packet);

		EventManager.fire(event);
		if (event.isCancelled()) ci.cancel();
	}
}
