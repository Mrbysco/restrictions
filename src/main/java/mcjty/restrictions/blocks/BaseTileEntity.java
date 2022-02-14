package mcjty.restrictions.blocks;

import mcjty.lib.tileentity.TickingTileEntity;
import mcjty.restrictions.items.GlassBoots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class BaseTileEntity extends TickingTileEntity {

    private AABB aabb = null;
    private final double speed;

    public BaseTileEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, double speed) {
        super(type, pos, state);
        this.speed = speed;
    }

    @Override
    public void setPowerInput(int powered) {
        if (powerLevel != powered) {
            powerLevel = powered;
            setChanged();
            BlockState state = level.getBlockState(getBlockPos());
            level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        }
    }

    protected AABB getBox() {
        if (aabb == null) {
            assert level != null;
            Direction direction = level.getBlockState(getBlockPos()).getValue(BlockStateProperties.FACING);
            aabb = new AABB(getBlockPos().relative(direction));
            if (powerLevel > 1) {
                aabb = aabb.minmax(new AABB(getBlockPos().relative(direction, powerLevel)));
            }

        }
        return aabb;
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        int oldpower = powerLevel;
        if (packet.getTag() != null) {
            load(packet.getTag());
        }
        if (oldpower != powerLevel) {
            aabb = null;
        }
    }

    @Override
    public void saveClientDataToNBT(CompoundTag tagCompound) {
        saveAdditional(tagCompound);
    }

    @Override
    protected void tickServer() {
        Direction direction = level.getBlockState(getBlockPos()).getValue(BlockStateProperties.FACING);
        if (powerLevel > 0) {
            List<Entity> entities = level.getEntitiesOfClass(Entity.class, getBox());
            for (Entity entity : entities) {
                entity.push(direction.getStepX() * speed, direction.getStepY() * speed, direction.getStepZ() * speed);
                if (direction == Direction.UP && entity.getDeltaMovement().y > -0.5D) {
                    entity.fallDistance = 1.0F;
                }
            }
        }
    }

    @Override
    protected void tickClient() {
        Direction direction = level.getBlockState(getBlockPos()).getValue(BlockStateProperties.FACING);
        if (powerLevel > 0) {
            List<Player> entities = level.getEntitiesOfClass(Player.class, getBox());
            for (Player player : entities) {
                ItemStack boots = ((Player) player).getItemBySlot(EquipmentSlot.FEET);
                if (boots.isEmpty() || !(boots.getItem() instanceof GlassBoots)) {
                    player.push(direction.getStepX() * speed, direction.getStepY() * speed, direction.getStepZ() * speed);
                    if (direction == Direction.UP && player.getDeltaMovement().y > -0.5D) {
                        player.fallDistance = 1.0F;
                    }
                }
            }
        }
    }
}
