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

package de.florianmichael.viafabricplus.injection.mixin.fixes.vialegacy;

import com.viaversion.viaversion.api.protocol.AbstractProtocol;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.Protocol1_19_3To1_19_4;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ClientboundPackets1_19_4;
import de.florianmichael.viafabricplus.fixes.versioned.classic.CPEAdditions;
import de.florianmichael.viafabricplus.fixes.viaversion.SnowTrackerc0_30cpe;
import net.raphimc.vialegacy.protocol.classic.c0_28_30toa1_0_15.packet.ClientboundPacketsc0_28;
import net.raphimc.vialegacy.protocol.classic.c0_28_30toa1_0_15.packet.ServerboundPacketsc0_28;
import net.raphimc.vialegacy.protocol.classic.c0_30cpetoc0_28_30.Protocolc0_30cpeToc0_28_30;
import net.raphimc.vialegacy.protocol.classic.c0_30cpetoc0_28_30.packet.ClientboundPacketsc0_30cpe;
import net.raphimc.vialegacy.protocol.classic.c0_30cpetoc0_28_30.packet.ServerboundPacketsc0_30cpe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Protocolc0_30cpeToc0_28_30.class, remap = false)
public abstract class MixinProtocolc0_30cpeToc0_28_30 extends AbstractProtocol<ClientboundPacketsc0_30cpe, ClientboundPacketsc0_28, ServerboundPacketsc0_30cpe, ServerboundPacketsc0_28> {

    @Inject(method = "registerPackets", at = @At("RETURN"))
    private void extendPackets(CallbackInfo ci) {
        this.registerClientbound(CPEAdditions.EXT_WEATHER_TYPE, null, new PacketHandlers() {
            @Override
            public void register() {
                handler(wrapper -> {
                    wrapper.cancel();
                    final byte weatherType = wrapper.read(Types.BYTE);

                    final PacketWrapper changeRainState = PacketWrapper.create(ClientboundPackets1_19_4.GAME_EVENT, wrapper.user());
                    changeRainState.write(Types.UNSIGNED_BYTE, weatherType == 0 /* sunny */ ? (short) 1 : (short) 2); // start raining
                    changeRainState.write(Types.FLOAT, 0F); // unused
                    changeRainState.send(Protocol1_19_3To1_19_4.class);

                    if (weatherType == 1 /* raining */ || weatherType == 2 /* snowing */) {
                        final PacketWrapper changeRainType = PacketWrapper.create(ClientboundPackets1_19_4.GAME_EVENT, wrapper.user());
                        changeRainType.write(Types.UNSIGNED_BYTE, (short) 7); // set rain gradient
                        changeRainType.write(Types.FLOAT, 1F);
                        changeRainType.send(Protocol1_19_3To1_19_4.class);
                    }
                    if (weatherType == 2) {
                        wrapper.user().put(SnowTrackerc0_30cpe.INSTANCE);
                    } else {
                        wrapper.user().remove(SnowTrackerc0_30cpe.class);
                    }
                });
            }
        });
    }

}
