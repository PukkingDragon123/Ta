package com.thesift.registry;

import com.mojang.serialization.Codec;
import com.thesift.TheSift;
import com.thesift.block.entity.LoreBookBlockEntity;
import com.thesift.entity.MiniCreator;
import com.thesift.knowledge.KnowledgeTracker;
import com.thesift.knowledge.Lore;
import com.thesift.music.Instrument;
import com.thesift.music.band.BandRegistry;
import com.thesift.music.band.BandVoice;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * F3 Knowledge &amp; lore: what the player has discovered (a saved, synced, death-proof data attachment), the
 * {@code thesift:lore} component naming the text of a Lore Book or Scroll, the placed Lore Book's block entity,
 * the Mini Creator and the sounds of reading and deciphering. The items and the placed books are generated into
 * {@link ModItems} / {@link ModBlocks} from tools/knowledge.py; the tracker that records discoveries and runs the
 * Mini Creator's quests is {@link KnowledgeTracker}.
 */
public final class ModKnowledge {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TheSift.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TheSift.MODID);
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(TheSift.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TheSift.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, TheSift.MODID);

    /** Everything this player has discovered, as keys ({@code entity:thesift:bulb}, {@code lore:creator_rifts}, ...). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<List<String>>> KNOWLEDGE = ATTACHMENTS.register("knowledge",
            () -> AttachmentType.<List<String>>builder(() -> List.of())
                    .serialize(Codec.STRING.listOf().fieldOf("unlocked"))
                    .sync(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()))
                    .copyOnDeath()
                    .build());

    /** Which text a Lore Book or Scroll holds ({@link Lore} id). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> LORE = COMPONENTS.registerComponentType("lore",
            b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredHolder<EntityType<?>, EntityType<MiniCreator>> MINI_CREATOR = ENTITIES.registerEntityType("mini_creator",
            MiniCreator::new, MobCategory.MISC, b -> b.sized(0.7F, 0.5F).eyeHeight(0.35F).clientTrackingRange(10));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LoreBookBlockEntity>> LORE_BOOK = BLOCK_ENTITIES.register("lore_book",
            () -> new BlockEntityType<>(LoreBookBlockEntity::new, ModBlocks.CREATOR_LORE_BOOK.get(), ModBlocks.PILLAGER_LORE_BOOK.get(),
                    ModBlocks.CULTIST_LORE_BOOK.get(), ModBlocks.OCEAN_LORE_BOOK.get(), ModBlocks.SOUL_LORE_BOOK.get()));

    public static final DeferredHolder<SoundEvent, SoundEvent> WHISPER = reg("knowledge.whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> DECIPHER = reg("knowledge.decipher");
    public static final DeferredHolder<SoundEvent, SoundEvent> UNLOCK = reg("knowledge.unlock");
    public static final DeferredHolder<SoundEvent, SoundEvent> READ = reg("knowledge.read");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUIDE_AMBIENT = reg("entity.mini_creator.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUIDE_TALK = reg("entity.mini_creator.talk");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUIDE_CELEBRATE = reg("entity.mini_creator.celebrate");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUIDE_STEP = reg("entity.mini_creator.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUIDE_APPEAR = reg("entity.mini_creator.appear");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUIDE_POOF = reg("entity.mini_creator.poof");

    private ModKnowledge() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(TheSift.id(name)));
    }

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
        COMPONENTS.register(bus);
        ENTITIES.register(bus);
        BLOCK_ENTITIES.register(bus);
        SOUNDS.register(bus);
        bus.addListener(ModKnowledge::attributes);
        bus.addListener(ModKnowledge::creativeTab);
        KnowledgeTracker.register();
        // the Mini Creator rings little bells along with your band - he only ever plays for the player he guides
        BandRegistry.voice(MINI_CREATOR, SoundEvents.NOTE_BLOCK_BELL).layer(SoundEvents.NOTE_BLOCK_CHIME, 0.4F).volume(0.8F)
                .families(Instrument.Family.values()).temper(BandVoice.Temper.LOYAL).instrument("creator_bells").colour(0xF2CC5A)
                .bond((m, player) -> m instanceof MiniCreator c && c.isGuiding(player) ? BandVoice.Bond.OWN : BandVoice.Bond.OTHER).register();
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(MINI_CREATOR.get(), MiniCreator.createAttributes().build());
    }

    /** Every Lore Book and Scroll, with its text, in the Sift's items tab. */
    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != ModCreativeTabs.ITEMS.getKey()) {
            return;
        }
        for (Lore lore : Lore.values()) {
            event.accept(lore.stack());
        }
    }

    /** The item a Lore piece is written in, by its origin and kind. */
    public static ItemStack loreStack(String origin, boolean scroll) {
        return new ItemStack(switch (origin + (scroll ? "_scroll" : "_book")) {
            case "creator_book" -> ModItems.CREATOR_LORE_BOOK.get();
            case "creator_scroll" -> ModItems.CREATOR_LORE_SCROLL.get();
            case "pillager_book" -> ModItems.PILLAGER_LORE_BOOK.get();
            case "pillager_scroll" -> ModItems.PILLAGER_LORE_SCROLL.get();
            case "cultist_book" -> ModItems.CULTIST_LORE_BOOK.get();
            case "cultist_scroll" -> ModItems.CULTIST_LORE_SCROLL.get();
            case "ocean_book" -> ModItems.OCEAN_LORE_BOOK.get();
            case "ocean_scroll" -> ModItems.OCEAN_LORE_SCROLL.get();
            case "soul_book" -> ModItems.SOUL_LORE_BOOK.get();
            default -> ModItems.SOUL_LORE_SCROLL.get();
        });
    }
}
