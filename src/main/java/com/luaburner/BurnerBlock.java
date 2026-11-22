package com.luaburner;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
/** The Blueprint Burner machine block - looks and behaves like a computer terminal. */
public class BurnerBlock extends BaseEntityBlock {
    public BurnerBlock() {
        super(Properties.of()
                .strength(3.0f)
                .noOcclusion());
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(BurnerBlock::new);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BurnerBlockEntity(pos, state);
    }

    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity>
            net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
                    Level level, BlockState state,
                    net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(
                type, ModRegistries.BURNER_BE.get(), BurnerBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BurnerBlockEntity burner && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                MenuProvider provider = burner.getMenuProvider();
                sp.openMenu(provider, buf -> buf.writeBlockPos(pos));
                if (sp.containerMenu instanceof BurnerMenu) {
                    NetworkHandler.sendList(sp, burner);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
}
