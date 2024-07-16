/*
 * This file is part of ViaFabricPlus - https://github.com/FlorianMichael/ViaFabricPlus
 * Copyright (C) 2021-2024 FlorianMichael/EnZaXD <florian.michael07@gmail.com> and RK_01/RaphiMC
 * Copyright (C) 2023-2024 contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.florianmichael.viafabricplus.settings.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import de.florianmichael.viafabricplus.ViaFabricPlus;
import de.florianmichael.viafabricplus.injection.access.IConfirmScreen;
import de.florianmichael.viafabricplus.save.impl.AccountsSave;
import de.florianmichael.viafabricplus.screen.VFPScreen;
import de.florianmichael.viafabricplus.settings.base.BooleanSetting;
import de.florianmichael.viafabricplus.settings.base.ButtonSetting;
import de.florianmichael.viafabricplus.settings.base.SettingGroup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.bedrock.StepMCChain;
import net.raphimc.minecraftauth.step.bedrock.StepPlayFabToken;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCodeMsaCode;
import net.raphimc.minecraftauth.step.xbl.StepXblDeviceToken;
import net.raphimc.minecraftauth.step.xbl.StepXblSisuAuthentication;
import net.raphimc.minecraftauth.step.xbl.StepXblXstsToken;
import net.raphimc.minecraftauth.util.logging.ConsoleLogger;
import net.raphimc.minecraftauth.util.logging.ILogger;

import java.util.concurrent.CompletableFuture;

public class BedrockSettings extends SettingGroup {

    private static final Text TITLE = Text.literal("Microsoft Bedrock login");

    private static final BedrockSettings INSTANCE = new BedrockSettings();

    private final ButtonSetting clickToSetBedrockAccount = new ButtonSetting(this, Text.translatable("bedrock_settings.viafabricplus.click_to_set_bedrock_account"), () -> CompletableFuture.runAsync(this::openBedrockAccountLogin)) {
        
        @Override
        public MutableText displayValue() {
            final var account = ViaFabricPlus.global().getSaveManager().getAccountsSave().getBedrockAccount();
            if (account != null) {
                return Text.literal("Bedrock account: " + account.getMcChain().getDisplayName());
            } else {
                return super.displayValue();
            }
        }
    };
    public final BooleanSetting replaceDefaultPort = new BooleanSetting(this, Text.translatable("bedrock_settings.viafabricplus.replace_default_port"), true);

    private final ILogger GUI_LOGGER = new ConsoleLogger() {
        @Override
        public void info(AbstractStep<?, ?> step, String message) {
            super.info(step, message);
            if (step instanceof StepMsaDeviceCodeMsaCode) {
                return;
            }
            MinecraftClient.getInstance().execute(() -> {
                if (MinecraftClient.getInstance().currentScreen instanceof ConfirmScreen confirmScreen) {
                    ((IConfirmScreen) confirmScreen).viaFabricPlus$setMessage(Text.translatable(translationKey(step)));
                }
            });
        }
    };

    public BedrockSettings() {
        super(Text.translatable("setting_group_name.viafabricplus.bedrock"));
    }
    
    private void openBedrockAccountLogin() {
        final AccountsSave accountsSave = ViaFabricPlus.global().getSaveManager().getAccountsSave();

        final MinecraftClient client = MinecraftClient.getInstance();
        final Screen prevScreen = client.currentScreen;
        try {
            accountsSave.setBedrockAccount(MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN.getFromInput(GUI_LOGGER, MinecraftAuth.createHttpClient(), new StepMsaDeviceCode.MsaDeviceCodeCallback(msaDeviceCode -> {
                client.execute(() -> client.setScreen(new ConfirmScreen(copyUrl -> {
                    if (copyUrl) {
                        client.keyboard.setClipboard(msaDeviceCode.getDirectVerificationUri());
                    } else {
                        client.setScreen(prevScreen);
                        Thread.currentThread().interrupt();
                    }
                }, TITLE, Text.translatable("bedrock_settings.viafabricplus.click_to_set_bedrock_account.notice"), Text.translatable("base.viafabricplus.copy_link"), Text.translatable("base.viafabricplus.cancel"))));
                Util.getOperatingSystem().open(msaDeviceCode.getDirectVerificationUri());
            })));

            RenderSystem.recordRenderCall(() -> client.setScreen(prevScreen));
        } catch (Throwable e) {
            Thread.currentThread().interrupt();
            VFPScreen.showErrorScreen("Microsoft Bedrock Login", e, prevScreen);
        }
    }

    private String translationKey(final AbstractStep<?, ?> step) {
        return "minecraftauth_library.viafabricplus." + switch (step) {
            case StepXblDeviceToken stepXblDeviceToken -> "authenticate_xbox_live";
            case StepXblSisuAuthentication stepXblSisuAuthentication -> "authenticate_sisu";
            case StepMCChain stepMCChain -> "authenticate_minecraft";
            case StepXblXstsToken stepXblXstsToken -> "requesting_xsts_token";
            case StepPlayFabToken stepPlayFabToken -> "authenticate_playfab";
            case null, default -> throw new IllegalArgumentException("Unknown step: " + step);
        };
    }

    public static BedrockSettings global() {
        return INSTANCE;
    }

}
