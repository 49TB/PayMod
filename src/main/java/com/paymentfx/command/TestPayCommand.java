package com.paymentfx.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.paymentfx.config.PaymentFxConfig;
import com.paymentfx.effect.EffectManager;
import com.paymentfx.effect.Tier;
import com.paymentfx.util.MoneyFormat;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;

/** `/testpay &lt;player&gt; &lt;amount&gt;` — previews any tier without a real payment. */
public final class TestPayCommand {
    private static final String[] AMOUNT_SUGGESTIONS = {
            "5M", "10M", "50M", "499M", "500M", "750M", "1B", "5B", "10B", "25B"
    };

    private TestPayCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("testpay")
                        .then(ClientCommandManager.argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (String name : context.getSource().getOnlinePlayerNames()) {
                                        builder.suggest(name);
                                    }
                                    return builder.buildFuture();
                                })
                                .then(ClientCommandManager.argument("amount", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (String amount : AMOUNT_SUGGESTIONS) {
                                                builder.suggest(amount);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            String player = StringArgumentType.getString(context, "player");
                                            String raw = StringArgumentType.getString(context, "amount");
                                            long value = MoneyFormat.parse(raw);
                                            if (value <= 0L) {
                                                context.getSource().sendFeedback(Component.translatable("paymentfx.test.invalid"));
                                                return 0;
                                            }
                                            PaymentFxConfig config = PaymentFxConfig.get();
                                            if (!config.enabled) {
                                                context.getSource().sendFeedback(Component.translatable("paymentfx.test.disabled"));
                                                return 0;
                                            }
                                            Tier tier = Tier.of(value, config);
                                            if (tier == null) {
                                                context.getSource().sendFeedback(Component.translatable("paymentfx.test.all_off"));
                                                return 0;
                                            }
                                            EffectManager.INSTANCE.trigger(player, value, MoneyFormat.dollars(value), true);
                                            context.getSource().sendFeedback(Component.translatable("paymentfx.test.sent",
                                                    player, MoneyFormat.dollars(value), tier.number + " - " + tier.label));
                                            return 1;
                                        })))));
    }
}
