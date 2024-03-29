package com.mmobuilder.voxel.g3d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import net.mgsx.gltf.loaders.shared.geometry.MeshTangentSpaceGenerator;
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;

import static com.mmobuilder.voxel.g3d.ChunkConstants.CHUNK_SIZE;
import static com.mmobuilder.voxel.g3d.ChunkConstants.WORLD_HEIGHT;

public class ChunkMeshGenerator {

    private final ModelBuilder modelBuilder = new ModelBuilder();
    private final VoxelCube voxelCube = new VoxelCube(true, true, true, true);

    private final ChunkHandler chunkHandler;

    private final Texture texture;

    /**
     * A cube has 6 faces.
     */
    private static final int CUBE_FACES = 6;
    /**
     * A quad has 4 vertices, one at each corner.
     */
    private static final int QUAD_VERTICES = 4;

    Texture diffuseTexture;
    Texture normalTexture;
    Texture mrTexture;

    public ChunkMeshGenerator(ChunkHandler chunkHandler, Texture texture) {
        this.chunkHandler = chunkHandler;
        this.texture = texture;

        // TODO: RELOCATE THIS TEXTURE INITIALIZATION TO SOMEWHERE ELSE!
        diffuseTexture = new Texture(Gdx.files.classpath("textures/dirt.png"), true);
        normalTexture = new Texture(Gdx.files.classpath("textures/dirt.png"), true);
        mrTexture = new Texture(Gdx.files.classpath("textures/dirt.png"), true);

        diffuseTexture.setFilter(Texture.TextureFilter.MipMapLinearNearest, Texture.TextureFilter.Nearest);
        diffuseTexture.setAnisotropicFilter(GLTexture.getMaxAnisotropicFilterLevel());

        normalTexture.setFilter(Texture.TextureFilter.MipMapLinearNearest, Texture.TextureFilter.Nearest);
        normalTexture.setAnisotropicFilter(GLTexture.getMaxAnisotropicFilterLevel());

        mrTexture.setFilter(Texture.TextureFilter.MipMapLinearNearest, Texture.TextureFilter.Nearest);
        mrTexture.setAnisotropicFilter(GLTexture.getMaxAnisotropicFilterLevel());
    }

    /**
     * Generates a chunk landscape model.
     */
    public void generateChunkModel(Chunk chunk) {
        // Define the attributes for this model
        VertexAttribute position = new VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE);
        VertexAttribute textureCoordinates = new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0");
        VertexAttribute normal = new VertexAttribute(VertexAttributes.Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE);
        VertexAttribute tangent = new VertexAttribute(VertexAttributes.Usage.Tangent, 4, ShaderProgram.TANGENT_ATTRIBUTE);
        VertexAttribute colorUnpacked = new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, ShaderProgram.COLOR_ATTRIBUTE);

        // Init vertices array
        float[] vertices = new float[((position.numComponents + normal.numComponents + tangent.numComponents + colorUnpacked.numComponents + textureCoordinates.numComponents) * QUAD_VERTICES * CHUNK_SIZE * CHUNK_SIZE * WORLD_HEIGHT) * CUBE_FACES];

        // Populate the vertices array with data
        int vertexOffset = 0;
        for (int y = 0; y < WORLD_HEIGHT; y++) {
            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {

                    int worldX = x + CHUNK_SIZE * chunk.getChunkX();
                    int worldZ = z + CHUNK_SIZE * chunk.getChunkZ();

                    Block block = chunkHandler.getBlock(worldX, y, worldZ);
                    if (block == null || !block.isVisible()) continue;

                    // Check neighboring blocks to determine which faces to cull
                    boolean top = isSolid(worldX, y + 1, worldZ);
                    boolean bot = isSolid(worldX, y - 1, worldZ);
                    boolean lef = isSolid(worldX - 1, y, worldZ);
                    boolean rig = isSolid(worldX + 1, y, worldZ);
                    boolean fro = isSolid(worldX, y, worldZ - 1);
                    boolean bac = isSolid(worldX, y, worldZ + 1);


                    if (!top) {
                        vertexOffset = voxelCube.createTop(vertices, vertexOffset, worldX, y, worldZ, block.getBlockColor(), new TextureRegion(diffuseTexture));
                    }
                    if (!bot) {
                        vertexOffset = voxelCube.createBottom(vertices, vertexOffset, worldX, y, worldZ, block.getBlockColor(), new TextureRegion(diffuseTexture));
                    }
                    if (!lef) {
                        vertexOffset = voxelCube.createLeft(vertices, vertexOffset, worldX, y, worldZ, block.getBlockColor(), new TextureRegion(diffuseTexture));
                    }
                    if (!rig) {
                        vertexOffset = voxelCube.createRight(vertices, vertexOffset, worldX, y, worldZ, block.getBlockColor(), new TextureRegion(diffuseTexture));
                    }
                    if (!fro) {
                        vertexOffset = voxelCube.createFront(vertices, vertexOffset, worldX, y, worldZ, block.getBlockColor(), new TextureRegion(diffuseTexture));
                    }
                    if (!bac) {
                        vertexOffset = voxelCube.createBack(vertices, vertexOffset, worldX, y, worldZ, block.getBlockColor(), new TextureRegion(diffuseTexture));
                    }
                }
            }
        }

        // Generate the indices
        int size = (6 * CHUNK_SIZE * CHUNK_SIZE * WORLD_HEIGHT) * CUBE_FACES;
        short[] indices = new short[size];
        generateIndices(indices);

        // Create the mesh
        Mesh terrainMesh = new Mesh(true, vertices.length, indices.length, position, textureCoordinates, normal, tangent, colorUnpacked);
        terrainMesh.setVertices(vertices);
        terrainMesh.setIndices(indices);

        // Create the MeshPart
        MeshPart meshPart = new MeshPart(null, terrainMesh, 0, size, GL30.GL_TRIANGLES);

        // Create a model out of the MeshPart
        Material material = new Material();
        material.set(PBRTextureAttribute.createBaseColorTexture(diffuseTexture));
        material.set(PBRTextureAttribute.createNormalTexture(normalTexture));
        material.set(PBRTextureAttribute.createMetallicRoughnessTexture(mrTexture));
        material.set(PBRColorAttribute.createBaseColorFactor(new Color(Color.WHITE).fromHsv(15, .9f, .8f)));
//        material.set(PBRColorAttribute.createEmissive(new Color(Color.RED)));

        modelBuilder.begin();
        modelBuilder.part(meshPart, material);
        Model model = modelBuilder.end();

        for(Mesh modelMesh : model.meshes){
            MeshTangentSpaceGenerator.computeTangentSpace(modelMesh, material, true, true);
        }

        chunk.setModel(model);
    }

    @SuppressWarnings("PointlessArithmeticExpression")
    public void generateIndices(short[] indices) {
        short j = 0;
        for (int i = 0; i < indices.length; i += 6, j += 4) {
            indices[i + 0] = (short) (j + 2);
            indices[i + 1] = (short) (j + 1);
            indices[i + 2] = (short) (j + 3);
            indices[i + 3] = (short) (j + 0);
            indices[i + 4] = (short) (j + 3);
            indices[i + 5] = (short) (j + 1);
        }
    }

    private boolean isSolid(int worldX, int worldY, int worldZ) {
        if (BlockUtil.isBlockOutsideWorldY(worldY)) return false;
        Block block = chunkHandler.getBlock(worldX, worldY, worldZ);

        if (block == null) return false;
        return block.isVisible();
    }

}
