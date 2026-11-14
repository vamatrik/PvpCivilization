package com.pvpciv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pvpciv.PvpCiv;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 0.9.9 — объёмные детали мобов этажа 3 поверх гуманоидной модели: шлемы, маски, морды, уши, хвосты, колчаны, наплечники.
 * Детали крепятся к костям (голова/тело/руки/ноги) и повторяют их позу; хвосты и уши слегка качаются.
 * Текстура деталей — палитра 256×256: 16 плиток по 64×64, номер плитки = цвет (tools/mob3d_tex.py).
 */
public class Mob3DLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    public enum Bone { HEAD, BODY, RARM, LARM, RLEG, LLEG }

    /** Один кубик детали. wob — качается (0 — нет, 1 — хвост, 2 — ухо). */
    public record Cube(Bone bone, int color, float x, float y, float z, float w, float h, float d,
                       float px, float py, float pz, float rx, float ry, float rz, int wob) {}

    public static Cube c(Bone b, int col, float x, float y, float z, float w, float h, float d) { return new Cube(b, col, x, y, z, w, h, d, 0, 0, 0, 0, 0, 0, 0); }
    /** Кубик с поворотом вокруг точки (px,py,pz) в координатах кости. */
    public static Cube r(Bone b, int col, float px, float py, float pz, float rx, float ry, float rz, float x, float y, float z, float w, float h, float d) {
        return new Cube(b, col, x, y, z, w, h, d, px, py, pz, rx, ry, rz, 0);
    }
    public static Cube wob(Cube c, int kind) { return new Cube(c.bone, c.color, c.x, c.y, c.z, c.w, c.h, c.d, c.px, c.py, c.pz, c.rx, c.ry, c.rz, kind); }

    private final ResourceLocation tex;
    private final ModelPart[] bones = new ModelPart[6];
    private final List<ModelPart> wobTail = new ArrayList<>(), wobEar = new ArrayList<>();
    private final List<float[]> wobTailBase = new ArrayList<>(), wobEarBase = new ArrayList<>();

    private java.util.function.Predicate<T> when = e -> true;
    public Mob3DLayer<T, M> when(java.util.function.Predicate<T> w) { when = w; return this; }

    public Mob3DLayer(RenderLayerParent<T, M> parent, String texName, List<Cube> cubes) {
        super(parent);
        this.tex = PvpCiv.id("textures/entity/mob3d/" + texName + ".png");
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition[] b = new PartDefinition[6];
        String[] names = {"head", "body", "rarm", "larm", "rleg", "lleg"};
        for (int i = 0; i < 6; i++) b[i] = root.addOrReplaceChild(names[i], CubeListBuilder.create(), PartPose.ZERO);
        int n = 0;
        List<String> tails = new ArrayList<>(), ears = new ArrayList<>();
        for (Cube c : cubes) {
            int u = (c.color % 4) * 64, v = (c.color / 4) * 64;
            String id = "p" + (n++);
            b[c.bone.ordinal()].addOrReplaceChild(id,
                    CubeListBuilder.create().texOffs(u, v).addBox(c.x - c.px, c.y - c.py, c.z - c.pz, c.w, c.h, c.d),
                    PartPose.offsetAndRotation(c.px, c.py, c.pz, c.rx, c.ry, c.rz));
            if (c.wob == 1) tails.add(c.bone.ordinal() + ":" + id);
            if (c.wob == 2) ears.add(c.bone.ordinal() + ":" + id);
        }
        ModelPart baked = LayerDefinition.create(mesh, 256, 256).bakeRoot();
        for (int i = 0; i < 6; i++) bones[i] = baked.getChild(names[i]);
        for (String s : tails) { ModelPart p = bones[s.charAt(0) - '0'].getChild(s.substring(2)); wobTail.add(p); wobTailBase.add(new float[]{p.xRot, p.yRot, p.zRot}); }
        for (String s : ears) { ModelPart p = bones[s.charAt(0) - '0'].getChild(s.substring(2)); wobEar.add(p); wobEarBase.add(new float[]{p.xRot, p.yRot, p.zRot}); }
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, T e, float limb, float limbAmt, float pt, float age, float yaw, float pitch) {
        if (e.isInvisible() || !when.test(e)) return;
        M m = getParentModel();
        bones[0].copyFrom(m.head); bones[1].copyFrom(m.body);
        bones[2].copyFrom(m.rightArm); bones[3].copyFrom(m.leftArm);
        bones[4].copyFrom(m.rightLeg); bones[5].copyFrom(m.leftLeg);
        float t = age + e.getId() * 7;
        for (int i = 0; i < wobTail.size(); i++) {
            float[] b0 = wobTailBase.get(i); ModelPart p = wobTail.get(i);
            p.xRot = b0[0] + (float) Math.sin(t * 0.11 + i) * 0.08f + limbAmt * 0.25f;
            p.yRot = b0[1] + (float) Math.sin(t * 0.15 + i * 1.3) * 0.35f;
            p.zRot = b0[2];
        }
        for (int i = 0; i < wobEar.size(); i++) {
            float[] b0 = wobEarBase.get(i); ModelPart p = wobEar.get(i);
            float tw = (t % 90) < 6 ? (float) Math.sin((t % 90) * Math.PI / 3) * 0.25f : 0;
            p.xRot = b0[0] + tw; p.yRot = b0[1]; p.zRot = b0[2];
        }
        int overlay = LivingEntityRenderer.getOverlayCoords(e, 0);
        var vc = buf.getBuffer(RenderType.entityCutoutNoCull(tex));
        for (ModelPart p : bones) p.render(ps, vc, light, overlay);
    }

    @SuppressWarnings("unused") private static final int O = OverlayTexture.NO_OVERLAY;
}
