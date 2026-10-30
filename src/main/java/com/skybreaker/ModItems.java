package com.skybreaker;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Function;

public final class ModItems {
	public static final StrikeItem ORBITAL_RAILGUN = register("orbital_railgun", p -> new StrikeItem(p, StrikeItem.Kind.RAILGUN));
	public static final StrikeItem SATELLITE_LASER = register("satellite_laser", p -> new StrikeItem(p, StrikeItem.Kind.LASER));
	public static final StrikeItem METEOR_BARRAGE = register("meteor_barrage", p -> new StrikeItem(p, StrikeItem.Kind.METEOR));
	public static final StrikeItem BLACK_HOLE = register("black_hole", p -> new StrikeItem(p, StrikeItem.Kind.BLACKHOLE));

	private static <T extends Item> T register(String name, Function<Item.Properties, T> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SkybreakerMod.id(name));
		Item.Properties props = new Item.Properties()
			.setId(key)
			.stacksTo(1)
			.rarity(Rarity.EPIC)
			.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props));
	}

	private static final ResourceKey<CreativeModeTab> COMBAT_TAB =
		ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("minecraft", "combat"));

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(COMBAT_TAB).register(output -> {
			output.accept(ORBITAL_RAILGUN);
			output.accept(SATELLITE_LASER);
			output.accept(METEOR_BARRAGE);
			output.accept(BLACK_HOLE);
		});
	}

	private ModItems() {
	}
}
