package gripe._90.arseng.cursedrelay;

import appeng.block.AEBaseBlock;
import appeng.block.AEBaseEntityBlock;
import gripe._90.arseng.block.entity.SourceAcceptorBlockEntity;
import net.minecraft.world.level.material.Material;

public class CursedRelayBlock extends AEBaseEntityBlock<CursedRelayTile> {
    public CursedRelayBlock() {
        super(AEBaseBlock.defaultProps(Material.METAL));
    }
}
