package io.icker.factions.core;

import io.icker.factions.api.persistents.User;
import io.icker.factions.core.SoundManager;
import io.icker.factions.util.Message;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class InteractionsUtil {
    public static void sync(Player player, ItemStack itemStack, InteractionHand hand) {
        player.setItemInHand(hand, itemStack);
        itemStack.setCount(itemStack.getCount());
        if (itemStack.isDamageableItem()) {
            itemStack.setDamageValue(itemStack.getDamageValue());
        }

        if (!player.isUsingItem()) {
            player.inventoryMenu.sendAllDataToRemote();
        }
    }

    public static void warn(ServerPlayer player, String action) {
        SoundManager.warningSound(player);
        User user = User.get(player.getUUID());
        new Message(
            "Cannot %s here", 
            action
        ).fail()
            .send(player, !user.radar);
    }
}
