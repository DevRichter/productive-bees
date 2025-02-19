package cy.jdkdigital.productivebees.common.entity.bee;

import cy.jdkdigital.productivebees.ProductiveBees;
import cy.jdkdigital.productivebees.client.particle.NectarParticleType;
import cy.jdkdigital.productivebees.common.block.entity.AdvancedBeehiveBlockEntity;
import cy.jdkdigital.productivebees.init.*;
import cy.jdkdigital.productivebees.setup.BeeReloadListener;
import cy.jdkdigital.productivebees.util.BeeAttributes;
import cy.jdkdigital.productivebees.util.BeeEffect;
import cy.jdkdigital.productivebees.util.BeeHelper;
import cy.jdkdigital.productivebees.util.ColorUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurableBee extends ProductiveBee implements IEffectBeeEntity
{
    private int attackCooldown = 0;
    public int breathCollectionCooldown = 600;
    private int teleportCooldown = 250;
    public PathfinderMob target = null;

    public static final EntityDataAccessor<String> TYPE = SynchedEntityData.defineId(ConfigurableBee.class, EntityDataSerializers.STRING);

    public ConfigurableBee(EntityType<? extends Bee> entityType, Level world) {
        super(entityType, world);

        beehiveInterests = (poiType) ->
                poiType == PoiType.BEEHIVE ||
                poiType == PoiType.BEE_NEST ||
                poiType == ModPointOfInterestTypes.NETHER_NEST.get() ||
                (poiType == ModPointOfInterestTypes.SOLITARY_HIVE.get() && isWild()) ||
                (poiType == ModPointOfInterestTypes.SOLITARY_NEST.get() && isWild()) ||
                (poiType == ModPointOfInterestTypes.DRACONIC_NEST.get() && isDraconic()) ||
                (poiType == ModPointOfInterestTypes.SUGARBAG_NEST.get() && getBeeType().equals("productivebees:sugarbag"));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType spawnReason, @Nullable SpawnGroupData livingEntityData, @Nullable CompoundTag tag) {
        String type = "";
        if (tag != null) {
            type = tag.contains("type") ? tag.getString("type") : tag.contains("EntityTag") ? tag.getCompound("EntityTag").getString("type") : "";

            if (type.equals("productivebees:ghostly") && ProductiveBees.rand.nextFloat() < 0.02f) {
                this.setCustomName(new TextComponent("BooBee"));
            } else if (type.equals("productivebees:blitz") && ProductiveBees.rand.nextFloat() < 0.02f) {
                this.setCustomName(new TextComponent("King BitzBee"));
            } else if (type.equals("productivebees:basalz") && ProductiveBees.rand.nextFloat() < 0.02f) {
                this.setCustomName(new TextComponent("Queen BazBee"));
            } else if (type.equals("productivebees:blizz") && ProductiveBees.rand.nextFloat() < 0.02f) {
                this.setCustomName(new TextComponent("Shiny BizBee"));
            } else if (type.equals("productivebees:redstone") && ProductiveBees.rand.nextFloat() < 0.01f) {
                this.setCustomName(new TextComponent("Redastone Bee"));
            } else if (type.equals("productivebees:destabilized_redstone") && ProductiveBees.rand.nextFloat() < 0.10f) {
                this.setCustomName(new TextComponent("Destabilized RedaStone Bee"));
            } else if (type.equals("productivebees:compressed_iron") && ProductiveBees.rand.nextFloat() < 0.05f) {
                this.setCustomName(new TextComponent("Depressed Iron Bee"));
            }
        }

        return super.finalizeSpawn(world, difficulty, spawnReason, livingEntityData, tag);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level.isClientSide) {
            --teleportCooldown;
            if (--attackCooldown < 0) {
                attackCooldown = 0;
            }
            if (attackCooldown == 0 && isAngry() && this.getTarget() != null && this.getTarget().distanceToSqr(this) < 4.0D) {
                attackCooldown = getEffectCooldown(getAttributeValue(BeeAttributes.TEMPER));
                attackTarget(this.getTarget());
            }

            // Draconic bees
            if (level.dimension() == Level.END && isDraconic() && --breathCollectionCooldown <= 0) {
                breathCollectionCooldown = 600;
                this.internalSetHasNectar(true);
            }

            // Redstone bees
            if (tickCount % 21 == 0 && hasNectar() && isRedstoned()) {
                for (int i = 1; i <= 2; ++i) {
                    BlockPos beePosDown = this.blockPosition().below(i);
                    if (level.isEmptyBlock(beePosDown)) {
                        BlockState redstoneState = ModBlocks.INVISIBLE_REDSTONE_BLOCK.get().defaultBlockState();
                        level.setBlockAndUpdate(beePosDown, redstoneState);
                        level.scheduleTick(beePosDown, redstoneState.getBlock(), 20);
                    }
                }
            }

            // Entity targeting bees
            if (target != null) {
                if (!hasNectar()) {
                    target.getNavigation().setSpeedModifier(0);
                } else {
                    target.setTarget(this);
                    target = null;
                }
            }

            // Kill unconfigured bees
            if (tickCount > 100 && getBeeType().isEmpty() && isAlive()) {
                this.kill();
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // self healing bees
        if (!this.level.isClientSide && this.isAlive()) {
            if (tickCount % 120 == 0 && this.canSelfHeal() && this.getHealth() < this.getMaxHealth()) {
                this.addEffect(new MobEffectInstance(MobEffects.HEAL, 1));
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        AttributeInstance attackDamage = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null && getDamage() != 2.0) {
            attackDamage.addTransientModifier(new AttributeModifier("Extra Damage", getDamage(), AttributeModifier.Operation.ADDITION));
        }
        return super.doHurtTarget(entity);
    }

    @Override
    public void spawnFluidParticle(Level worldIn, double xMin, double xMax, double zMin, double zMax, double posY, ParticleOptions particleData) {
        NectarParticleType particle;
        switch (getParticleType()) {
            case "pop":
                particle = ModParticles.COLORED_POPPING_NECTAR.get();
                break;
            case "lava":
                particle = ModParticles.COLORED_LAVA_NECTAR.get();
                break;
            case "portal":
                particle = ModParticles.COLORED_PORTAL_NECTAR.get();
                break;
            case "rising":
                particle = ModParticles.COLORED_RISING_NECTAR.get();
                break;
            case "drip":
            default:
                particle = ModParticles.COLORED_FALLING_NECTAR.get();
                break;
        }

        if (hasParticleColor()) {
            particle.setColor(getParticleColor());
        } else {
            particle.setColor(new float[]{0.92F, 0.782F, 0.72F});
        }

        worldIn.addParticle(particle, Mth.lerp(worldIn.random.nextDouble(), xMin, xMax), posY, Mth.lerp(worldIn.random.nextDouble(), zMin, zMax), 0.0D, 0.0D, 0.0D);
    }

    @Override
    protected void customServerAiStep() {
        // Teleport to active path
        if (this.teleportCooldown <= 0) {
            if (null != this.navigation.getPath() && isTeleporting()) {
                if (this.hasHive()) {
                    BlockEntity te = level.getBlockEntity(this.getHivePos());
                    if (te instanceof AdvancedBeehiveBlockEntity) {
                        int antiTeleportUpgrades = ((AdvancedBeehiveBlockEntity) te).getUpgradeCount(ModItems.UPGRADE_ANTI_TELEPORT.get());
                        if (antiTeleportUpgrades > 0) {
                            this.teleportCooldown = 10000;
                            super.customServerAiStep();
                            return;
                        }
                    }
                }
                BlockPos pos = this.navigation.getPath().getTarget();
                teleport(pos.getX(), pos.getY(), pos.getZ());
            }
            this.teleportCooldown = 250;
        }

        super.customServerAiStep();
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 motionMultiplierIn) {
        if (!isStringy() || state.getBlock() != Blocks.COBWEB) {
            super.makeStuckInBlock(state, motionMultiplierIn);
        }
    }

    @Override
    public boolean canFreeze() {
        return !isColdResistant() && super.canFreeze();
    }

    @Override
    public void attackTarget(LivingEntity target) {
        if (this.isAlive() && getNBTData().contains("attackResponse")) {
            String attackResponse = getNBTData().getString("attackResponse");
            switch (attackResponse) {
                case "fire":
                    target.setRemainingFireTicks(200);
                case "lava":
                    // Place flowing lava on the targets location
                    level.setBlock(target.blockPosition(), Blocks.LAVA.defaultBlockState(), 11);
            }
        }
    }

    public void setBeeType(String data) {
        this.entityData.set(TYPE, data);
    }

    public String getBeeType() {
        return this.entityData.get(TYPE);
    }

    @Override
    public float getSpeed() {
        return super.getSpeed() * this.getSpeedModifier();
    }

    @Override
    public void setHasStung(boolean hasStung) {
        if (!isStingless()) {
            super.setHasStung(hasStung);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> param) {
        if (TYPE.equals(param)) {
            refreshDimensions();
        }
        super.onSyncedDataUpdated(param);
        // /summon productivebees:configurable_bee ~ ~ ~ {"type":"productivebees:diamond", "NoAI":true, "HasNectar": true}
        // /kill @e[type=productivebees:configurable_bee, name="Diamond Bee"]
        // /data get entity @s SelectedItem
    }

    public void setAttributes() {
        CompoundTag nbt = getNBTData();
        if (nbt.contains(("productivity"))) {
            setAttributeValue(BeeAttributes.PRODUCTIVITY, nbt.getInt("productivity"));
        }
        if (nbt.contains(("temper"))) {
            setAttributeValue(BeeAttributes.TEMPER, nbt.getInt("temper"));
        }
        if (nbt.contains(("endurance"))) {
            setAttributeValue(BeeAttributes.ENDURANCE, nbt.getInt("endurance"));
        }
        if (nbt.contains(("behavior"))) {
            setAttributeValue(BeeAttributes.BEHAVIOR, nbt.getInt("behavior"));
        }
        if (nbt.contains(("weather_tolerance"))) {
            setAttributeValue(BeeAttributes.WEATHER_TOLERANCE, nbt.getInt("weather_tolerance"));
        }
    }

    @Override
    public int getColor(int tintIndex) {
        CompoundTag nbt = getNBTData();
        if (nbt.contains("primaryColor")) {
            return tintIndex == 0 ? nbt.getInt("primaryColor") : nbt.getInt("secondaryColor");
        }
        return super.getColor(tintIndex);
    }

    @Nonnull
    @Override
    protected Component getTypeName() {
        CompoundTag nbt = getNBTData();
        if (nbt != null) {
            return new TranslatableComponent("entity.productivebees." + getBeeName() + "_bee");
        }
        return super.getTypeName();
    }

    @Override
    public float getSizeModifier() {
        CompoundTag nbt = getNBTData();
        return nbt != null ? nbt.getFloat("size") : super.getSizeModifier();
    }

    public float getSpeedModifier() {
        CompoundTag nbt = getNBTData();
        return nbt != null ? nbt.getFloat("speed") : 1.0F;
    }

    public double getDamage() {
        CompoundTag nbt = getNBTData();
        return nbt != null ? nbt.getDouble("attack") : 2.0D;
    }

    @Override
    public boolean canSelfBreed() {
        CompoundTag nbt = getNBTData();
        return nbt.getBoolean("selfbreed");
    }

    @Override
    public boolean isFlowerValid(BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }

        if (this.getFlowerType().equals("entity_types")) {
            CompoundTag nbt = this.getNBTData();
            if (nbt != null) {
                if (nbt.contains("flowerTag")) {
                    TagKey<EntityType<?>> entityTag = ModTags.getEntityTag(new ResourceLocation(nbt.getString("flowerTag")));

                    List<Entity> entities = level.getEntities(this, (new AABB(pos).inflate(1.0D, 1.0D, 1.0D)), (entity -> entity.getType().is(entityTag)));
                    if (!entities.isEmpty()) {
                        target = (PathfinderMob) entities.get(0);

                        target.addEffect(new MobEffectInstance(MobEffects.LUCK, 400));

                        return true;
                    }
                }
            }
        }

        return super.isFlowerValid(pos);
    }

    @Override
    public boolean isFlowerBlock(BlockState flowerBlock) {
        if (flowerBlock.isAir()) {
            return false;
        }

        boolean canConvertBlock = BeeHelper.hasBlockConversionRecipe(this, flowerBlock);
        if (canConvertBlock) {
            return true;
        }
        CompoundTag nbt = getNBTData();
        if (nbt != null && this.getFlowerType().equals("blocks")) {
            if (nbt.contains("flowerTag")) {
                TagKey<Block> flowerTag = ModTags.getBlockTag(new ResourceLocation(nbt.getString("flowerTag")));
                return flowerBlock.is(flowerTag);
            } else if (nbt.contains("flowerBlock")) {
                return flowerBlock.getBlock().getRegistryName().toString().equals(nbt.getString("flowerBlock"));
            } else if (nbt.contains("flowerFluid") && !flowerBlock.getFluidState().isEmpty()) {
                if (nbt.getString("flowerFluid").contains("#")) {
                    TagKey<Fluid> flowerFluid = ModTags.getFluidTag(new ResourceLocation(nbt.getString("flowerFluid").replace("#", "")));
                    return flowerBlock.getFluidState().is(flowerFluid);
                } else {
                    return flowerBlock.getFluidState().getType().getRegistryName().toString().equals(nbt.getString("flowerFluid"));
                }
            }
        }
        return super.isFlowerBlock(flowerBlock);
    }

    @Override
    public Ingredient getBreedingIngredient() {
        String id = getNBTData().getString("breedingItem");
        if (id.isEmpty()) {
            return super.getBreedingIngredient();
        }

        if (id.startsWith("#")) {
            return Ingredient.of(ModTags.getItemTag(new ResourceLocation(id.substring(1))));
        } else {
            return Ingredient.of(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)));
        }
    }

    @Override
    public Integer getBreedingItemCount() {
        return getNBTData().getInt("breedingItemCount");
    }

    @Override
    public TagKey<Block> getNestingTag() {
        CompoundTag nbt = getNBTData();
        if (nbt != null && nbt.contains("nestingPreference")) {
            return ModTags.getBlockTag(new ResourceLocation(nbt.getString("nestingPreference")));
        }
        return super.getNestingTag();
    }


    @Override
    public BeeEffect getBeeEffect() {
        CompoundTag nbt = getNBTData();
        if (nbt.contains(("effect"))) {
            return new BeeEffect(nbt.getCompound("effect"));
        }
        return super.getBeeEffect();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TYPE, "");
    }

    @Override
    public void readAdditionalSaveData(@Nonnull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setBeeType(compound.getString("type"));
        breathCollectionCooldown = compound.getInt("breathCollectionCooldown");
    }

    @Override
    public void addAdditionalSaveData(@Nonnull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("type", getBeeType());
        compound.putInt("breathCollectionCooldown", breathCollectionCooldown);
    }

    public CompoundTag getNBTData() {
        CompoundTag nbt = BeeReloadListener.INSTANCE.getData(getBeeType());

        return nbt != null ? nbt : new CompoundTag();
    }

    public boolean hasBeeTexture() {
        return getNBTData().contains("beeTexture");
    }

    public String getBeeTexture() {
        return getNBTData().getString("beeTexture");
    }

    public String getRenderer() {
        return getNBTData().getString("renderer");
    }

    public String getRenderTransform() {
        return getNBTData().getString("renderTransform");
    }

    public boolean useGlowLayer() {
        return getNBTData().getBoolean("useGlowLayer") || (isRedstoned() && hasNectar());
    }

    private boolean isWild() {
        return getNBTData().contains("nestingPreference");
    }

    // Traits
    public boolean isFireproof() {
        return getNBTData().getBoolean("fireproof");
    }

    public boolean isWithered() {
        return getNBTData().getBoolean("withered");
    }

    public boolean isTranslucent() {
        return getNBTData().getBoolean("translucent");
    }

    public boolean isBlinding() {
        return getNBTData().getBoolean("blinding");
    }

    public boolean isDraconic() {
        return getNBTData().getBoolean("draconic");
    }

    public boolean isRedstoned() {
        return getNBTData().getBoolean("redstoned");
    }

    public boolean isSlimy() {
        return getNBTData().getBoolean("slimy");
    }

    public boolean isTeleporting() {
        return getNBTData().getBoolean("teleporting");
    }

    public boolean isStringy() {
        return getNBTData().getBoolean("stringy");
    }

    public boolean isStingless() {
        return getNBTData().getBoolean("stingless");
    }

    public boolean hasMunchies() {
        return getNBTData().getBoolean("munchies");
    }

    public boolean isWaterproof() {
        return getNBTData().getBoolean("waterproof");
    }

    public boolean isColdResistant() {
        return getNBTData().getBoolean("coldResistant");
    }

    public boolean isIrradiated() {
        return getNBTData().getBoolean("irradiated");
    }

    public String getParticleType() {
        return getNBTData().getString("particleType");
    }

    public boolean hasParticleColor() {
        return getNBTData().contains("particleColor");
    }

    public boolean canSelfHeal() {
        return getNBTData().getBoolean("selfheal");
    }

    public String getFlowerType() {
        return getNBTData().getString("flowerType");
    }

    public float[] getParticleColor() {
        return ColorUtil.getCacheColor(getNBTData().getInt("particleColor"));
    }

    public float[] getTertiaryColor() {
        return ColorUtil.getCacheColor(getNBTData().getInt("tertiaryColor"));
    }

    @Override
    public Map<MobEffect, Integer> getAggressiveEffects() {
        if (isWithered()) {
            return new HashMap<>()
            {{
                put(MobEffects.WITHER, 350);
            }};
        }
        if (hasMunchies()) {
            return new HashMap<>()
            {{
                put(MobEffects.HUNGER, 530);
            }};
        }
        if (isBlinding()) {
            return new HashMap<>()
            {{
                put(MobEffects.BLINDNESS, 450);
            }};
        }

        return null;
    }

    public List<String> getInvulnerabilities() {
        Tag inv = getNBTData().get("invulnerability");

        List<String> list = new ArrayList<>();
        if (inv instanceof ListTag listInv) {
            listInv.forEach(tag -> {
                list.add(tag.getAsString());
            });
        }

        return list;
    }

    @Override
    public boolean isInvulnerableTo(@Nonnull DamageSource source) {
        if (isWithered() && source.equals(DamageSource.WITHER)) {
            return true;
        }
        if (isDraconic() && source.equals(DamageSource.DRAGON_BREATH)) {
            return true;
        }
        if (isTranslucent() && source.equals(DamageSource.ANVIL)) {
            return true;
        }
        if (isWaterproof() && source.equals(DamageSource.DROWN)) {
            return true;
        }
        if (isColdResistant() && source.equals(DamageSource.FREEZE)) {
            return true;
        }
        if (isFireproof() && (source.equals(DamageSource.HOT_FLOOR) || source.equals(DamageSource.IN_FIRE) || source.equals(DamageSource.ON_FIRE) || source.equals(DamageSource.LAVA))) {
            return true;
        }
        return super.isInvulnerableTo(source) || getInvulnerabilities().contains(source.msgId);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (isWithered()) {
            return effect.getEffect() != MobEffects.WITHER && super.canBeAffected(effect);
        }
        return super.canBeAffected(effect);
    }

    private void teleport(double x, double y, double z) {
        BlockPos.MutableBlockPos blockpos$mutable = new BlockPos.MutableBlockPos(x, y, z);

        while (blockpos$mutable.getY() > 0 && !level.getBlockState(blockpos$mutable).getMaterial().blocksMotion()) {
            blockpos$mutable.move(Direction.DOWN);
        }

        BlockState blockstate = level.getBlockState(blockpos$mutable);
        if (blockstate.getMaterial().blocksMotion()) {
            EntityTeleportEvent.EnderEntity event = new EntityTeleportEvent.EnderEntity(this, x, y, z);
            if (!MinecraftForge.EVENT_BUS.post(event)) {
                boolean hasTeleported = this.randomTeleport(event.getTargetX(), event.getTargetY(), event.getTargetZ(), true);
                if (hasTeleported && !this.isSilent()) {
                    level.playSound(null, this.xo, this.yo, this.zo, SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 0.3F, 0.3F);
                    this.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.2F, 1.0F);
                }
            }
        }
    }
}
