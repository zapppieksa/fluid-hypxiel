package dev.sxmurxy.mre.renderers.impl;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.sxmurxy.mre.providers.ColorProvider;
import dev.sxmurxy.mre.providers.ResourceProvider;
import dev.sxmurxy.mre.renderers.IRenderer;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;

public record BuiltText(
		MsdfFont font,
		String text,
		float size,
		float thickness,
		int color,
		float smoothness,
		float spacing,
		int outlineColor,
		float outlineThickness
) implements IRenderer {

	private static final ShaderProgramKey MSDF_FONT_SHADER_KEY = new ShaderProgramKey(ResourceProvider.getShaderIdentifier("msdf_font"),
			VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

	@Override
	public void render(Matrix4f matrix, float x, float y, float z) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();

		RenderSystem.setShaderTexture(0, this.font.getTextureId());

		boolean outlineEnabled = (this.outlineThickness > 0.0f);
		ShaderProgram shader = RenderSystem.setShader(MSDF_FONT_SHADER_KEY);
		shader.getUniform("Range").set(this.font.getAtlas().range());
		shader.getUniform("Thickness").set(this.thickness);
		shader.getUniform("Smoothness").set(this.smoothness);
		shader.getUniform("Outline").set(outlineEnabled ? 1 : 0);

		// Set TextColor uniform
		float[] colorComponents = ColorProvider.normalize(this.color);
		var textColorUniform = shader.getUniform("TextColor");
		if (textColorUniform != null) {
			textColorUniform.set(colorComponents[0], colorComponents[1], colorComponents[2], colorComponents[3]);
		} else {
			System.err.println("Warning: TextColor uniform not found in msdf_font shader");
		}

		if (outlineEnabled) {
			shader.getUniform("OutlineThickness").set(this.outlineThickness);
			float[] outlineComponents = ColorProvider.normalize(this.outlineColor);
			var outlineColorUniform = shader.getUniform("OutlineColor");
			if (outlineColorUniform != null) {
				outlineColorUniform.set(outlineComponents[0], outlineComponents[1],
						outlineComponents[2], outlineComponents[3]);
			} else {
				System.err.println("Warning: OutlineColor uniform not found in msdf_font shader");
			}
		}

		BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
		this.font.applyGlyphs(matrix, builder, this.text, this.size,
				(this.thickness + this.outlineThickness * 0.5f) * 0.5f * this.size, this.spacing,
				x, y + this.font.getMetrics().baselineHeight() * this.size, z, this.color);

		BufferRenderer.drawWithGlobalProgram(builder.end());

		RenderSystem.setShaderTexture(0, 0);

		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

}