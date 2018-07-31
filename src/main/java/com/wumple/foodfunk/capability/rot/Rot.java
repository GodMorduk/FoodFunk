package com.wumple.foodfunk.capability.rot;

import java.util.List;

import com.wumple.foodfunk.Reference;
import com.wumple.foodfunk.capability.ContainerListenerRot;
import com.wumple.foodfunk.capability.preserving.IPreserving;
import com.wumple.foodfunk.capability.preserving.Preserving;
import com.wumple.foodfunk.configuration.ConfigContainer;
import com.wumple.foodfunk.configuration.ConfigHandler;
import com.wumple.util.adapter.IThing;
import com.wumple.util.capability.CapabilityContainerListenerManager;
import com.wumple.util.capability.eventtimed.EventTimedThingCap;
import com.wumple.util.capability.eventtimed.IEventTimedThingCap;
import com.wumple.util.container.ContainerUseTracker;
import com.wumple.util.misc.CraftingUtil;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

public class Rot extends EventTimedThingCap<IThing, RotInfo> implements IRot
{
    // The {@link Capability} instance
    @CapabilityInject(IRot.class)
    public static final Capability<IRot> CAPABILITY = null;
    public static final EnumFacing DEFAULT_FACING = null;

    // IDs of the capability
    public static final ResourceLocation ID = new ResourceLocation(Reference.MOD_ID, "rot");

    public static void register()
    {
        CapabilityManager.INSTANCE.register(IRot.class, new RotStorage(), () -> new Rot());

        CapabilityContainerListenerManager.registerListenerFactory(ContainerListenerRot::new);
    }
    
    @Override
    public void copyFrom(IEventTimedThingCap<IThing, RotInfo> other)
    {
        // WAS: info = other.getInfo();
        // Keep the oldest values of the two rots
        // For example: Melon 14 days -> Slices 7 days -> Melon 7 days
        long date = Math.min(getDate(), other.getDate());
        long time = Math.min(getTime(), other.getTime());
        info.set(date, time);        
    }

    public Rot()
    {
        super();
    }

    public Rot(Rot other)
    {
        super(other);
        info = other.info;
    }
    
    @Override
    public RotInfo newT()
    {
        return new RotInfo();
    }

    // ----------------------------------------------------------------------
    // Functionality

    @Override
    public IThing expired(World world, IThing thing)
    {
        RotProperty rotProps = ConfigHandler.rotting.getRotProperty(thing);
        // forget owner to eliminate dependency
        owner = null;
        return (rotProps != null) ? rotProps.forceRot(thing) : null;
    }

    @Override
    public boolean isEnabled()
    {
        return ConfigContainer.enabled;
    }
    
    @Override
    public boolean isDebugging()
    {
        return ConfigContainer.zdebugging.debug;
    }

    /*
     * Build tooltip info based on this rot
     */
    @Override
    public void doTooltip(ItemStack stack, EntityPlayer entity, boolean advanced, List<String> tips)
    {
        boolean usableStack = (stack != null) && (!stack.isEmpty());
        
        if (isEnabled() && (entity != null))
        {
            if (info != null)
            {
                World world = entity.getEntityWorld();
                        
                // if not initialized, set with reasonable guess to be overwritten by server update
                checkInitialized(world);
                
                // preserving container state aka fake temperature - ambient, chilled, cold, frozen
                if (info.isSet())
                {
                    if (usableStack && (entity.openContainer != null))
                    {
                        IPreserving cap = getPreservingContainer(entity, stack);
                        if (cap != null)
                        {
                            int ratio = cap.getRatio();
                            String key = getTemperatureTooltipKey(ratio);
                            tips.add(new TextComponentTranslation(key, ratio).getUnformattedText());
                        }
                    }
                }

                // Rot state
                boolean beingCrafted = (stack != null) ? CraftingUtil.isItemBeingCraftedBy(stack, entity) : false;
                String key = getStateTooltipKey(info, beingCrafted);

                if (key != null)
                {
                    tips.add(new TextComponentTranslation(key, info.getPercent() + "%", info.getDaysLeft(),
                            info.getDaysTotal()).getUnformattedText());
                }

                // advanced tooltip debug info
                if (advanced && isDebugging())
                {
                    tips.add(new TextComponentTranslation("misc.foodfunk.tooltip.advanced.datetime", info.getDate(),
                            info.getTime()).getUnformattedText());
                    tips.add(new TextComponentTranslation("misc.foodfunk.tooltip.advanced.expire", info.getCurTime(),
                            info.getExpirationTimestamp()).getUnformattedText());

                    int dimension = world.provider.getDimension();
                    int dimensionRatio = info.getDimensionRatio(world);
                    tips.add(new TextComponentTranslation("misc.foodfunk.tooltip.advanced.dimratio", dimensionRatio, dimension).getUnformattedText());
                }
            }
        }
    }
    
    // ----------------------------------------------------------------------
    // Internal

    @Override
    public IRot getCap(ICapabilityProvider thing)
    {
        return IRot.getRot(thing);
    }
    
    // only good on client side
    IPreserving getPreservingContainer(EntityPlayer entity, ItemStack stack)
    {
        return ContainerUseTracker.getContainerCapability(entity, stack, Preserving.CAPABILITY, Preserving.DEFAULT_FACING);
    }

    protected static String getTemperatureTooltipKey(final int ratio)
    {
        String key = null;

        if (ratio == 0)
        {
            key = "misc.foodfunk.tooltip.state.cold0";
        }
        else if ((ratio > 0) && (ratio <= 50))
        {
            key = "misc.foodfunk.tooltip.state.cold1";
        }
        else if ((ratio > 50) && (ratio < 100))
        {
            key = "misc.foodfunk.tooltip.state.cold2";
        }
        else if (ratio == 100)
        {
            key = "misc.foodfunk.tooltip.state.cold3";
        }
        else if (ratio > 100)
        {
            key = "misc.foodfunk.tooltip.state.cold4";
        }
        else if ((ratio < 0) && (ratio >= -50))
        {
            key = "misc.foodfunk.tooltip.state.warm1";
        }
        else if ((ratio < -50) && (ratio > -100))
        {
            key = "misc.foodfunk.tooltip.state.warm2";
        }
        else if (ratio <= -100)
        {
            key = "misc.foodfunk.tooltip.state.warm3";
        }

        return key;
    }

    @Override
    protected String getStateTooltipKey(RotInfo local, boolean beingCrafted)
    {
        String key = null;

        if (local.isNonExpiring())
        {
            key = "misc.foodfunk.tooltip.preserved";
        }
        else if (local.isSet() && !beingCrafted)
        {
            if (local.getPercent() >= 100)
            {
                key = "misc.foodfunk.tooltip.decaying";
            }
            else
            {
                key = "misc.foodfunk.tooltip.rot";
            }
        }        
        else if (local.time > 0)
        {
            key = "misc.foodfunk.tooltip.fresh";
        }

        return key;
    }

    // ----------------------------------------------------------------------
    // Possible future

    /*
     * public static boolean isInTheCold(ItemStack stack) { // TODO walk up container tree, if any is cold chest then true. If none, get world pos of topmost container. // TODO
     * Check isOnItemFrame // TODO if temperature mod, get temp from it // TODO ToughAsNails:TemperatureHelper.getTargetAtPosUnclamped() ?
     * https://github.com/Glitchfiend/ToughAsNails // TODO if no temperature mod, check biome for temp?
     * 
     * return false; }
     */
}
