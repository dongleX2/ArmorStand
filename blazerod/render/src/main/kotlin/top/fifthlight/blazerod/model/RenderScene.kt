package top.fifthlight.blazerod.model

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.util.Identifier
import org.joml.Matrix4fc
import top.fifthlight.blazerod.model.node.RenderNode
import top.fifthlight.blazerod.model.node.RenderNodeComponent
import top.fifthlight.blazerod.model.node.UpdatePhase
import top.fifthlight.blazerod.model.node.forEach
import top.fifthlight.blazerod.model.resource.RenderCamera
import top.fifthlight.blazerod.model.resource.RenderExpression
import top.fifthlight.blazerod.model.resource.RenderExpressionGroup
import top.fifthlight.blazerod.model.resource.RenderSkin
import top.fifthlight.blazerod.util.AbstractRefCount

class RenderScene(
    val rootNode: RenderNode,
    val nodes: List<RenderNode>,
    val skins: List<RenderSkin>,
    val expressions: List<RenderExpression>,
    val expressionGroups: List<RenderExpressionGroup>,
    val cameras: List<RenderCamera>,
) : AbstractRefCount() {
    companion object {
        private val TYPE_ID = Identifier.of("blazerod", "scene")
    }

    override val typeId: Identifier
        get() = TYPE_ID

    private val sortedNodes: List<RenderNode>
    private val debugRenderNodes: List<RenderNode>
    val primitiveComponents: List<RenderNodeComponent.Primitive>
    val morphedPrimitiveComponents: List<RenderNodeComponent.Primitive>
    val nodeIdMap: Map<NodeId, RenderNode>
    val nodeNameMap: Map<String, RenderNode>
    val humanoidTagMap: Map<HumanoidTag, RenderNode>
    init {
        rootNode.increaseReferenceCount()
        val nodes = mutableListOf<RenderNode>()
        val debugRenderNodes = mutableListOf<RenderNode>()
        val primitiveComponents = mutableListOf<RenderNodeComponent.Primitive>()
        val morphedPrimitives = Int2ReferenceOpenHashMap<RenderNodeComponent.Primitive>()
        val nodeIdMap = mutableMapOf<NodeId, RenderNode>()
        val nodeNameMap = mutableMapOf<String, RenderNode>()
        val humanoidTagMap = mutableMapOf<HumanoidTag, RenderNode>()
        rootNode.forEach { node ->
            nodes.add(node)
            node.nodeId?.let { nodeIdMap.put(it, node) }
            node.nodeName?.let { nodeNameMap.put(it, node) }
            node.humanoidTags.forEach { humanoidTagMap[it] = node }
            if (node.hasPhase(UpdatePhase.Type.DEBUG_RENDER)) {
                debugRenderNodes.add(node)
            }
            node.getComponentsOfType(RenderNodeComponent.Type.Primitive)?.let { components ->
                primitiveComponents.addAll(components)
                for (component in components) {
                    component.morphedPrimitiveIndex?.let { index ->
                        if (morphedPrimitives.containsKey(index)) {
                            throw IllegalStateException("Duplicate morphed primitive index: $index")
                        }
                        morphedPrimitives.put(index, component)
                    }
                }
            }
        }
        this.sortedNodes = nodes
        this.debugRenderNodes = debugRenderNodes
        this.primitiveComponents = primitiveComponents
        this.morphedPrimitiveComponents = (0 until morphedPrimitives.size).map {
            morphedPrimitives.get(it) ?: error("Morphed primitive index not found: $it")
        }
        this.nodeIdMap = nodeIdMap
        this.nodeNameMap = nodeNameMap
        this.humanoidTagMap = humanoidTagMap
    }

    private fun executePhase(instance: ModelInstance, phase: UpdatePhase) {
        for (node in sortedNodes) {
            node.update(phase, node, instance)
        }
    }

    fun updateCamera(instance: ModelInstance) {
        if (cameras.isEmpty()) {
            return
        }
        if (instance.modelData.undirtyNodeCount == nodes.size) {
            return
        }
        executePhase(instance, UpdatePhase.InfluenceTransformUpdate)
        executePhase(instance, UpdatePhase.GlobalTransformPropagation)
        executePhase(instance, UpdatePhase.CameraUpdate)
    }

    fun debugRender(instance: ModelInstance, viewProjectionMatrix: Matrix4fc, consumers: VertexConsumerProvider) {
        if (debugRenderNodes.isEmpty()) {
            return
        }
        if (instance.modelData.undirtyNodeCount != nodes.size) {
            executePhase(instance, UpdatePhase.InfluenceTransformUpdate)
            executePhase(instance, UpdatePhase.GlobalTransformPropagation)
        }
        UpdatePhase.DebugRender.acquire(viewProjectionMatrix, consumers).use {
            executePhase(instance, it)
        }
    }

    fun updateRenderData(instance: ModelInstance) {
        if (instance.modelData.undirtyNodeCount == nodes.size) {
            return
        }
        executePhase(instance, UpdatePhase.InfluenceTransformUpdate)
        executePhase(instance, UpdatePhase.GlobalTransformPropagation)
        executePhase(instance, UpdatePhase.RenderDataUpdate)
    }

    override fun onClosed() {
        rootNode.decreaseReferenceCount()
    }
}
