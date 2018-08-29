package net.machinemuse.powersuits.powermodule.movement;

import net.machinemuse.numina.api.module.EnumModuleCategory;
import net.machinemuse.numina.api.module.EnumModuleTarget;
import net.machinemuse.numina.api.module.IPlayerTickModule;
import net.machinemuse.numina.api.module.IToggleableModule;
import net.machinemuse.numina.utils.item.MuseItemUtils;
import net.machinemuse.powersuits.api.constants.MPSModuleConstants;
import net.machinemuse.powersuits.api.module.ModuleManager;
import net.machinemuse.powersuits.client.event.MuseIcon;
import net.machinemuse.powersuits.item.ItemComponent;
import net.machinemuse.powersuits.item.armor.ItemPowerArmorLeggings;
import net.machinemuse.powersuits.powermodule.PowerModuleBase;
import net.machinemuse.powersuits.utils.ElectricItemUtils;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.util.Objects;

/**
 * Ported by leon on 10/18/16.
 */
public class SprintAssistModule extends PowerModuleBase implements IToggleableModule, IPlayerTickModule {

    public static final UUID TAGUUID = new UUID(-7931854408382894632L, -8160638015224787553L);

    public SprintAssistModule(EnumModuleTarget moduleTarget) {
        super(moduleTarget);
        ModuleManager.INSTANCE.addInstallCost(getDataName(), MuseItemUtils.copyAndResize(ItemComponent.servoMotor, 4));

        addBasePropertyDouble(MPSModuleConstants.SPRINT_ENERGY_CONSUMPTION, 0, "RF");
        addTradeoffPropertyDouble("Power", MPSModuleConstants.SPRINT_ENERGY_CONSUMPTION, 100);
        addBasePropertyDouble(MPSModuleConstants.SPRINT_SPEED_MULTIPLIER, 1, "%");
        addTradeoffPropertyDouble("Power", MPSModuleConstants.SPRINT_SPEED_MULTIPLIER, 2);

        addBasePropertyDouble(MPSModuleConstants.SPRINT_ENERGY_CONSUMPTION, 0, "RF");
        addTradeoffPropertyDouble("Compensation", MPSModuleConstants.SPRINT_ENERGY_CONSUMPTION, 20);
        addBasePropertyDouble(MPSModuleConstants.SPRINT_FOOD_COMPENSATION, 0, "%");
        addTradeoffPropertyDouble("Compensation", MPSModuleConstants.SPRINT_FOOD_COMPENSATION, 1);

        addBasePropertyDouble(MPSModuleConstants.WALKING_ENERGY_CONSUMPTION, 0, "RF");
        addTradeoffPropertyDouble("Walking Assist", MPSModuleConstants.WALKING_ENERGY_CONSUMPTION, 100);
        addBasePropertyDouble(MPSModuleConstants.WALKING_SPEED_MULTIPLIER, 1, "%");
        addTradeoffPropertyDouble("Walking Assist", MPSModuleConstants.WALKING_SPEED_MULTIPLIER, 1);

        addBasePropertyDouble(MPSModuleConstants.SLOT_POINTS, 1);
        addIntTradeoffProperty(MPSModuleConstants.SPRINT_ENERGY_CONSUMPTION, MPSModuleConstants.SLOT_POINTS, 4, "pts", 1, 0);
    }

    @Override
    public void onPlayerTickActive(EntityPlayer player, ItemStack item) {
        if (item == player.getItemStackFromSlot(EntityEquipmentSlot.LEGS)) { // now you actually have to wear these to get the speed boost
            double horzMovement = player.distanceWalkedModified - player.prevDistanceWalkedModified;
            double totalEnergy = ElectricItemUtils.getMaxPlayerEnergy(player);
            if (horzMovement > 0) { // stop doing drain calculations when player hasn't moved
                if (player.isSprinting()) {
                    double exhaustion = Math.round(horzMovement * 100.0F) * 0.01;
                    double sprintCost = ModuleManager.INSTANCE.getOrSetModularPropertyDouble(item, MPSModuleConstants.SPRINT_ENERGY_CONSUMPTION);
                    if (sprintCost < totalEnergy) {
                        double sprintMultiplier = ModuleManager.INSTANCE.getOrSetModularPropertyDouble(item, MPSModuleConstants.SPRINT_SPEED_MULTIPLIER);
                        double exhaustionComp = ModuleManager.INSTANCE.getOrSetModularPropertyDouble(item, MPSModuleConstants.SPRINT_FOOD_COMPENSATION);
                        ElectricItemUtils.drainPlayerEnergy(player, (int) (sprintCost * horzMovement * 5));
                        setMovementModifier(item, sprintMultiplier * 1.2);
                        player.getFoodStats().addExhaustion((float) (-0.01 * exhaustion * exhaustionComp));
                        player.jumpMovementFactor = player.getAIMoveSpeed() * .2f;
                    }
                } else {
                    double cost = ModuleManager.INSTANCE.getOrSetModularPropertyDouble(item, MPSModuleConstants.WALKING_ENERGY_CONSUMPTION);
                    if (cost < totalEnergy) {
                        double walkMultiplier = ModuleManager.INSTANCE.getOrSetModularPropertyDouble(item, MPSModuleConstants.WALKING_SPEED_MULTIPLIER);
                        ElectricItemUtils.drainPlayerEnergy(player, (int) (cost * horzMovement * 5));
                        setMovementModifier(item, walkMultiplier);
                        player.jumpMovementFactor = player.getAIMoveSpeed() * .2f;
                    }
                }
            }
        } else
            onPlayerTickInactive(player, item);
    }

    @Override
    public void onPlayerTickInactive(EntityPlayer player, ItemStack itemStack) {
        if (!itemStack.isEmpty() && itemStack.getItem() instanceof ItemPowerArmorLeggings) {
            NBTTagList modifiers = itemStack.getTagCompound().getTagList("AttributeModifiers", Constants.NBT.TAG_COMPOUND);
            if (!modifiers.hasNoTags()) {
                for (int i = 0; i < modifiers.tagCount(); i++) {
                    NBTTagCompound tag = modifiers.getCompoundTagAt(i);
                    if (Objects.equals(new net.machinemuse.powersuits.powermodule.movement.AttributeModifier(tag).name, "Sprint Assist")) {
                        tag.setDouble("Amount", 0);
                    }
                }
            }
        }
    }

    public void setMovementModifier(ItemStack item, double multiplier) {
        NBTTagList modifiers = item.getTagCompound().getTagList("AttributeModifiers", Constants.NBT.TAG_COMPOUND);
        NBTTagCompound sprintModifiers = new NBTTagCompound();
        item.getTagCompound().setTag("AttributeModifiers", modifiers);
        for (int i = 0; i < modifiers.tagCount(); i++) {
            NBTTagCompound tag = modifiers.getCompoundTagAt(i);
            if (Objects.equals(new AttributeModifier(tag).name, "Sprint Assist")) {
                sprintModifiers = tag;
                sprintModifiers.setInteger("Operation", 1);
                sprintModifiers.setDouble("Amount", multiplier - 1);
                sprintModifiers.setString("Slot", EntityEquipmentSlot.LEGS.getName());
            }
        }

        //TODO: fix this so knockback does not have to be readded
        // this should be when first created
        if (sprintModifiers.hasNoTags()) {
            modifiers.appendTag(new AttributeModifier(1, TAGUUID, multiplier - 1, "generic.movementSpeed", "Sprint Assist", EntityEquipmentSlot.LEGS).toNBT());

            // add knockback resistance back because it doesn't show in tooltip after AttributeModifiers tag is added
            modifiers.appendTag(new AttributeModifier(0, TAGUUID, 0.25,
                    SharedMonsterAttributes.KNOCKBACK_RESISTANCE.getName(),
                    SharedMonsterAttributes.KNOCKBACK_RESISTANCE.getName(), EntityEquipmentSlot.LEGS).toNBT());
        }
    }

    @Override
    public EnumModuleCategory getCategory() {
        return EnumModuleCategory.CATEGORY_MOVEMENT;
    }

    @Override
    public String getDataName() {
        return MPSModuleConstants.MODULE_SPRINT_ASSIST__DATANAME;
    }

    @Override
    public TextureAtlasSprite getIcon(ItemStack item) {
        return MuseIcon.sprintAssist;
    }
}