package top.fifthlight.blazerod.model.data

import net.minecraft.util.Identifier
import org.joml.Matrix4f
import org.joml.Matrix4fc
import top.fifthlight.blazerod.model.RenderScene
import top.fifthlight.blazerod.util.AbstractRefCount
import top.fifthlight.blazerod.util.CowBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ModelMatricesBuffer private constructor(val primitiveNodesSize: Int) : CowBuffer.Content<ModelMatricesBuffer>,
    AbstractRefCount() {
    constructor(scene: RenderScene) : this(scene.primitiveComponents.size)

    companion object {
        private val IDENTITY = Matrix4f()
        const val MAT4X4_SIZE = 4 * 4 * 4
        private val TYPE_ID = Identifier.of("blazerod", "model_matrices_buffer")
    }

    override val typeId: Identifier
        get() = TYPE_ID

    val buffer: ByteBuffer = ByteBuffer.allocateDirect(primitiveNodesSize * MAT4X4_SIZE).order(ByteOrder.nativeOrder())

    fun clear() {
        repeat(primitiveNodesSize) {
            IDENTITY.get(it * MAT4X4_SIZE, buffer)
        }
        buffer.clear()
    }

    fun setMatrix(index: Int, src: Matrix4fc) {
        src.get(index * MAT4X4_SIZE, buffer)
    }

    fun getMatrix(index: Int, dest: Matrix4f) {
        dest.set(index * MAT4X4_SIZE, buffer)
    }

    override fun copy(): ModelMatricesBuffer = ModelMatricesBuffer(primitiveNodesSize).also {
        it.buffer.clear()
        buffer.clear()
        it.buffer.put(buffer)
        it.buffer.clear()
    }

    override fun onClosed() = Unit
}