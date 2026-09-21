package com.flywire.minecraft.entity;

import com.flywire.minecraft.brain.BrainManager;
import com.flywire.minecraft.brain.MotorOutput;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/** A real living mob whose server-side movement is decoded from the FlyWire runtime. */
public final class FlyEntity extends PathAwareEntity {
    private MotorOutput motor = MotorOutput.SILENT;
    private boolean flying = false;
    private float wingPhase;
    private int feedingTicks;

    public FlyEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 1;
        this.setNoGravity(false);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 2.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    public static boolean canSpawn(EntityType<FlyEntity> type, ServerWorldAccess world, net.minecraft.entity.SpawnReason reason, BlockPos pos, net.minecraft.util.math.random.Random random) {
        BlockState below = world.getBlockState(pos.down());
        return below.isOpaque() && world.getLightLevel(pos) > 4;
    }

    @Override
    protected void initGoals() {
        // Intentionally empty. Vanilla pathfinding is not a substitute for the connectome.
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient()) {
            wingPhase += flying ? 0.75f : 0.12f;
            return;
        }
        Map<String, Float> sensory = senseWorld();
        motor = BrainManager.get().tick(this, sensory);
        applyMotor(motor);
        wingPhase += 0.45f + motor.wingBeat() * 1.5f;
        if (feedingTicks > 0) feedingTicks--;
    }

    private Map<String, Float> senseWorld() {
        Map<String, Float> signals = new HashMap<>();
        BlockPos here = getBlockPos();
        float brightness = getWorld().getLightLevel(here) / 15f;
        int solid = 0;
        int flowers = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockState state = getWorld().getBlockState(here.add(dx, dy, dz));
                    if (state.isOpaque()) solid++;
                    if (state.isIn(BlockTags.FLOWERS) || state.isOf(Blocks.HONEY_BLOCK)) flowers++;
                }
            }
        }
        Vec3d forward = Vec3d.fromPolar(0, getYaw());
        BlockState ahead = getWorld().getBlockState(here.add(Math.round((float) forward.x), 0, Math.round((float) forward.z)));
        float obstacle = ahead.isOpaque() ? 1f : solid / 80f;
        signals.put("visual", Math.min(1f, brightness * 0.7f + obstacle * 0.3f));
        signals.put("looming", loomingThreat());
        signals.put("olfactory", Math.min(1f, flowers / 10f));
        signals.put("gustatory", isFood(ahead) || flowers > 0 && distanceToFood(1.5) ? 1f : 0f);
        signals.put("mechanosensory", Math.min(1f, solid / 24f));
        signals.put("thermosensory", nearbyHotBlock() ? 1f : 0f);
        signals.put("gravity", 0.25f);
        return signals;
    }

    private float loomingThreat() {
        float signal = 0f;
        for (PlayerEntity player : getWorld().getPlayers()) {
            double distance = distanceTo(player);
            if (distance > 10) continue;
            double speed = player.getVelocity().length();
            signal = Math.max(signal, (float) ((10 - distance) / 10.0) * 0.65f + (float) Math.min(0.35, speed * 3));
        }
        for (var other : getWorld().getOtherEntities(this, getBoundingBox().expand(5), entity -> entity instanceof LivingEntity)) {
            signal = Math.max(signal, 0.15f);
        }
        return Math.min(1f, signal);
    }

    private boolean distanceToFood(double radius) {
        BlockPos center = getBlockPos();
        int r = (int) Math.ceil(radius);
        for (BlockPos pos : BlockPos.iterate(center.add(-r, -r, -r), center.add(r, r, r))) {
            if (isFood(getWorld().getBlockState(pos)) && Math.sqrt(pos.getSquaredDistance(getPos())) <= radius + 0.75) return true;
        }
        return false;
    }

    private boolean nearbyHotBlock() {
        BlockPos center = getBlockPos();
        for (BlockPos pos : BlockPos.iterate(center.add(-1, -1, -1), center.add(1, 1, 1))) {
            BlockState state = getWorld().getBlockState(pos);
            if (state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE) || state.isOf(Blocks.LAVA)) return true;
        }
        return false;
    }

    private static boolean isFood(BlockState state) {
        return state.isIn(BlockTags.FLOWERS) || state.isOf(Blocks.HONEY_BLOCK) || state.isOf(Blocks.SUGAR_CANE)
                || state.isOf(Blocks.SWEET_BERRY_BUSH);
    }

    private void applyMotor(MotorOutput output) {
        float turn = output.turn() * 8f;
        setYaw(getYaw() + turn);
        boolean shouldFly = flying;
        if (output.takeoff() > 0.15f || output.escape() > 0.1f) shouldFly = true;
        if (output.landing() > 0.4f && onGround()) shouldFly = false;
        if (onGround() && output.takeoff() < 0.05f && output.escape() < 0.05f && output.forward() < 0.05f) shouldFly = false;
        flying = shouldFly;
        setNoGravity(flying);

        Vec3d facing = Vec3d.fromPolar(0, getYaw());
        Vec3d side = new Vec3d(-facing.z, 0, facing.x);
        double thrust = output.forward() * 0.055;
        double strafe = output.strafe() * 0.035;
        double lift = flying ? output.vertical() * 0.045 : 0;
        Vec3d desired = facing.multiply(thrust).add(side.multiply(strafe)).add(0, lift, 0);
        Vec3d velocity = getVelocity().multiply(0.84).add(desired);
        if (!flying) velocity = new Vec3d(velocity.x * 0.65, Math.min(0, velocity.y), velocity.z * 0.65);
        if (velocity.lengthSquared() > 0.18 * 0.18) velocity = velocity.normalize().multiply(0.18);
        setVelocity(velocity);
        move(MovementType.SELF, getVelocity());
        setVelocity(getVelocity().multiply(flying ? 0.92 : 0.6));
        if (output.feeding() > 0.25f && distanceToFood(1.25)) feedingTicks = 20;
    }

    public MotorOutput motor() { return motor; }
    public boolean isFlying() { return flying; }
    public float wingPhase() { return wingPhase; }
    public boolean isFeeding() { return feedingTicks > 0; }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_BEE_LOOP; }
    @Override
    protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) { return SoundEvents.ENTITY_BEE_HURT; }
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_BEE_DEATH; }
    @Override
    protected float getSoundVolume() { return 0.25f; }
}
