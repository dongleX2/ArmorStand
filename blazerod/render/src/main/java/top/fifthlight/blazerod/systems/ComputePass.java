package top.fifthlight.blazerod.systems;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.jetbrains.annotations.NotNull;
import top.fifthlight.blazerod.pipeline.ComputePipeline;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public interface ComputePass extends AutoCloseable {
    void pushDebugGroup(@NotNull Supplier<String> label);

    void popDebugGroup();

    void setPipeline(@NotNull ComputePipeline pipeline);

    void bindSampler(@NotNull String name, @Nullable GpuTextureView view);

    void setUniform(@NotNull String name, @NotNull GpuBuffer buffer);

    void setUniform(@NotNull String name, @NotNull GpuBufferSlice slice);

    void setStorageBuffer(@NotNull String name, @NotNull GpuBufferSlice buffer);

    void dispatch(int x, int y, int z);

    @Override
    void close();
}