package top.fifthlight.blazerod.model.renderer

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexFormatElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.render.OverlayTexture
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import top.fifthlight.blazerod.extension.setVertexFormatMode
import top.fifthlight.blazerod.model.RenderScene
import top.fifthlight.blazerod.model.RenderTask
import top.fifthlight.blazerod.model.data.MorphTargetBuffer
import top.fifthlight.blazerod.model.data.RenderSkinBuffer
import top.fifthlight.blazerod.model.resource.RenderMaterial
import top.fifthlight.blazerod.model.resource.RenderPrimitive
import top.fifthlight.blazerod.model.util.getUByteNormalized
import top.fifthlight.blazerod.model.util.toNormalizedSByte
import top.fifthlight.blazerod.model.util.toNormalizedUByte
import top.fifthlight.blazerod.render.BlazerodVertexFormatElements
import top.fifthlight.blazerod.render.setIndexBuffer
import top.fifthlight.blazerod.util.CpuBufferPool
import top.fifthlight.blazerod.util.GpuShaderDataPool
import top.fifthlight.blazerod.util.forEachInt
import top.fifthlight.blazerod.util.upload
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class CpuTransformRenderer private constructor() :
    Renderer<CpuTransformRenderer, CpuTransformRenderer.Type>() {
    companion object Type : Renderer.Type<CpuTransformRenderer, Type>() {
        override val isAvailable: Boolean
            get() = true
        override val supportInstancing: Boolean
            get() = false

        @JvmStatic
        override fun create() = CpuTransformRenderer()
    }

    override val type: Type
        get() = Type

    private val dataPool = GpuShaderDataPool.create(
        usage = GpuBuffer.USAGE_VERTEX,
        extraUsage = 0,
        alignment = 0,
        supportSlicing = false,
    )
    private val cpuPool = CpuBufferPool()

    private fun VertexFormat.getOffsetOrNull(element: VertexFormatElement) = if (contains(element)) {
        getOffset(element)
    } else {
        null
    }

    private fun transformVertex(
        sourceVertexFormat: VertexFormat,
        sourceVertices: Int,
        sourceVertexBuffer: ByteBuffer,
        lightU: Short,
        lightV: Short,
        skinBuffer: RenderSkinBuffer?,
        targetBuffer: MorphTargetBuffer?,
        morphTargetData: RenderPrimitive.Targets?,
    ): ByteBuffer {
        val targetVertexFormat = RenderPipelines.ENTITY_TRANSLUCENT.vertexFormat
        val targetPositionOffset = targetVertexFormat.getOffset(VertexFormatElement.POSITION)
        val targetColorOffset = targetVertexFormat.getOffset(VertexFormatElement.COLOR)
        val targetTextureOffset = targetVertexFormat.getOffset(VertexFormatElement.UV0)
        val targetOverlayOffset = targetVertexFormat.getOffset(VertexFormatElement.UV1)
        val targetLightOffset = targetVertexFormat.getOffset(VertexFormatElement.UV2)
        val targetNormalOffset = targetVertexFormat.getOffset(VertexFormatElement.NORMAL)

        val sourcePositionOffset = sourceVertexFormat.getOffsetOrNull(VertexFormatElement.POSITION)
        val sourceColorOffset = sourceVertexFormat.getOffsetOrNull(VertexFormatElement.COLOR)
        val sourceTextureOffset = sourceVertexFormat.getOffsetOrNull(VertexFormatElement.UV0)
        val sourceNormalOffset = sourceVertexFormat.getOffsetOrNull(VertexFormatElement.NORMAL)
        val sourceJointOffset = sourceVertexFormat.getOffsetOrNull(BlazerodVertexFormatElements.JOINT)
        val sourceWeightOffset = sourceVertexFormat.getOffsetOrNull(BlazerodVertexFormatElements.WEIGHT)

        val positionTarget = targetBuffer?.positionChannel
        val colorTarget = targetBuffer?.colorChannel
        val texCoordTarget = targetBuffer?.texCoordChannel

        val transformedBuffer = cpuPool
            .allocate(sourceVertices * targetVertexFormat.vertexSize)
            .order(ByteOrder.nativeOrder())

        val processorCount = Runtime.getRuntime().availableProcessors()
        val taskCount = if (sourceVertices > processorCount && sourceVertices > 1000) {
            processorCount
        } else {
            1
        }
        val taskSize = sourceVertices / taskCount
        runBlocking {
            val tasks = (0 until taskCount).map { jobIndex ->
                val transformedBuffer = transformedBuffer.duplicate().order(ByteOrder.nativeOrder())
                async(Dispatchers.Default) {
                    val startVertex = jobIndex * taskSize
                    val endVertex = if (jobIndex == taskCount - 1) {
                        sourceVertices
                    } else {
                        (jobIndex + 1) * taskSize
                    }
                    val positionVector = Vector3f()
                    val normalVector = Vector3f(0f, 1f, 0f)
                    val jointPosition = Vector3f()
                    val skinnedPosition = Vector3f()
                    val skinMatrix = Matrix4f()
                    val colorVector = Vector4f()
                    val texCoordVector = Vector2f()
                    for (vertexIndex in startVertex until endVertex) {
                        val sourceOffset = vertexIndex * sourceVertexFormat.vertexSize
                        val targetOffset = vertexIndex * targetVertexFormat.vertexSize
                        sourcePositionOffset?.let {
                            positionVector.set(sourceOffset + sourcePositionOffset, sourceVertexBuffer)
                            if (positionTarget != null && morphTargetData != null) {
                                positionTarget.keySet().forEachInt { index ->
                                    val weight = positionTarget[index]
                                    val data = morphTargetData.position.cpuBuffer!!
                                    val baseOffset = (sourceVertices * index + vertexIndex) * 16
                                    positionVector.add(
                                        data.getFloat(baseOffset + 0) * weight,
                                        data.getFloat(baseOffset + 4) * weight,
                                        data.getFloat(baseOffset + 8) * weight,
                                    )
                                }
                            }
                            if (sourceJointOffset != null && sourceWeightOffset != null && skinBuffer != null) {
                                skinnedPosition.set(0f)
                                for (index in (0 until 4)) {
                                    val weight = sourceVertexBuffer.getFloat(sourceOffset + sourceWeightOffset + index * 4)
                                    if (weight < 1E-6) {
                                        continue
                                    }
                                    val joint = sourceVertexBuffer.getShort(sourceOffset + sourceJointOffset + index * 2).toUShort().toInt()
                                    if (joint in (0 until skinBuffer.jointSize)) {
                                        skinBuffer.getMatrix(joint, skinMatrix)
                                    } else {
                                        continue
                                    }
                                    positionVector.mulPosition(skinMatrix, jointPosition)
                                    jointPosition.mulAdd(weight, skinnedPosition, skinnedPosition)
                                }
                                positionVector.set(skinnedPosition)
                            }
                            positionVector.get(targetOffset + targetPositionOffset, transformedBuffer)
                        }
                        sourceColorOffset?.let {
                            if (colorTarget != null && morphTargetData != null) {
                                colorVector.set(
                                    sourceVertexBuffer.getUByteNormalized(sourceOffset + sourceColorOffset + 0),
                                    sourceVertexBuffer.getUByteNormalized(sourceOffset + sourceColorOffset + 1),
                                    sourceVertexBuffer.getUByteNormalized(sourceOffset + sourceColorOffset + 2),
                                    sourceVertexBuffer.getUByteNormalized(sourceOffset + sourceColorOffset + 3),
                                )
                                colorTarget.keySet().forEachInt { index ->
                                    val weight = colorTarget[index]
                                    val data = morphTargetData.color.cpuBuffer!!
                                    val baseOffset = (sourceVertices * index + vertexIndex) * 16
                                    colorVector.add(
                                        data.getFloat(baseOffset + 0) * weight,
                                        data.getFloat(baseOffset + 4) * weight,
                                        data.getFloat(baseOffset + 8) * weight,
                                        data.getFloat(baseOffset + 12) * weight,
                                    )
                                }
                                transformedBuffer.put(targetOffset + targetColorOffset + 0, colorVector.x().toNormalizedUByte())
                                transformedBuffer.put(targetOffset + targetColorOffset + 1, colorVector.y().toNormalizedUByte())
                                transformedBuffer.put(targetOffset + targetColorOffset + 2, colorVector.z().toNormalizedUByte())
                                transformedBuffer.put(targetOffset + targetColorOffset + 3, colorVector.w().toNormalizedUByte())
                            } else {
                                // 4 bytes
                                val colorBytes = sourceVertexBuffer.getInt(sourceOffset + sourceColorOffset)
                                transformedBuffer.putInt(targetOffset + targetColorOffset, colorBytes)
                            }
                        }
                        sourceTextureOffset?.let {
                            if (texCoordTarget != null && morphTargetData != null) {
                                texCoordVector.set(
                                    sourceVertexBuffer.getFloat(sourceOffset + sourceTextureOffset + 0),
                                    sourceVertexBuffer.getFloat(sourceOffset + sourceTextureOffset + 4),
                                )
                                texCoordTarget.keySet().forEachInt { index ->
                                    val weight = texCoordTarget[index]
                                    val data = morphTargetData.texCoord.cpuBuffer!!
                                    val baseOffset = (sourceVertices * index + vertexIndex) * 8
                                    texCoordVector.add(
                                        data.getFloat(baseOffset + 0) * weight,
                                        data.getFloat(baseOffset + 4) * weight,
                                    )
                                }
                                transformedBuffer.putFloat(targetOffset + targetTextureOffset + 0, texCoordVector.x())
                                transformedBuffer.putFloat(targetOffset + targetTextureOffset + 4, texCoordVector.y())
                            } else {
                                // 8 bytes
                                val textureBytes = sourceVertexBuffer.getLong(sourceOffset + sourceTextureOffset)
                                transformedBuffer.putLong(targetOffset + targetTextureOffset, textureBytes)
                            }
                        }
                        transformedBuffer.putShort(
                            targetOffset + targetOverlayOffset + 0,
                            (OverlayTexture.DEFAULT_UV and 0xFFFF).toShort()
                        )
                        transformedBuffer.putShort(
                            targetOffset + targetOverlayOffset + 2,
                            ((OverlayTexture.DEFAULT_UV shr 16) and 0xFFFF).toShort()
                        )
                        transformedBuffer.putShort(targetOffset + targetLightOffset + 0, lightU)
                        transformedBuffer.putShort(targetOffset + targetLightOffset + 2, lightV)
                        if (sourceNormalOffset != null) {
                            // 3 bytes
                            repeat(3) {
                                transformedBuffer.put(
                                    targetOffset + targetNormalOffset + it,
                                    sourceVertexBuffer.get(sourceOffset + sourceNormalOffset + it),
                                )
                            }
                        } else {
                            transformedBuffer.put(
                                targetOffset + targetNormalOffset + 0,
                                normalVector.x.toNormalizedSByte(),
                            )
                            transformedBuffer.put(
                                targetOffset + targetNormalOffset + 1,
                                normalVector.y.toNormalizedSByte(),
                            )
                            transformedBuffer.put(
                                targetOffset + targetNormalOffset + 2,
                                normalVector.z.toNormalizedSByte(),
                            )
                        }
                    }
                }
            }
            tasks.joinAll()
        }

        return transformedBuffer
    }

    private val modelMatrix = Matrix4f()
    override fun render(
        colorFrameBuffer: GpuTextureView,
        depthFrameBuffer: GpuTextureView?,
        scene: RenderScene,
        primitive: RenderPrimitive,
        primitiveIndex: Int,
        task: RenderTask,
        skinBuffer: RenderSkinBuffer?,
        targetBuffer: MorphTargetBuffer?,
    ) {
        if (!primitive.cpuComplete) {
            return
        }
        val instance = task.instance
        val material = primitive.material
        val convertedBuffer = transformVertex(
            sourceVertexFormat = material.vertexFormat,
            sourceVertices = primitive.vertices,
            sourceVertexBuffer = primitive.cpuVertexBuffer!!,
            lightU = (task.light and 0xFFFF).toShort(),
            lightV = ((task.light shr 16) and 0xFFFF).toShort(),
            skinBuffer = skinBuffer,
            targetBuffer = targetBuffer,
            morphTargetData = primitive.targets,
        )

        val device = RenderSystem.getDevice()
        val commandEncoder = device.createCommandEncoder()

        instance.modelData.modelMatricesBuffer.content.getMatrix(primitiveIndex, modelMatrix)
        modelMatrix.mulLocal(task.modelViewMatrix)
        val dynamicUniforms = RenderSystem.getDynamicUniforms().write(
            modelMatrix,
            Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
            RenderSystem.getModelOffset(),
            RenderSystem.getTextureMatrix(),
            RenderSystem.getShaderLineWidth()
        )

        val vertexBuffer = dataPool.upload(convertedBuffer)

        commandEncoder.createRenderPass(
            { "BlazeRod render pass" },
            colorFrameBuffer,
            OptionalInt.empty(),
            depthFrameBuffer,
            OptionalDouble.empty()
        ).use {
            with(it) {
                setPipeline(RenderPipelines.ENTITY_TRANSLUCENT)
                RenderSystem.bindDefaultUniforms(this)
                setUniform("DynamicTransforms", dynamicUniforms)
                bindSampler("Sampler2", MinecraftClient.getInstance().gameRenderer.lightmapTextureManager.glTextureView)
                bindSampler("Sampler1", MinecraftClient.getInstance().gameRenderer.overlayTexture.texture.glTextureView)
                (material as? RenderMaterial.Unlit)?.let { material ->
                    bindSampler("Sampler0", material.baseColorTexture.view)
                }

                setVertexFormatMode(primitive.vertexFormatMode)
                setVertexBuffer(0, vertexBuffer.buffer())
                primitive.indexBuffer?.let { indices ->
                    setIndexBuffer(indices)
                    drawIndexed(0, 0, indices.length, 1)
                } ?: run {
                    draw(0, primitive.vertices)
                }
            }
        }
    }

    override fun rotate() {
        dataPool.rotate()
        cpuPool.rotate()
    }

    override fun close() {
        dataPool.close()
    }
}