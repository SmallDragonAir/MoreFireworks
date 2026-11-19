package com.morefireworks.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.morefireworks.core.FireworkCombinatorics;
import com.morefireworks.core.RgbColors;
import com.morefireworks.core.RocketRandomizer;
import com.morefireworks.core.RocketSpec;
import com.morefireworks.core.SpecCodec;
import com.morefireworks.core.StarShape;
import com.morefireworks.core.StarSpec;
import com.morefireworks.naming.FireworkNamer;
import com.morefireworks.naming.NameMode;
import com.morefireworks.naming.NamingLanguage;
import com.morefireworks.registry.MFDataComponents;
import com.morefireworks.registry.MFItems;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;

/**
 * {@code /morefireworks ...} — the mod's whole user facing toolbox.
 *
 * <p>Everything is computed on demand. The combinatorics command never enumerates anything: it
 * evaluates closed form geometric series with {@link BigInteger}, so it answers instantly even
 * though the answer has hundreds of digits.</p>
 */
public final class MFCommands {

    private MFCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("morefireworks")
                .then(Commands.literal("count")
                        .executes(MFCommands::count))
                .then(Commands.literal("random")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> giveRandom(context, "random", new Random().nextLong()))
                        .then(Commands.literal("complex")
                                .executes(context -> giveRandom(context, "complex", new Random().nextLong())))
                        .then(Commands.literal("extreme")
                                .executes(context -> giveRandom(context, "extreme", new Random().nextLong())))
                        .then(Commands.argument("seed", LongArgumentType.longArg())
                                .executes(context -> giveRandom(context, "random",
                                        LongArgumentType.getLong(context, "seed")))))
                .then(Commands.literal("info")
                        .executes(MFCommands::info))
                .then(Commands.literal("name")
                        .executes(MFCommands::name))
                .then(Commands.literal("status")
                        .executes(MFCommands::status))
                .then(Commands.literal("recipecheck")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(MFCommands::recipeCheck))
                .then(Commands.literal("testlaunch")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> testLaunch(context, "complex", 20260101L))
                        .then(Commands.literal("complex")
                                .executes(context -> testLaunch(context, "complex", 20260101L)))
                        .then(Commands.literal("extreme")
                                .executes(context -> testLaunch(context, "extreme", 20260101L)))
                        .then(Commands.literal("random")
                                .executes(context -> testLaunch(context, "random", 20260101L))))
                .then(Commands.literal("star")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("colour", StringArgumentType.word())
                                .executes(context -> giveStar(context, StarShape.SMALL_BALL))
                                .then(Commands.argument("shape", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (StarShape shape : StarShape.values()) {
                                                builder.suggest(shape.id());
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> giveStar(context,
                                                StarShape.byIdOrNull(StringArgumentType.getString(context, "shape")))))))
        );
    }

    // ------------------------------------------------------------------
    // count
    // ------------------------------------------------------------------

    private static int count(CommandContext<CommandSourceStack> context) {
        FireworkCombinatorics.Report report = FireworkCombinatorics.report();
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.translatable("morefireworks.command.count.header")
                .withStyle(ChatFormatting.GOLD), false);
        line(source, "morefireworks.command.count.stars", report.starsPerType());
        line(source, "morefireworks.command.count.colorLists", report.orderedInitialLists());
        line(source, "morefireworks.command.count.fadeLists", report.orderedFadeLists());
        line(source, "morefireworks.command.count.rockets", report.starRockets());
        line(source, "morefireworks.command.count.plain", report.plainRockets());
        line(source, "morefireworks.command.count.total", report.totalItems());
        line(source, "morefireworks.command.count.visual", report.visualRockets());
        source.sendSuccess(() -> Component.translatable("morefireworks.command.count.digits",
                Component.literal(Integer.toString(report.getTotalDecimalDigits()))
                        .withStyle(ChatFormatting.AQUA)), false);
        source.sendSuccess(() -> Component.translatable("morefireworks.command.count.scientific",
                Component.literal(report.scientific()).withStyle(ChatFormatting.AQUA)), false);
        return 1;
    }

    private static void line(CommandSourceStack source, String key, BigInteger value) {
        source.sendSuccess(() -> Component.translatable(key,
                Component.literal(value.toString()).withStyle(ChatFormatting.AQUA)), false);
    }

    // ------------------------------------------------------------------
    // random
    // ------------------------------------------------------------------

    private static int giveRandom(CommandContext<CommandSourceStack> context, String mode, long seed)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        RocketSpec spec = switch (mode) {
            case "complex" -> RocketRandomizer.complex(seed);
            case "extreme" -> RocketRandomizer.extreme(seed);
            default -> RocketRandomizer.random(seed);
        };
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = com.morefireworks.registry.MFCreativeTabs.rocket(spec);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        context.getSource().sendSuccess(() -> Component.translatable("morefireworks.command.random.given",
                Component.literal(mode).withStyle(ChatFormatting.AQUA),
                Component.literal(Long.toString(seed)).withStyle(ChatFormatting.GRAY),
                stack.getHoverName().copy().withStyle(ChatFormatting.GOLD)), false);
        return 1;
    }

    // ------------------------------------------------------------------
    // star
    // ------------------------------------------------------------------

    private static int giveStar(CommandContext<CommandSourceStack> context, StarShape shape)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        String text = StringArgumentType.getString(context, "colour");
        int rgb;
        try {
            rgb = RgbColors.parse(text);
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.translatable("morefireworks.command.star.badColour", text));
            return 0;
        }
        if (shape == null) {
            context.getSource().sendFailure(Component.translatable("morefireworks.command.star.badShape"));
            return 0;
        }
        StarSpec star = new StarSpec(shape, List.of(rgb), List.of(), false, false);
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = com.morefireworks.registry.MFCreativeTabs.star(star);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        context.getSource().sendSuccess(() -> Component.translatable("morefireworks.command.star.given",
                stack.getHoverName().copy().withStyle(ChatFormatting.GOLD)), false);
        return 1;
    }

    // ------------------------------------------------------------------
    // info / name
    // ------------------------------------------------------------------

    private static int info(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        RocketSpec spec = stack.get(MFDataComponents.ROCKET_SPEC.get());
        if (spec == null) {
            StarSpec star = stack.get(MFDataComponents.STAR_SPEC.get());
            if (star == null) {
                context.getSource().sendFailure(Component.translatable("morefireworks.command.info.none"));
                return 0;
            }
            context.getSource().sendSuccess(() -> Component.literal(SpecCodec.encodeStar(star))
                    .withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        context.getSource().sendSuccess(() -> Component.literal(SpecCodec.encode(spec))
                .withStyle(ChatFormatting.GRAY), false);
        context.getSource().sendSuccess(() -> Component.translatable("morefireworks.command.info.stars",
                Component.literal(Integer.toString(spec.starCount())).withStyle(ChatFormatting.AQUA)), false);
        return 1;
    }

    private static int name(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        RocketSpec spec = player.getMainHandItem().get(MFDataComponents.ROCKET_SPEC.get());
        if (spec == null) {
            context.getSource().sendFailure(Component.translatable("morefireworks.command.info.none"));
            return 0;
        }
        for (NamingLanguage language : NamingLanguage.values()) {
            String shortName = FireworkNamer.name(spec, language, NameMode.SHORT);
            String fullName = FireworkNamer.name(spec, language, NameMode.FULL);
            context.getSource().sendSuccess(() -> Component.literal(shortName)
                    .withStyle(ChatFormatting.GOLD), false);
            context.getSource().sendSuccess(() -> Component.literal(fullName)
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    // ------------------------------------------------------------------
    // status / testlaunch
    // ------------------------------------------------------------------

    /**
     * Reports how many staged detonations are in flight. Console friendly, so the behaviour of
     * {@link com.morefireworks.logic.StagedDetonation} can be observed without a client attached.
     */
    private static int status(CommandContext<CommandSourceStack> context) {
        int active = com.morefireworks.logic.StagedDetonation.activeCount();
        context.getSource().sendSuccess(() -> Component.translatable("morefireworks.command.status",
                Component.literal(Integer.toString(active)).withStyle(ChatFormatting.AQUA)), false);
        return 1;
    }

    /**
     * Runs the real registered assembly recipe against a synthetic crafting grid, so the recipe can
     * be verified on a live server without a client attached.
     */
    private static int recipeCheck(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        net.minecraft.server.level.ServerLevel level = source.getLevel();
        java.util.List<ItemStack> grid = new java.util.ArrayList<>();
        grid.add(new ItemStack(net.minecraft.world.item.Items.FIREWORK_ROCKET));
        grid.add(com.morefireworks.registry.MFCreativeTabs.star(new StarSpec(StarShape.STAR, List.of(0xB02E26), List.of(), true, false)));
        grid.add(com.morefireworks.registry.MFCreativeTabs.star(new StarSpec(StarShape.LARGE_BALL, List.of(0x3C44AA), List.of(0xFED83D), false, true)));
        grid.add(new ItemStack(net.minecraft.world.item.Items.FIREWORK_STAR));
        net.minecraft.world.item.crafting.CraftingInput input =
                net.minecraft.world.item.crafting.CraftingInput.of(2, 2, grid);
        java.util.Optional<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> found =
                level.getServer().getRecipeManager()
                        .getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level);
        if (found.isEmpty()) {
            source.sendFailure(Component.translatable("morefireworks.command.recipecheck.miss"));
            return 0;
        }
        ItemStack result = found.get().value().assemble(input, level.registryAccess());
        RocketSpec spec = result.get(MFDataComponents.ROCKET_SPEC.get());
        if (spec == null) {
            source.sendFailure(Component.translatable("morefireworks.command.recipecheck.miss"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("morefireworks.command.recipecheck.hit",
                Component.literal(found.get().id().toString()).withStyle(ChatFormatting.GRAY),
                Component.literal(Integer.toString(spec.starCount())).withStyle(ChatFormatting.AQUA),
                Component.literal(SpecCodec.encode(spec)).withStyle(ChatFormatting.DARK_GRAY)), false);
        source.sendSuccess(() -> Component.literal(
                result.getHoverName().getString()).withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal(
                FireworkNamer.name(spec, NamingLanguage.EN_US, NameMode.FULL)).withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal(
                FireworkNamer.name(spec, NamingLanguage.ZH_CN, NameMode.SHORT)).withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    /**
     * Launches a generated rocket at world spawn without needing a player, so the entity and staged
     * detonation paths can be exercised from the server console.
     */    private static int testLaunch(CommandContext<CommandSourceStack> context, String mode, long seed) {
        CommandSourceStack source = context.getSource();
        net.minecraft.server.level.ServerLevel level = source.getLevel();
        RocketSpec spec = switch (mode) {
            case "extreme" -> RocketRandomizer.extreme(seed);
            case "random" -> RocketRandomizer.random(seed);
            default -> RocketRandomizer.complex(seed);
        };
        ItemStack stack = com.morefireworks.registry.MFCreativeTabs.rocket(spec);
        net.minecraft.world.phys.Vec3 spawn = source.getPosition().add(0.0, 1.0, 0.0);
        com.morefireworks.logic.StagedDetonation.launchAt(level, stack, spawn, net.minecraft.world.phys.Vec3.ZERO);
        source.sendSuccess(() -> Component.translatable("morefireworks.command.testlaunch",
                Component.literal(mode).withStyle(ChatFormatting.AQUA),
                Component.literal(Integer.toString(spec.starCount())).withStyle(ChatFormatting.AQUA),
                Component.literal(Integer.toString(com.morefireworks.logic.StagedDetonation.activeCount()))
                        .withStyle(ChatFormatting.AQUA),
                Component.literal(FireworkNamer.name(spec, NamingLanguage.EN_US, NameMode.SHORT))
                        .withStyle(ChatFormatting.GOLD)), false);
        return 1;
    }
}
