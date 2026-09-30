package com.thesift.block;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Carved Dreamstone murals. Glyphs 0-3 are the four Harmony tones (moon, wave, bell, star);
 * glyphs 4-7 are story carvings: the Warden, the Bulb, the Enchoer and the open portal.
 */
public class GlyphStoneBlock extends Block {
    public static final IntegerProperty GLYPH = IntegerProperty.create("glyph", 0, 7);

    public GlyphStoneBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(GLYPH, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GLYPH);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(GLYPH, 4 + context.getLevel().getRandom().nextInt(4));
    }
}
