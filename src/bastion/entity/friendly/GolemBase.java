package bastion.entity.friendly;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentThorns;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityMultiPart;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Icon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import net.minecraft.world.World;
import bastion.util.CoordITuple;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class GolemBase extends EntityCreature implements IInventory
{
    public WeakReference<EntityLivingBase> leader; //Monster
    public String ownerName = ""; //Player
    public int maxHealth = 20;
    public int baseAttack;
    public boolean paused;
    int useTime;
    protected static Random rand = new Random();
    //public static CoordTuple target;
    public int targetX;
    public int targetY;
    public int targetZ;
    public boolean targetLock;

    public ItemStack[] inventory;

    public GolemBase(World world)
    {
        super(world);
        setupInventory();
        //setCanPickUpLoot(true);
    }

    public void setupInventory ()
    {
        inventory = new ItemStack[0];
    }

    /*@Override
    public int getMaxHealth ()
    {
        //Workaround for dying on spawn
        if (maxHealth == 0)
            return 20;

        return maxHealth;
    }

    @Override
    public void initCreature ()
    {
        baseAttack = 3;
        paused = false;
    }*/

    public EntityLivingBase getLeader ()
    {
        if (leader == null || leader.get() == null)
            return this.worldObj.getPlayerEntityByName(ownerName);
        return leader.get();
    }

    public void setLeader (EntityLivingBase living)
    {
        if (living instanceof EntityPlayer)
            ownerName = ((EntityPlayer) living).username;
        else
            leader = new WeakReference(living);
    }

    public boolean isLeader (Entity entity)
    {
        if (entity == null)
        {
            return false;
        }
        if (entity instanceof EntityPlayer)
        {
            EntityPlayer entityplayer = (EntityPlayer) entity;
            return entityplayer.username.equalsIgnoreCase(ownerName);
        }
        return false;
    }

    public float getSpeed ()
    {
        return 0.25f;
    }

    /* AI */

    @Override
    protected boolean isAIEnabled ()
    {
        return true;
    }

    public void updateWanderPath ()
    {
        if (!paused)
            super.updateWanderPath();
    }

    public boolean standby ()
    {
        return false;
    }

    public boolean following ()
    {
        return false;
    }

    public boolean patrolling ()
    {
        return true;
    }
    
    public boolean hasTask ()
    {
        return false;
    }
    
    public void setHasTask(boolean flag)
    {
        
    }

    public void attackEntityAsGolem (Entity target)
    {
        ItemStack stack = getHeldItem();
        if (stack == null)
            target.attackEntityFrom(DamageSource.causeMobDamage(this), baseAttack);
    }

    public void teleport (double x, double y, double z)
    {
        this.setPosition(x, y, z);
        worldObj.playSoundAtEntity(this, "mob.endermen.portal", 0.5F, (rand.nextFloat() - rand.nextFloat()) * 0.2F + 1.0F);
    }

    /* Other */
    protected boolean canDespawn ()
    {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Icon getItemIcon (ItemStack par1ItemStack, int par2)
    {
        Icon icon = super.getItemIcon(par1ItemStack, par2);
        if (par1ItemStack.getItem().requiresMultipleRenderPasses())
        {
            return par1ItemStack.getItem().getIcon(par1ItemStack, par2);
        }

        return icon;
    }

    /* Inventory */

    @Override
    public void onLivingUpdate ()
    {
        super.onLivingUpdate();
        if (!this.worldObj.isRemote && !this.dead && this.worldObj.getGameRules().getGameRuleBooleanValue("mobGriefing"))
        {
            List list = this.worldObj.getEntitiesWithinAABB(EntityItem.class, this.boundingBox.expand(1.5D, 1.0D, 1.5D));
            Iterator iterator = list.iterator();

            while (iterator.hasNext())
            {
                EntityItem entityitem = (EntityItem) iterator.next();

                if (!entityitem.isDead && entityitem.getEntityItem() != null)
                {
                    ItemStack itemstack = entityitem.getEntityItem();
                    if (addItemStackToInventory(itemstack))
                    {
                        this.playSound("random.pop", 0.2F, ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
                        entityitem.setDead();
                    }
                }
            }
        }
    }

    @Override
    public ItemStack getStackInSlot (int slot)
    {
        if (slot < 0 || slot >= inventory.length + 5) //equipment
            return null;
        if (slot >= inventory.length)
            return getCurrentItemOrArmor(slot - inventory.length);
        return inventory[slot];
    }

    public boolean isStackInSlot (int slot)
    {
        if (slot < 0 || slot >= inventory.length + 5)
            return false;
        if (slot >= inventory.length)
            return getCurrentItemOrArmor(slot - inventory.length) != null;
        return inventory[slot] != null;
    }

    @Override
    public int getSizeInventory ()
    {
        return inventory.length;// + 5;
    }

    @Override
    public int getInventoryStackLimit ()
    {
        return 64;
    }

    @Override
    public void setInventorySlotContents (int slot, ItemStack itemstack)
    {
        if (slot >= inventory.length)
        {
            setCurrentItemOrArmor(slot - inventory.length, itemstack);
        }
        else
        {
            inventory[slot] = itemstack;
            if (itemstack != null && itemstack.stackSize > getInventoryStackLimit())
            {
                itemstack.stackSize = getInventoryStackLimit();
            }
        }
    }

    @Override
    public ItemStack decrStackSize (int slot, int quantity)
    {
        if (slot >= inventory.length)
        {
            ItemStack equip = getCurrentItemOrArmor(slot - inventory.length);
            if (equip != null)
            {
                if (equip.stackSize <= quantity)
                {
                    ItemStack stack = equip;
                    equip = null;
                    return stack;
                }
                ItemStack split = equip.splitStack(quantity);
                if (equip.stackSize == 0)
                {
                    equip = null;
                    setCurrentItemOrArmor(slot - inventory.length, null);
                }
                return split;
            }
        }
        else
        {
            if (inventory[slot] != null)
            {
                if (inventory[slot].stackSize <= quantity)
                {
                    ItemStack stack = inventory[slot];
                    inventory[slot] = null;
                    return stack;
                }
                ItemStack split = inventory[slot].splitStack(quantity);
                if (inventory[slot].stackSize == 0)
                {
                    inventory[slot] = null;
                }
                return split;
            }
        }
        
        return null;
    }

    /* Inventory Management */

    public boolean addItemStackToInventory (ItemStack par1ItemStack)
    {
        if (par1ItemStack == null)
        {
            return false;
        }
        else
        {
            try
            {
                int i;

                if (par1ItemStack.isItemDamaged())
                {
                    i = this.getFirstEmptyStack();

                    if (i >= 0)
                    {
                        this.inventory[i] = ItemStack.copyItemStack(par1ItemStack);
                        this.inventory[i].animationsToGo = 5;
                        par1ItemStack.stackSize = 0;
                        return true;
                    }
                    else
                    {
                        return false;
                    }
                }
                else
                {
                    do
                    {
                        i = par1ItemStack.stackSize;
                        par1ItemStack.stackSize = this.storePartialItemStack(par1ItemStack);
                    } while (par1ItemStack.stackSize > 0 && par1ItemStack.stackSize < i);

                    return par1ItemStack.stackSize < i;
                }
            }
            catch (Throwable throwable)
            {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Adding item to inventory");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Item being added");
                crashreportcategory.addCrashSection("Item ID", Integer.valueOf(par1ItemStack.itemID));
                crashreportcategory.addCrashSection("Item data", Integer.valueOf(par1ItemStack.getItemDamage()));
                throw new ReportedException(crashreport);
            }
        }
    }

    public int getFirstEmptyStack () //Equipped?
    {
        for (int i = 0; i < this.inventory.length; ++i)
        {
            if (this.inventory[i] == null)
            {
                return i;
            }
        }

        return -1;
    }

    private int storePartialItemStack (ItemStack par1ItemStack)
    {
        int i = par1ItemStack.itemID;
        int j = par1ItemStack.stackSize;
        int k;

        if (par1ItemStack.getMaxStackSize() == 1)
        {
            k = this.getFirstEmptyStack();

            if (k < 0)
            {
                return j;
            }
            else
            {
                if (this.inventory[k] == null)
                {
                    this.inventory[k] = ItemStack.copyItemStack(par1ItemStack);
                }

                return 0;
            }
        }
        else
        {
            k = this.storeItemStack(par1ItemStack);

            if (k < 0)
            {
                k = this.getFirstEmptyStack();
            }

            if (k < 0)
            {
                return j;
            }
            else
            {
                if (this.inventory[k] == null)
                {
                    this.inventory[k] = new ItemStack(i, 0, par1ItemStack.getItemDamage());

                    if (par1ItemStack.hasTagCompound())
                    {
                        this.inventory[k].setTagCompound((NBTTagCompound) par1ItemStack.getTagCompound().copy());
                    }
                }

                int l = j;

                if (j > this.inventory[k].getMaxStackSize() - this.inventory[k].stackSize)
                {
                    l = this.inventory[k].getMaxStackSize() - this.inventory[k].stackSize;
                }

                if (l > this.getInventoryStackLimit() - this.inventory[k].stackSize)
                {
                    l = this.getInventoryStackLimit() - this.inventory[k].stackSize;
                }

                if (l == 0)
                {
                    return j;
                }
                else
                {
                    j -= l;
                    this.inventory[k].stackSize += l;
                    this.inventory[k].animationsToGo = 5;
                    return j;
                }
            }
        }
    }

    private int storeItemStack (ItemStack par1ItemStack)
    {
        for (int i = 0; i < this.inventory.length; ++i)
        {
            if (this.inventory[i] != null && this.inventory[i].itemID == par1ItemStack.itemID && this.inventory[i].isStackable() && this.inventory[i].stackSize < this.inventory[i].getMaxStackSize()
                    && this.inventory[i].stackSize < this.getInventoryStackLimit() && (!this.inventory[i].getHasSubtypes() || this.inventory[i].getItemDamage() == par1ItemStack.getItemDamage())
                    && ItemStack.areItemStackTagsEqual(this.inventory[i], par1ItemStack))
            {
                return i;
            }
        }

        return -1;
    }

    /* Misc */

    @Override
    public String getInvName ()
    {
        return "golem.none";
    }

    @Override
    public boolean isInvNameLocalized ()
    {
        return false;
    }

    @Override
    public void onInventoryChanged ()
    {

    }

    public ItemStack getStackInSlotOnClosing (int slot)
    {
        return null;
    }

    public void openChest ()
    {
    }

    public void closeChest ()
    {
    }

    @Override
    public boolean isItemValidForSlot (int i, ItemStack itemstack)
    {
        return true;
    }

    @Override
    public boolean isUseableByPlayer (EntityPlayer entityplayer)
    {
        return true;
    }

    public void destroyCurrentEquippedItem ()
    {
        worldObj.playSoundAtEntity(this, "random.break", 0.5F, (rand.nextFloat() - rand.nextFloat()) * 0.2F + 1.0F);
        this.setCurrentItemOrArmor(0, null);
    }

    /* Saving */
    public void writeEntityToNBT (NBTTagCompound tags)
    {
        super.writeEntityToNBT(tags);
        tags.setString("OwnerName", ownerName);
        NBTTagList nbttaglist = new NBTTagList();
        for (int iter = 0; iter < inventory.length; iter++)
        {
            if (inventory[iter] != null)
            {
                NBTTagCompound tagList = new NBTTagCompound();
                tagList.setByte("Slot", (byte) iter);
                inventory[iter].writeToNBT(tagList);
                nbttaglist.appendTag(tagList);
            }
        }

        tags.setTag("Items", nbttaglist);

    }

    public void readEntityFromNBT (NBTTagCompound tags)
    {
        super.readEntityFromNBT(tags);
        ownerName = tags.getString("OwnerName");
        NBTTagList nbttaglist = tags.getTagList("Items");
        inventory = new ItemStack[getSizeInventory()];
        for (int iter = 0; iter < nbttaglist.tagCount(); iter++)
        {
            NBTTagCompound tagList = (NBTTagCompound) nbttaglist.tagAt(iter);
            byte slotID = tagList.getByte("Slot");
            if (slotID >= 0 && slotID < inventory.length)
            {
                inventory[slotID] = ItemStack.loadItemStackFromNBT(tagList);
            }
        }
    }

    public double getDistanceSqToEntity (CoordITuple target)
    {
        double d0 = this.posX - target.x;
        double d1 = this.posY - target.y;
        double d2 = this.posZ - target.z;
        return d0 * d0 + d1 * d1 + d2 * d2;
    }
}
