package com.thesift.block.entity;

import com.thesift.registry.ModEurophy;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Europhy Table's menu: four ingredient slots on the arms (north, east, south, west), the output in the
 * centre, the player's inventory below. Slot positions match the GUI texture drawn by tools/materials.py.
 * While the output is forming the table holds on to everything.
 */
public class EurophyTableMenu extends AbstractContainerMenu {
    public static final int[][] INPUT_XY = {{80, 20}, {108, 48}, {80, 76}, {52, 48}};
    public static final int OUTPUT_X = 80, OUTPUT_Y = 48, INVENTORY_Y = 104;
    private static final int TABLE_END = EurophyTableBlockEntity.SIZE;   // 5
    private static final int INV_END = TABLE_END + 27;                    // 32
    private static final int HOTBAR_END = INV_END + 9;                    // 41

    private final Container container;
    private final ContainerData data;

    public EurophyTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(EurophyTableBlockEntity.SIZE), new SimpleContainerData(EurophyTableBlockEntity.DATA));
    }

    public EurophyTableMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModEurophy.EUROPHY_MENU.get(), containerId);
        checkContainerSize(container, EurophyTableBlockEntity.SIZE);
        checkContainerDataCount(data, EurophyTableBlockEntity.DATA);
        this.container = container;
        this.data = data;
        for (int k = 0; k < EurophyTableBlockEntity.INPUTS; k++) {
            this.addSlot(new Slot(container, k, INPUT_XY[k][0], INPUT_XY[k][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !EurophyTableMenu.this.isForming();
                }

                @Override
                public boolean mayPickup(Player player) {
                    return !EurophyTableMenu.this.isForming();
                }
            });
        }
        this.addSlot(new Slot(container, EurophyTableBlockEntity.OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return !EurophyTableMenu.this.isForming();
            }
        });
        this.addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        this.addDataSlots(data);
    }

    public int charge() {
        return this.data.get(0);
    }

    public int need() {
        return this.data.get(1);
    }

    public boolean isForming() {
        return this.data.get(2) > 0;
    }

    public float formProgress() {
        int len = Math.max(1, this.data.get(3));
        return this.data.get(2) <= 0 ? 0.0F : 1.0F - this.data.get(2) / (float) len;
    }

    public int status() {
        return this.data.get(4);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (index < TABLE_END) {
                if (this.isForming() || !this.moveItemStackTo(stack, TABLE_END, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
                if (index == EurophyTableBlockEntity.OUTPUT) {
                    slot.onQuickCraft(stack, copy);
                }
            } else if (this.isForming() || !this.moveItemStackTo(stack, 0, EurophyTableBlockEntity.INPUTS, false)) {
                if (index < INV_END) {
                    if (!this.moveItemStackTo(stack, INV_END, HOTBAR_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(stack, TABLE_END, INV_END, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == copy.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return copy;
    }
}
