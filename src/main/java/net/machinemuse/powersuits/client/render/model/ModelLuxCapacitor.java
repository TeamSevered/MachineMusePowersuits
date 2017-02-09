package net.machinemuse.powersuits.client.render.model;

import com.google.common.collect.ImmutableList;
import net.machinemuse.numina.geometry.Colour;
import net.machinemuse.powersuits.block.BlockLuxCapacitor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.property.IExtendedBlockState;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import java.util.*;

/**
 * Ported to Java by lehjr on 12/27/16.
 */
public class ModelLuxCapacitor implements IBakedModel, IPerspectiveAwareModel {
    private static ResourceLocation location;
    private static final LuxCapModelHelper modelHelper = LuxCapModelHelper.getInstance();
    private static IBakedModel baseBakedModel; // used mainly for the Item model

    public ModelLuxCapacitor(IBakedModel bakedModelIn) {
        this.baseBakedModel = bakedModelIn;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        return modelHelper.getQuads(state, side, rand);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return baseBakedModel.isAmbientOcclusion();
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformTypeIn) {
        Matrix4f matrix;
        if (baseBakedModel != null && baseBakedModel instanceof IPerspectiveAwareModel) {
            matrix = ((IPerspectiveAwareModel) baseBakedModel).handlePerspective(cameraTransformTypeIn).getValue();
        } else {
            matrix = TRSRTransformation.identity().getMatrix();
        }
        return Pair.of(this, matrix);
    }

    @Override
    public boolean isBuiltInRenderer() {
        return baseBakedModel.isBuiltInRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return baseBakedModel.getParticleTexture();
    }

    @Override
    public boolean isGui3d() {
        return baseBakedModel.isAmbientOcclusion();
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return baseBakedModel.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return baseBakedModel.getOverrides();
    }
}