package top.fifthlight.blazerod.gl;

import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import net.minecraft.client.gl.ShaderProgram;
import top.fifthlight.blazerod.pipeline.ComputePipeline;

public record CompiledComputePipeline(ComputePipeline info, ShaderProgram program) implements CompiledRenderPipeline {
    @Override
    public boolean isValid() {
        return this.program != ShaderProgram.INVALID;
    }
}
