package top.fifthlight.blazerod.model.node.component

import top.fifthlight.blazerod.model.ModelInstance
import top.fifthlight.blazerod.model.node.RenderNode
import top.fifthlight.blazerod.model.node.UpdatePhase
import top.fifthlight.blazerod.model.node.getWorldTransform

class Camera : RenderNodeComponent<Camera> {
    val cameraIndex: Int

    constructor(cameraIndex: Int) : super() {
        this.cameraIndex = cameraIndex
    }

    override fun onClosed() {}

    override val type: Type<Camera>
        get() = Type.Camera

    companion object {
        private val updatePhases = listOf(UpdatePhase.Type.CAMERA_UPDATE)
    }

    override val updatePhases
        get() = Companion.updatePhases

    override fun update(phase: UpdatePhase, node: RenderNode, instance: ModelInstance) {
        if (phase is UpdatePhase.CameraUpdate) {
            val cameraTransform = instance.modelData.cameraTransforms[cameraIndex]
            cameraTransform.update(instance.getWorldTransform(node))
        }
    }
}