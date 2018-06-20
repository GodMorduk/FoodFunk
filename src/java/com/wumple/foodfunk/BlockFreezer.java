package com.wumple.foodfunk;

import javax.annotation.Nullable;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class BlockFreezer extends BlockChestBase implements ITileEntityProvider
{   
	public BlockFreezer()
	{
		super(Material.IRON);
		setHardness(3.0F);
		setCreativeTab(CreativeTabs.MISC);
		
		ObjectHandler.RegistrationHandler.nameHelper(this, "foodfunk:freezer");

	    setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
	}

    /**
     * Returns a new instance of a block's tile entity class. Called on placing the block.
     */
	@Override
    @Nullable
    public TileEntity createNewTileEntity(World worldIn, int meta)
    {
	    return new TileEntityFreezer();
	}
}