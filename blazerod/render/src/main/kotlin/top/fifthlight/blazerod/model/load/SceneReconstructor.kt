package top.fifthlight.blazerod.model.load

import net.minecraft.client.gl.RenderPassImpl
import top.fifthlight.blazerod.model.RenderScene
import top.fifthlight.blazerod.model.TransformId
import top.fifthlight.blazerod.model.node.RenderNode
import top.fifthlight.blazerod.model.node.RenderNodeComponent.*
import top.fifthlight.blazerod.model.resource.RenderCamera
import top.fifthlight.blazerod.model.resource.RenderMaterial
import top.fifthlight.blazerod.model.resource.RenderPrimitive
import top.fifthlight.blazerod.model.resource.RenderTexture
import top.fifthlight.blazerod.util.checkInUse

class SceneReconstructor private constructor(private val info: GpuLoadModelLoadInfo) {
    private suspend fun loadTexture(
        textureInfo: MaterialLoadInfo.TextureInfo?,
        fallback: RenderTexture = RenderTexture.Companion.WHITE_RGBA_TEXTURE,
    ) = textureInfo?.let {
        info.textures[textureInfo.textureIndex].await()
    } ?: fallback

    private suspend fun loadMaterial(materialLoadInfo: MaterialLoadInfo) = when (materialLoadInfo) {
        is MaterialLoadInfo.Pbr -> RenderMaterial.Pbr(
            name = materialLoadInfo.name,
            baseColor = materialLoadInfo.baseColor,
            baseColorTexture = loadTexture(materialLoadInfo.baseColorTexture),
            metallicFactor = materialLoadInfo.metallicFactor,
            roughnessFactor = materialLoadInfo.roughnessFactor,
            metallicRoughnessTexture = loadTexture(materialLoadInfo.metallicRoughnessTexture),
            normalTexture = loadTexture(materialLoadInfo.normalTexture),
            occlusionTexture = loadTexture(materialLoadInfo.occlusionTexture),
            emissiveTexture = loadTexture(materialLoadInfo.emissiveTexture),
            emissiveFactor = materialLoadInfo.emissiveFactor,
            alphaMode = materialLoadInfo.alphaMode,
            alphaCutoff = materialLoadInfo.alphaCutoff,
            doubleSided = materialLoadInfo.doubleSided,
            skinned = materialLoadInfo.skinned,
            morphed = materialLoadInfo.morphed,
        )

        is MaterialLoadInfo.Unlit -> RenderMaterial.Unlit(
            name = materialLoadInfo.name,
            baseColor = materialLoadInfo.baseColor,
            baseColorTexture = loadTexture(materialLoadInfo.baseColorTexture),
            alphaMode = materialLoadInfo.alphaMode,
            alphaCutoff = materialLoadInfo.alphaCutoff,
            doubleSided = materialLoadInfo.doubleSided,
            skinned = materialLoadInfo.skinned,
            morphed = materialLoadInfo.morphed,
        )
    }

    private val cameras = mutableListOf<RenderCamera>()
    private suspend fun loadNode(
        index: Int,
        node: NodeLoadInfo,
    ) = RenderNode(
        nodeId = node.nodeId,
        nodeName = node.nodeName,
        humanoidTags = node.humanoidTags,
        nodeIndex = index,
        absoluteTransform = node.transform,
        components = node.components.mapNotNull { component ->
            when (component) {
                is NodeLoadInfo.Component.Primitive -> {
                    val primitiveInfo = info.primitiveInfos[component.infoIndex]
                    val vertexBuffer = info.vertexBuffers[primitiveInfo.vertexBufferIndex].await()
                    val indexBuffer = primitiveInfo.indexBufferIndex?.let { index -> info.indexBuffers[index].await() }
                    val material = primitiveInfo.materialInfo?.let { materialLoadInfo ->
                        loadMaterial(materialLoadInfo)
                    } ?: RenderMaterial.defaultMaterial
                    val targets =
                        primitiveInfo.morphedPrimitiveIndex?.let { index -> info.morphTargetInfos[index].await() }
                    Primitive(
                        primitiveIndex = component.infoIndex,
                        primitive = RenderPrimitive(
                            vertices = primitiveInfo.vertices,
                            vertexFormatMode = primitiveInfo.vertexFormatMode,
                            vertexBuffer = vertexBuffer,
                            indexBuffer = indexBuffer,
                            material = material,
                            targets = targets?.let {
                                RenderPrimitive.Targets(
                                    position = it.position,
                                    color = it.color,
                                    texCoord = it.texCoord,
                                )
                            },
                            targetGroups = targets?.targetGroups ?: listOf(),
                        ),
                        skinIndex = primitiveInfo.skinIndex,
                        morphedPrimitiveIndex = primitiveInfo.morphedPrimitiveIndex,
                    )
                }

                is NodeLoadInfo.Component.Joint -> {
                    Joint(
                        skinIndex = component.skinIndex,
                        jointIndex = component.jointIndex,
                    )
                }

                is NodeLoadInfo.Component.Camera -> {
                    val cameraIndex = cameras.size
                    cameras.add(
                        RenderCamera(
                            cameraIndex = cameraIndex,
                            camera = component.camera,
                        )
                    )
                    Camera(cameraIndex)
                }

                is NodeLoadInfo.Component.InfluenceTarget -> {
                    val influence = component.influence
                    InfluenceTarget(
                        sourceNodeIndex = info.nodes.indexOfFirst { it.nodeId == influence.source }.takeIf { it >= 0 }
                            ?: return@mapNotNull null,
                        influence = influence.influence,
                        influenceRotation = influence.influenceRotation,
                        influenceTranslation = influence.influenceTranslation,
                        target = TransformId.INFLUENCE,
                    )
                }

                else -> null
            }
        },
    )

    private suspend fun reconstruct(): RenderScene {
        val nodes = info.nodes.mapIndexed { index, node -> loadNode(index, node) }
        for ((index, node) in nodes.withIndex()) {
            val nodeInfo = info.nodes[index]
            node.initializeChildren(nodeInfo.childrenIndices.map { nodes[it] })
        }
        return RenderScene(
            rootNode = nodes[info.rootNodeIndex],
            nodes = nodes,
            skins = info.skins,
            expressions = info.expressions,
            expressionGroups = info.expressionGroups,
            cameras = cameras,
        )
    }

    companion object {
        suspend fun reconstruct(info: GpuLoadModelLoadInfo) = SceneReconstructor(info).reconstruct().also {
            if (RenderPassImpl.IS_DEVELOPMENT) {
                info.textures.forEach { it.await()?.checkInUse() }
                info.indexBuffers.forEach { it.await().checkInUse() }
                info.vertexBuffers.forEach { it.await().checkInUse() }
            }
        }
    }
}
