/*
 * Minecraft Forge
 * Copyright (c) 2016.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.machinemuse.powersuits.client.render.model.obj;

import net.machinemuse.numina.general.MuseLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Level;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.util.*;

/*
 * Loader for OBJ models.
 * To enable your mod call instance.addDomain(modid).
 * If you need more control over accepted resources - extend the class, and register a new instance with ModelLoaderRegistry.
 *
 * LIES!!! you cannot extend enum in java as they are treated as final.
 *
 */
@SideOnly(Side.CLIENT)
public enum MPSOBJLoader implements ICustomModelLoader {
    INSTANCE;

    private IResourceManager manager;
    private final Set<String> enabledDomains = new HashSet<String>();
    private final Map<ResourceLocation, OBJModelPlus> cache = new HashMap<>();
    private final Map<ResourceLocation, Exception> errors = new HashMap<>();

    public void addDomain(String domain) {
        enabledDomains.add(domain.toLowerCase());
        FMLLog.log(Level.INFO, "MPSOBJLoader: Domain %s has been added.", domain.toLowerCase());
    }

    public boolean accepts(ResourceLocation modelLocation) {
        return enabledDomains.contains(modelLocation.getResourceDomain()) && modelLocation.getResourcePath().endsWith(".obj");
    }

    @Nullable
    private IModel loadModelWithoutCaching(ResourceLocation modelLocation) throws Exception {
        ResourceLocation file = new ResourceLocation(modelLocation.getResourceDomain(), modelLocation.getResourcePath());
        OBJModelPlus model = null;

        IResource resource;
        try {
            resource = manager.getResource(file);
        } catch (FileNotFoundException e) {
            MuseLogger.logException("failed to load model: ", e);
            if (modelLocation.getResourcePath().startsWith("models/block/"))
                resource = manager.getResource(new ResourceLocation(file.getResourceDomain(), "models/item/" + file.getResourcePath().substring("models/block/".length())));
            else if (modelLocation.getResourcePath().startsWith("models/item/"))
                resource = manager.getResource(new ResourceLocation(file.getResourceDomain(), "models/block/" + file.getResourcePath().substring("models/item/".length())));
            else if (modelLocation.getResourcePath().startsWith("models/models/item/"))
                resource = manager.getResource(new ResourceLocation(file.getResourceDomain(), "models/item/" + file.getResourcePath().substring("models/models/item/".length())));
            else if (modelLocation.getResourcePath().startsWith("models/models/block/"))
                resource = manager.getResource(new ResourceLocation(file.getResourceDomain(), "models/block/" + file.getResourcePath().substring("models/models/block/".length())));
            else throw e;
        }
        OBJModelPlus.Parser parser = new OBJModelPlus.Parser(resource, manager);
        try {
            model = parser.parse();
        } catch (Exception e) {
            errors.put(modelLocation, e);
        }
        return model;
    }

    public void registerModelSprites(ResourceLocation modelLocation) throws Exception {
        try {
            OBJModelPlus model = (OBJModelPlus) loadModelWithoutCaching(modelLocation);
            if (model != null) {
                Collection<ResourceLocation> spriteList = model.getTextures();
                for (ResourceLocation spriteLocation: spriteList)
                    Minecraft.getMinecraft().getTextureMapBlocks().registerSprite(spriteLocation);
            }
        } catch (Exception e) {
            throw new ModelLoaderRegistry.LoaderException("Error loading model previously: " + modelLocation, errors.get(modelLocation));
        }
    }

    @Override
    public IModel loadModel(ResourceLocation modelLocation) throws Exception {
        ResourceLocation file = new ResourceLocation(modelLocation.getResourceDomain(), modelLocation.getResourcePath());
         OBJModelPlus model = null;
        if (!cache.containsKey(file)) {
            model = (OBJModelPlus) loadModelWithoutCaching(file);
            if (model!= null) {
                cache.put(file, model);
            } else
                throw new ModelLoaderRegistry.LoaderException("Error loading model previously: " + file, errors.get(modelLocation));
        } else
            model = cache.get(file);
        return model;
    }

    public void onResourceManagerReload(IResourceManager resourceManager) {
        this.manager = resourceManager;
        cache.clear();
        errors.clear();
    }
}