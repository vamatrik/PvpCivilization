package com.pvpciv.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/**
 * 0.9.9 — модель бойцов этажа 3 вместо зомби-позы: руки опущены, в бою оружие «наготове»,
 * удар — замах из-за головы и рубящий удар с поворотом корпуса.
 */
public class FightModel<T extends Mob> extends HumanoidModel<T> {
    private final boolean claws;

    public FightModel(ModelPart root, boolean claws) { super(root); this.claws = claws; }

    @Override
    public void setupAnim(T e, float limb, float limbAmt, float age, float yaw, float pitch) {
        super.setupAnim(e, limb, limbAmt, age, yaw, pitch);
        if (e.isAggressive() && attackTime <= 0) {
            // стойка: оружие вперёд, вторая рука чуть в сторону, лёгкое покачивание
            float sway = Mth.sin(age * 0.12f) * 0.05f;
            rightArm.xRot = rightArm.xRot * 0.4f - (claws ? 0.9f : 0.75f) + sway;
            rightArm.yRot = -0.15f;
            leftArm.xRot = leftArm.xRot * 0.4f - (claws ? 0.9f : 0.35f) - sway;
            leftArm.zRot = claws ? -0.1f : -0.25f;
            body.xRot = 0.08f;
            if (claws) head.xRot += 0.15f;
        }
    }

    @Override
    protected void setupAttackAnimation(T e, float age) {
        if (attackTime <= 0) return;
        float t = attackTime;
        float upX = claws ? -2.4f : -2.9f;
        if (t < 0.4f) {   // замах
            float k = t / 0.4f;
            rightArm.xRot = Mth.lerp(k, -0.75f, upX);
            rightArm.zRot = Mth.lerp(k, 0f, -0.35f);
            body.yRot = -0.4f * k;
        } else {          // удар
            float k = (t - 0.4f) / 0.6f, ease = 1 - (1 - k) * (1 - k) * (1 - k);
            rightArm.xRot = Mth.lerp(ease, upX, 0.35f);
            rightArm.zRot = Mth.lerp(ease, -0.35f, 0.15f);
            body.yRot = Mth.lerp(ease, -0.4f, 0.35f);
        }
        rightArm.yRot = body.yRot;
        rightArm.z = Mth.sin(body.yRot) * 5f;
        rightArm.x = -Mth.cos(body.yRot) * 5f;
        leftArm.z = -Mth.sin(body.yRot) * 5f;
        leftArm.x = Mth.cos(body.yRot) * 5f;
        leftArm.yRot = body.yRot;
        if (claws) {   // оборотень бьёт обеими лапами со сдвигом
            float k2 = Mth.clamp((t - 0.2f) / 0.8f, 0, 1);
            leftArm.xRot = k2 < 0.35f ? Mth.lerp(k2 / 0.35f, -0.9f, upX) : Mth.lerp((k2 - 0.35f) / 0.65f, upX, 0.35f);
        }
    }
}
