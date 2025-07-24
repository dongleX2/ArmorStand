package top.fifthlight.blazerod.animation

import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.blazerod.animation.AnimationChannelItem.*
import top.fifthlight.blazerod.model.RenderScene
import top.fifthlight.blazerod.model.animation.Animation
import top.fifthlight.blazerod.model.animation.AnimationChannel
import top.fifthlight.blazerod.model.util.MutableFloat

object AnimationLoader {
    fun load(
        scene: RenderScene,
        animation: Animation,
    ): AnimationItem {
        fun AnimationChannel.Type.NodeData.findTargetTransformIndex(): Int? {
            val node = targetNode?.id.let { nodeId -> scene.nodeIdMap[nodeId] }
                ?: targetNodeName?.let { name -> scene.nodeNameMap[name] }
                ?: targetHumanoidTag?.let { humanoid ->
                    scene.humanoidTagMap[humanoid]
                }
            return node?.nodeIndex
        }

        @Suppress("UNCHECKED_CAST")
        fun mapAnimationChannel(channel: AnimationChannel<*, *>): AnimationChannelItem<*, *>? {
            return when (channel.type) {
                AnimationChannel.Type.Translation -> {
                    val data = channel.data as AnimationChannel.Type.TransformData
                    TranslationItem(
                        index = data.node.findTargetTransformIndex() ?: return null,
                        transformId = data.transformId,
                        channel = channel as AnimationChannel<Vector3f, Unit>,
                    )
                }

                AnimationChannel.Type.Scale -> {
                    val data = channel.data as AnimationChannel.Type.TransformData
                    ScaleItem(
                        index = data.node.findTargetTransformIndex() ?: return null,
                        transformId = data.transformId,
                        channel = channel as AnimationChannel<Vector3f, Unit>,
                    )
                }

                AnimationChannel.Type.Rotation -> {
                    val data = channel.data as AnimationChannel.Type.TransformData
                    RotationItem(
                        index = data.node.findTargetTransformIndex() ?: return null,
                        transformId = data.transformId,
                        channel = channel as AnimationChannel<Quaternionf, Unit>,
                    )
                }

                AnimationChannel.Type.Morph -> {
                    val data = channel.data as AnimationChannel.Type.MorphData
                    MorphItem(
                        primitiveIndex = data.nodeData.findTargetTransformIndex() ?: return null,
                        targetGroupIndex = data.targetMorphGroupIndex,
                        channel = channel as AnimationChannel<MutableFloat, AnimationChannel.Type.MorphData>,
                    )
                }

                AnimationChannel.Type.Expression -> {
                    val channel =
                        channel as AnimationChannel<MutableFloat, AnimationChannel.Type.ExpressionData>
                    val data = channel.data
                    scene.expressions.firstOrNull { (it.name != null && it.name == data.name) || (it.tag != null && it.tag == data.tag) }
                        ?.let { ExpressionItem(it, channel) }
                        ?: scene.expressionGroups.firstOrNull { (it.name != null && it.name == data.name) || (it.tag != null && it.tag == data.tag) }
                            ?.let { ExpressionGroupItem(it, channel) }
                }

                AnimationChannel.Type.CameraFov -> {
                    val data = channel.data as AnimationChannel.Type.CameraData
                    val cameraIndex =
                        scene.cameras.indexOfFirst { it.camera.name == data.cameraName }.takeIf { it >= 0 }
                            ?: return null
                    CameraFovItem(
                        cameraIndex = cameraIndex,
                        channel = channel as AnimationChannel<MutableFloat, AnimationChannel.Type.CameraData>,
                    )
                }

                AnimationChannel.Type.MMDCameraDistance -> {
                    val data = channel.data as AnimationChannel.Type.CameraData
                    val cameraIndex =
                        scene.cameras.indexOfFirst { it.camera.name == data.cameraName }.takeIf { it >= 0 }
                            ?: return null
                    MMDCameraDistanceItem(
                        cameraIndex = cameraIndex,
                        channel = channel as AnimationChannel<MutableFloat, AnimationChannel.Type.CameraData>,
                    )
                }

                AnimationChannel.Type.MMDCameraRotation -> {
                    val data = channel.data as AnimationChannel.Type.CameraData
                    val cameraIndex =
                        scene.cameras.indexOfFirst { it.camera.name == data.cameraName }.takeIf { it >= 0 }
                            ?: return null
                    MMDCameraRotationItem(
                        cameraIndex = cameraIndex,
                        channel = channel as AnimationChannel<Vector3f, AnimationChannel.Type.CameraData>,
                    )
                }

                AnimationChannel.Type.MMDCameraTarget -> {
                    val data = channel.data as AnimationChannel.Type.CameraData
                    val cameraIndex =
                        scene.cameras.indexOfFirst { it.camera.name == data.cameraName }.takeIf { it >= 0 }
                            ?: return null
                    MMDCameraTargetItem(
                        cameraIndex = cameraIndex,
                        channel = channel as AnimationChannel<Vector3f, AnimationChannel.Type.CameraData>,
                    )
                }
            }
        }

        return AnimationItem(
            name = animation.name,
            channels = animation.channels.mapNotNull(::mapAnimationChannel)
        )
    }
}