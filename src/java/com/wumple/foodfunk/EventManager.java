package com.wumple.foodfunk;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EventManager
{
	@SubscribeEvent
	public void onEntityJoinWorld(EntityJoinWorldEvent event)
	{
		if(ConfigContainer.rotEnabled && !event.getWorld().isRemote)
		{
			if (event.getEntity() instanceof EntityItem)
			{
				EntityItem item = (EntityItem)event.getEntity();
				ItemStack rotStack = RotHandler.doRot(event.getWorld(), item.getItem());
				
				if(item.getItem() != rotStack)
				{
					item.setItem(rotStack);
				}
			} 
			else if (event.getEntity() instanceof EntityPlayer)
			{
				IInventory invo = ((EntityPlayer)event.getEntity()).inventory;
				RotHandler.rotInvo(event.getWorld(), invo);
			} 
			else if (event.getEntity() instanceof IInventory)
			{
				IInventory invo = (IInventory)event.getEntity();
				RotHandler.rotInvo(event.getWorld(), invo);
			}
		}
	}
	
	@SubscribeEvent
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		//ItemStack item = event.getEntityPlayer().getHeldItemMainhand();
		
		if(ConfigContainer.rotEnabled && event instanceof RightClickBlock && !event.getWorld().isRemote)
		{
			TileEntity tile = event.getEntityPlayer().world.getTileEntity(event.getPos());
			
			if(tile != null & tile instanceof IInventory)
			{
				RotHandler.rotInvo(event.getEntityPlayer().world, (IInventory)tile);
			}
		}
	}
		
	@SubscribeEvent
	public void onEntityInteract(EntityInteract event)
	{
		if(event.isCanceled() || event.getEntityPlayer().world.isRemote)
		{
			return;
		}
		
		if(!ConfigContainer.rotEnabled)
		{
			return;
		}
		
		if(event.getTarget() != null && event.getTarget() instanceof IInventory && ConfigContainer.rotEnabled)
		{
			IInventory chest = (IInventory)event.getTarget();
			
			RotHandler.rotInvo(event.getEntityPlayer().world, chest);
		}
	}
	
	@SubscribeEvent
	public void onLivingUpdate(LivingUpdateEvent event)
	{
		if(event.getEntityLiving().world.isRemote)
		{
			return;
		}
		
		if(event.getEntityLiving() instanceof EntityPlayer)
		{
			InventoryPlayer invo = (InventoryPlayer)((EntityPlayer)event.getEntityLiving()).inventory;
			
			if(ConfigContainer.rotEnabled)
			{
				RotHandler.rotInvo(event.getEntityLiving().world, invo);
			}
		}
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onItemTooltip(ItemTooltipEvent event)
	{
		ItemStack stack = event.getItemStack();
		Entity entity = event.getEntity();
		
		if(ConfigContainer.rotEnabled && (stack != null) && !stack.isEmpty() && (entity != null))
		{
			
			long time = entity.world.getTotalWorldTime();
			RotHandler.RotTimes rotTimes = RotHandler.getRotTimes(stack, time);
					
			if(rotTimes != null)
			{							
				event.getToolTip().add(
						new TextComponentTranslation(
								"misc.foodfunk.tooltip.rot", 
								rotTimes.getPercent() + "%", 
								rotTimes.getDays(),
								rotTimes.getTime()
							).getUnformattedText());
				//event.toolTip.add("Rotten: 0% (Day " + days + "/" + time + ")");
				//event.toolTip.add("Use-By: Day " + MathHelper.floor((rotTimes.date + rotTimes.time)/ConfigHandler.TICKS_PER_DAY));
			}
			
		}
	}
	
	@SubscribeEvent
	public void onCrafted(ItemCraftedEvent event) // Prevents exploit of making foods with almost rotten food to prolong total life of food supplies
	{
		if((!ConfigContainer.rotEnabled) || event.player.world.isRemote || event.crafting == null || event.crafting.isEmpty() || event.crafting.getItem() == null)
		{
			return;
		}
		
		RotHandler.handleCraftedRot(event.player.world, event.craftMatrix, event.crafting);
	}
}
