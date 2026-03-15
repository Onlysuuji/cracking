package org.example2.cracking;


import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "cracking", value = Dist.CLIENT)
public final class ClientEvents {
    private static ObservedEnchantState pendingObservation = null;
    private static int pendingTicks = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (!(mc.screen instanceof EnchantmentScreen)) return;
        if (!(mc.player.containerMenu instanceof EnchantmentMenu menu)) return;

        ItemStack item = menu.getSlot(0).getItem();
        if (item.isEmpty() || !item.isEnchantable()) return;

        ObservedEnchantState obs = new ObservedEnchantState(
                item,
                menu.getEnchantmentSeed(),
                menu.costs,
                menu.enchantClue,
                menu.levelClue
        );

        if (!obs.equals(pendingObservation)) {
            pendingObservation = obs;
            pendingTicks = 1;
            return;
        }

        pendingTicks++;
        if (pendingTicks < 2) {
            return;
        }

        if (obs.equals(CrackRuntime.latestObservation)) {
            return;
        }
        if (!obs.equals(CrackRuntime.latestObservation)) {
            CrackRuntime.latestObservation = obs;
            CrackRuntime.observations.add(obs);
            EnchantSeedCracker.submit(mc.level.registryAccess());
        }
        pendingObservation = null;
        pendingTicks = 0;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof EnchantmentScreen)) return;

        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;

        int x = 5;
        int y = 5;
        g.drawString(font, "Enchant crack", x, y, 0xFFFFFF, false); y += font.lineHeight + 2;
        g.drawString(font, "status: " + CrackRuntime.status, x, y, 0xFFFFFF, false); y += font.lineHeight + 2;
        g.drawString(font, "obs: " + CrackRuntime.observations.size(), x, y, 0xFFFFFF, false); y += font.lineHeight + 2;
        g.drawString(font, "candidates: " + CrackRuntime.candidateCount, x, y, 0xFFFFFF, false); y += font.lineHeight + 2;

        if (CrackRuntime.crackedSeed != null) {
            g.drawString(font,
                    String.format("seed: 0x%08X", CrackRuntime.crackedSeed),
                    x, y, 0x00FF00, false);
        }
    }
}