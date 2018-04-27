/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.blockcapturing;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import org.apache.logging.log4j.Level;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.config.SpongeConfig;
import org.spongepowered.common.config.category.BlockCapturingCategory;
import org.spongepowered.common.config.category.BlockCapturingModCategory;
import org.spongepowered.common.config.type.GeneralConfigBase;
import org.spongepowered.common.interfaces.world.IMixinWorldServer;
import org.spongepowered.common.mixin.plugin.blockcapturing.IModData_BlockCapturing;

import java.util.Arrays;

@Mixin(Block.class)
public abstract class MixinBlock_BlockCapturing implements IModData_BlockCapturing, BlockType {

    private boolean processTickChangesImmediately;
    private boolean refreshCache = true;

    @Override
    public boolean processTickChangesImmediately() {
        return this.processTickChangesImmediately;
    }

    @Override
    public void initializeBlockCapturingState(World worldIn) {
        SpongeConfig<? extends GeneralConfigBase> activeConfig = ((IMixinWorldServer) worldIn).getActiveConfig();
        BlockCapturingCategory blockCapturing = activeConfig.getConfig().getBlockCapturing();
        String[] ids = this.getId().split(":");
        if (ids.length != 2) {
            final PrettyPrinter printer = new PrettyPrinter(60).add("Malformatted Block ID discovered!").centre().hr()
                .addWrapped(60, "Sponge has found a malformatted block id when trying to"
                                + " load configurations for the block id. The printed out block id"
                                + "is not originally from sponge, and should be brought up with the"
                                + "mod developer as the registration for this block is not likely"
                                + "to work with other systems and assumptions of having a properly"
                                + "formatted block id.")
                .add("%s : %s", "Malformed ID", this.getId())
                .add("%s : %s", "Discovered id array", ids)
                .add();
            final String id = ids[0];
            ids = new String[]{"unknown", id};
            printer
                .add("Sponge will attempt to work around this by using the provided generated id:")
                .add("%s : %s", "Generated ID", Arrays.toString(ids))
                .log(SpongeImpl.getLogger(), Level.WARN);

        }
        final String modId = ids[0];
        final String name = ids[1];

        BlockCapturingModCategory modCapturing = blockCapturing.getModMappings().get(modId);

        if (modCapturing == null) {
            modCapturing = new BlockCapturingModCategory();
            blockCapturing.getModMappings().put(modId, modCapturing);
        }
        if (!modCapturing.isEnabled()) {
            this.processTickChangesImmediately = false;
            modCapturing.getBlockMap().computeIfAbsent(name.toLowerCase(), k -> this.processTickChangesImmediately);
        } else {
            this.processTickChangesImmediately = modCapturing.getBlockMap().computeIfAbsent(name.toLowerCase(), k -> false);
        }

        if (blockCapturing.autoPopulateData()) {
            activeConfig.save();
        }
    }

    @Override
    public boolean requiresBlockCapturingRefresh() {
        return this.refreshCache;
    }

    @Override
    public void requiresBlockCapturingRefresh(boolean refresh) {
        this.refreshCache = refresh;
    }

}
