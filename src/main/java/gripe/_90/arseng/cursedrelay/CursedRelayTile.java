package gripe._90.arseng.cursedrelay;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.hooks.ticking.TickHandler;
import com.hollingsworth.arsnouveau.api.item.IWandable;
import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import com.hollingsworth.arsnouveau.common.block.Relay;
import com.hollingsworth.arsnouveau.common.entity.EntityFlyingItem;
import com.hollingsworth.arsnouveau.common.entity.EntityFollowProjectile;
import com.hollingsworth.arsnouveau.common.util.PortUtil;
import gripe._90.arseng.definition.ArsEngBlocks;
import gripe._90.arseng.me.key.SourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.checkerframework.checker.units.qual.C;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public class CursedRelayTile extends AEBaseBlockEntity implements IWandable {

    RelayNodeWrapper node1;
    RelayNodeWrapper node2;

    RelayStorageWrapper storageWrapper1;
    RelayStorageWrapper storageWrapper2;

    Logger logger = LoggerContext.getContext().getLogger(getClass());

    public CursedRelayTile(BlockPos p_155229_, BlockState p_155230_) {
        super(ArsEngBlocks.CURSED_RELAY_TILE, p_155229_, p_155230_);
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        if(node1 != null){
            node1.save(data, "node1");
        }
        if(node2 != null){
            node2.save(data, "node2");
        }
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        node1 = RelayNodeWrapper.load(data,"node1");
        storageWrapper1 = new RelayStorageWrapper(node1);
        node2 = RelayNodeWrapper.load(data,"node2");
        storageWrapper2 = new RelayStorageWrapper(node2);
    }

    @Override
    public void onFinishedConnectionLast(@Nullable BlockPos storedPos, @Nullable LivingEntity storedEntity, Player playerEntity) {
        IWandable.super.onFinishedConnectionLast(storedPos, storedEntity, playerEntity);
        onFinishedConnection(storedPos,playerEntity);
    }

    @Override
    public void onFinishedConnectionFirst(@Nullable BlockPos storedPos, @Nullable LivingEntity storedEntity, Player playerEntity) {
        IWandable.super.onFinishedConnectionFirst(storedPos, storedEntity, playerEntity);
        onFinishedConnection(storedPos,playerEntity);
    }

    @Override
    public void onReady() {
        super.onReady();
        TickHandler.instance().addCallable(level,this::refreshNodeConnections);
    }

    void refreshNodeConnections(){
        RefreshNodes();
        if(node1 == null || node2 == null){
            logger.warn("tried to refresh invalid nodes!");
            return;
        }
        CursedRelayNodePart part1 = node1.getNode(this);
        CursedRelayNodePart part2 = node2.getNode(this);
        storageWrapper1 = new RelayStorageWrapper(node1);
        storageWrapper2 = new RelayStorageWrapper(node2);
        part1.setRelayTile(this);
        part2.setRelayTile(this);
        part1.onConnectionFinalized();
        part2.onConnectionFinalized();
    }

    void onFinishedConnection(BlockPos pos, Player player){
        PortUtil.sendMessage(player,Component.literal("wanded cursed relay"));
        RefreshNodes();
        if(node1 != null && node2 != null){
            refreshNodeConnections();
            PortUtil.sendMessage(player,Component.literal("both nodes already set, refreshing connections"));
            return;
        }
        if(pos == null){
            PortUtil.sendMessage(player,Component.literal("block position is null!"));
            return;
        }
        RelayNodeWrapper node = null;
        CursedRelayNodePart part = null;
        for(Direction direction : Direction.values()){
            part = RelayNodeWrapper.getNode(pos, direction,this);
            node = RelayNodeWrapper.of(part);
            if(node != null){
                break;
            }
        }

        if(node != null){
            PortUtil.sendMessage(player, Component.literal("set node"));
            part.setRelayTile(this);
            if(node1 == null){
                node1 = node;
                storageWrapper1 = new RelayStorageWrapper(node1);
            }
            else{
                node2 = node;
                storageWrapper2 = new RelayStorageWrapper(node2);
                //make them refresh their storage
                refreshNodeConnections();
            }
        }
        else{
            PortUtil.sendMessage(player,Component.literal("selected tile contained no node! "));
        }
    }

    void RefreshNodes(){
        node1 = RefreshNode(node1);
        node2 = RefreshNode(node2);

        if(node1==null){
            storageWrapper1 = null;
        }
        if(node2==null){
            storageWrapper2 = null;
        }
    }

    RelayNodeWrapper RefreshNode(RelayNodeWrapper wrapper) {
        if (wrapper != null && wrapper.getNode(this) != null) {
            return wrapper;
        }

        if(wrapper != null) {
            LoggerContext.getContext().getLogger(getClass()).warn("node failed refresh at pos: " + wrapper.pos);
        }

        return null;
    }


    public CursedRelayNodePart getOtherCursedNode(CursedRelayNodePart node){
        if(node == null || node1 == null || node2 == null){
            return null;
        }
        if(node1.isNode(node)){
            return node2.getNode(this);
        }
        else if(node2.isNode(node)){
            return node1.getNode(this);
        }
        else{
            return null;
        }
    }

    public RelayStorageWrapper getOtherStorage(CursedRelayNodePart node){
        if(node == null){
            logger.warn("get other storage called from null node!");
            return null;
        }
        else if(node1 == null || node2 == null){
            logger.warn("get other storage called while nodes not initialized!");
            return null;
        }
        else if(node1.isNode(node)){
            logger.info("returning wrapper 2");
            return new RelayStorageWrapper(node2);
        }
        else if(node2.isNode(node)){
            logger.info("returning wrapper 1");
            return new RelayStorageWrapper(node1);
        }
        else{
            logger.warn("get other storage called from node outside of network!");
            logger.info("calling node: "+node.getBlockEntity().getBlockPos());
            return null;
        }
    }

    static class RelayNodeWrapper{
        BlockPos pos;
        Direction direction;

        public static RelayNodeWrapper of(BlockPos pos, Direction dir){
            if(pos == null || dir == null){
                return null;
            }
            return new RelayNodeWrapper(pos, dir);
        }

        public static RelayNodeWrapper of(CursedRelayNodePart node){
            if(node == null){
                return null;
            }
            else{
                return new RelayNodeWrapper(node);
            }
        }

        public <T> T getFromStorageOrDefault(CursedRelayTile tile, Function<MEStorage,T> getter, T defaultVal){
            MEStorage storage = getStorage(tile);
            if(storage == null){
                tile.logger.warn("missing storage! sending default value: "+defaultVal);
                return defaultVal;
            }
            else{
                return getter.apply(storage);
            }
        }

        public MEStorage getStorage(CursedRelayTile tile){
            CursedRelayNodePart part = getNode(tile);
            if(part == null){
                return null;
            }
            return part.getConnectedGridStorage();
        }

        public boolean isNode(CursedRelayNodePart node){
            return node.getBlockEntity().getBlockPos() == pos /* && node.getSide() == direction*/;
        }

        private RelayNodeWrapper(BlockPos pos, Direction dir){
            this.pos = pos;
            this.direction = dir;
        }

        private RelayNodeWrapper(CursedRelayNodePart node){
            if(node == null){
                pos = null;
                direction = null;
            }
            pos = node.getBlockEntity().getBlockPos();
            direction = node.getSide();
            if(direction == null){
                throw new NullPointerException("direction on node was null!");
            }
        }

        void save(CompoundTag tag, String name){
            if(pos == null){
                return;
            }
            CompoundTag myTag = new CompoundTag();
            myTag.putInt("x",pos.getX());
            myTag.putInt("y",pos.getY());
            myTag.putInt("z",pos.getZ());
            myTag.putString("dir",direction.name());
            tag.put(name,myTag);
        }

        public static RelayNodeWrapper load(CompoundTag tag, String name){
            BlockPos pos = null;
            Direction dir = null;
            if(tag.contains(name)) {
                CompoundTag myTag = tag.getCompound(name);
                int x = myTag.getInt("x");
                int y = myTag.getInt("y");
                int z = myTag.getInt("z");
                pos = new BlockPos(x, y, z);
                dir = Direction.valueOf(myTag.getString("dir"));
            }
            return of(pos,dir);
        }

        public CursedRelayNodePart getNode(CursedRelayTile relay){
            return getNode(pos,direction,relay);
        }

        private static CursedRelayNodePart getNode(BlockPos pos, Direction direction, CursedRelayTile relay){
            if(pos == null){
                return null;
            }
            BlockEntity tile = relay.level.getBlockEntity(pos);
            if(!(tile instanceof IPartHost partHost)){
                return null;
            }
            IPart part = partHost.getPart(direction);
            if(part instanceof CursedRelayNodePart node){
                return node;
            }
            else{
                return null;
            }
        }
    }

    void SpawnParticles(BlockPos startPos, BlockPos endPos, AEKey key){
        if(startPos == null){
            logger.error("start pos was null!");
            return;
        }
        if(endPos == null){
            logger.error("end pos was null!");
            return;
        }
        if(key instanceof AEItemKey itemKey) {
            EntityFlyingItem flyingItem = new EntityFlyingItem(level, startPos, endPos);
            flyingItem.setStack(itemKey.toStack());
            level.addFreshEntity(flyingItem);
        }
        else if(key instanceof AEFluidKey fluidKey){
            EntityFollowProjectile aoeProjectile = new EntityFollowProjectile(level, startPos, endPos);
            aoeProjectile.setColor(new ParticleColor(20, 50, 255));
            level.addFreshEntity(aoeProjectile);
        }
        else if(key instanceof SourceKey sourceKey){
            EntityFollowProjectile aoeProjectile = new EntityFollowProjectile(level, startPos, endPos);
            level.addFreshEntity(aoeProjectile);
        }
        else{
            EntityFollowProjectile aoeProjectile = new EntityFollowProjectile(level, startPos, endPos);
            aoeProjectile.setColor(new ParticleColor(90, 255, 255));
            level.addFreshEntity(aoeProjectile);
        }
    }

    BlockPos getNodePos(RelayStorageWrapper wrapper){
        RelayNodeWrapper node = wrapper.wrapper;
        return node.pos;
    }

    BlockPos getOtherNodePos(RelayStorageWrapper wrapper){
        RelayNodeWrapper node = wrapper.wrapper;
        if(node1.pos == node.pos){
            return node2.pos;
        }
        else if(node2.pos == node1.pos){
            return node1.pos;
        }
        else{
            return null;
        }
    }

    boolean checkedOut = false;

    class RelayStorageWrapper implements MEStorage {

        RelayNodeWrapper wrapper;


        public RelayStorageWrapper(RelayNodeWrapper node){
            this.wrapper = node;
        }

        @Override
        public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
            return wrapper.<Boolean>getFromStorageOrDefault(CursedRelayTile.this,
                    (storage) -> storage.isPreferredStorageFor(what,source),
                    false);
        }

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            try {
                //so that we don't get circular references with each storage checking the other for items
                if (checkedOut) {
                    return 0;
                }
                checkedOut = true;
                return wrapper.<Long>getFromStorageOrDefault(CursedRelayTile.this,
                        (storage) -> {
                            if (mode == Actionable.MODULATE) {
                                SpawnParticles(getBlockPos(), getNodePos(this), what);
                                SpawnParticles(getOtherNodePos(this), getBlockPos(),  what);
                            }
                            logger.info("insert storage function");
                            return storage.insert(what, amount, mode, source);
                        },
                        0L);
            }
            catch (Exception e){
                e.printStackTrace();
                logger.info("error during insertion");
                throw new RuntimeException(e);
            } finally {
                checkedOut = false;
            }
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
            try {
                //so that we don't get circular references with each storage checking the other for items
                if(checkedOut){
                    return 0;
                }
                checkedOut = true;
                return wrapper.<Long>getFromStorageOrDefault(CursedRelayTile.this,
                        (storage) -> {
                            if (mode == Actionable.MODULATE) {
                                SpawnParticles(getNodePos(this), getBlockPos(), what);
                                SpawnParticles(getBlockPos(), getOtherNodePos(this), what);
                            }
                            logger.info("extract storage function");
                            return storage.extract(what, amount, mode, source);
                        },
                        0L);
            }
            catch (Exception e){
                e.printStackTrace();
                logger.info("error during extraction lol");
                throw new RuntimeException(e);
            }
            finally {
                checkedOut = false;
            }
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            wrapper.getFromStorageOrDefault(CursedRelayTile.this,
                    (storage) -> {storage.getAvailableStacks(out); return false;},
                    false);
        }

        @Override
        public Component getDescription() {
            return Component.literal("cursed relay tile storage at pos: " + getBlockPos());
        }

        @Override
        public KeyCounter getAvailableStacks() {
            return wrapper.<KeyCounter>getFromStorageOrDefault(CursedRelayTile.this,
                    (storage) -> storage.getAvailableStacks(),
                    new KeyCounter());
        }
    }
}
