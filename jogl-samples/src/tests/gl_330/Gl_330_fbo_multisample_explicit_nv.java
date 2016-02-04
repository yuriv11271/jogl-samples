/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_330;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import static com.jogamp.opengl.GL2.GL_TEXTURE_RENDERBUFFER_NV;
import static com.jogamp.opengl.GL2GL3.*;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import core.glm;
import dev.Mat4;
import framework.Profile;
import framework.Semantic;
import framework.Test;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jgli.Texture2d;
import dev.Vec2;
import dev.Vec2i;
import dev.Vec3;

/**
 *
 * @author GBarbieri
 */
public class Gl_330_fbo_multisample_explicit_nv extends Test {

    public static void main(String[] args) {
        Gl_330_fbo_multisample_explicit_nv gl_330_fbo_multisample_explicit_nv = new Gl_330_fbo_multisample_explicit_nv();
    }

    public Gl_330_fbo_multisample_explicit_nv() {
        super("gl-330-fbo-multisample-explicit-nv", Profile.CORE, 3, 3, new Vec2((float) Math.PI * 0.2f));
    }

    private final String VERT_SHADER_SOURCE = "multisample-explicit-texture-nv";
    private final String[] FRAG_SHADER_SOURCE = {
        "multisample-explicit-texture-nv",
        "multisample-explicit-box-nv",
        "multisample-explicit-near-nv"};
    private final String SHADERS_ROOT = "src/data/gl_330";
    private final String TEXTURE_DIFFUSE = "kueken7_rgba8_srgb.dds";
    private Vec2i FRAMEBUFFER_SIZE = new Vec2i(160, 120);

    // With DDS textures, v texture coordinate are reversed, from top to bottom
    private int vertexCount = 6;
    private int vertexSize = vertexCount * 2 * 2 * Float.BYTES;
    private float[] vertexData = {
        -4.0f * 0.3f, -3.0f * 0.3f, 0.0f, 1.0f,
        +4.0f * 0.3f, -3.0f * 0.3f, 1.0f, 1.0f,
        +4.0f * 0.3f, +3.0f * 0.3f, 1.0f, 0.0f,
        +4.0f * 0.3f, +3.0f * 0.3f, 1.0f, 0.0f,
        -4.0f * 0.3f, +3.0f * 0.3f, 0.0f, 0.0f,
        -4.0f * 0.3f, -3.0f * 0.3f, 0.0f, 1.0f};

    private class Program {

        public static final int THROUGH = 0;
        public static final int RESOLVE_BOX = 1;
        public static final int RESOLVE_NEAR = 2;
        public static final int MAX = 3;
    }

    private class Renderbuffer {

        public static final int DEPTH = 0;
        public static final int COLOR = 1;
        public static final int MAX = 2;
    }

    private class Texture {

        public static final int DIFFUSE = 0;
        public static final int COLOR = 1;
        public static final int MAX = 2;
    }

    private class Shader {

        public static final int VERT = 0;
        public static final int FRAG_THROUGH = 1;
        public static final int FRAG_RESOLVE_BOX = 2;
        public static final int FRAG_RESOLVE_NEAR = 3;
        public static final int MAX = 4;
    }

    private int[] vertexArrayName = {0}, programName = new int[Program.MAX], bufferName = {0}, samplerName = {0},
            textureName = new int[Texture.MAX], renderbufferName = new int[Renderbuffer.MAX], framebufferName = {0},
            uniformMvp = new int[Program.MAX], uniformDiffuse = new int[Program.MAX];

    @Override
    protected boolean begin(GL gl) {

        GL3 gl3 = (GL3) gl;

        boolean validated = true;
        validated = validated && checkExtension(gl3, "GL_NV_explicit_multisample");

        if (validated) {
            validated = initProgram(gl3);
        }
        if (validated) {
            validated = initBuffer(gl3);
        }
        if (validated) {
            validated = initVertexArray(gl3);
        }
        if (validated) {
            validated = initSampler(gl3);
        }
        if (validated) {
            validated = initRenderbuffer(gl3);
        }
        if (validated) {
            validated = initTexture(gl3);
        }
        if (validated) {
            validated = initFramebuffer(gl3);
        }

        return validated && checkError(gl3, "begin");
    }

    private boolean initSampler(GL3 gl3) {

        gl3.glGenSamplers(1, samplerName, 0);
        gl3.glSamplerParameteri(samplerName[0], GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        gl3.glSamplerParameteri(samplerName[0], GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        gl3.glSamplerParameteri(samplerName[0], GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(samplerName[0], GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(samplerName[0], GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameterfv(samplerName[0], GL_TEXTURE_BORDER_COLOR, new float[]{0.0f, 0.0f, 0.0f, 0.0f}, 0);
        gl3.glSamplerParameterf(samplerName[0], GL_TEXTURE_MIN_LOD, -1000.f);
        gl3.glSamplerParameterf(samplerName[0], GL_TEXTURE_MAX_LOD, 1000.f);
        gl3.glSamplerParameterf(samplerName[0], GL_TEXTURE_LOD_BIAS, 0.0f);
        gl3.glSamplerParameteri(samplerName[0], GL_TEXTURE_COMPARE_MODE, GL_NONE);
        gl3.glSamplerParameteri(samplerName[0], GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);

        return checkError(gl3, "initSampler");
    }

    private boolean initProgram(GL3 gl3) {

        boolean validated = true;

        ShaderCode[] shaderCodes = new ShaderCode[Shader.MAX];

        shaderCodes[Shader.VERT] = ShaderCode.create(gl3, GL_VERTEX_SHADER,
                this.getClass(), SHADERS_ROOT, null, VERT_SHADER_SOURCE, "vert", null, true);
        for (int i = 0; i < Program.MAX; i++) {
            shaderCodes[Shader.FRAG_THROUGH + i] = ShaderCode.create(gl3, GL_FRAGMENT_SHADER,
                    this.getClass(), SHADERS_ROOT, null, FRAG_SHADER_SOURCE[i], "frag", null, true);
        }

        for (int i = 0; i < Program.MAX; i++) {

            ShaderProgram shaderProgram = new ShaderProgram();

            shaderProgram.add(shaderCodes[Shader.VERT]);
            shaderProgram.add(shaderCodes[Shader.FRAG_THROUGH + i]);

            shaderProgram.init(gl3);

            programName[i] = shaderProgram.program();

            shaderProgram.link(gl3, System.out);

            uniformMvp[i] = gl3.glGetUniformLocation(programName[i], "mvp");
            uniformDiffuse[i] = gl3.glGetUniformLocation(programName[i], "diffuse");
        }

        return validated & checkError(gl3, "initProgram");
    }

    private boolean initBuffer(GL3 gl3) {

        gl3.glGenBuffers(1, bufferName, 0);
        gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName[0]);
        gl3.glBufferData(GL_ARRAY_BUFFER, vertexSize, GLBuffers.newDirectFloatBuffer(vertexData), GL_STATIC_DRAW);
        gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

        return checkError(gl3, "initBuffer");
    }

    private boolean initTexture(GL3 gl3) {

        try {
            jgli.Texture2d texture = new Texture2d(jgli.Load.load(TEXTURE_ROOT + "/" + TEXTURE_DIFFUSE));
            jgli.Gl.Format format = jgli.Gl.translate(texture.format());

            gl3.glGenTextures(Texture.MAX, textureName, 0);
            gl3.glActiveTexture(GL_TEXTURE0);
            gl3.glBindTexture(GL_TEXTURE_2D, textureName[Texture.DIFFUSE]);
            for (int level = 0; level < texture.levels(); ++level) {
                gl3.glTexImage2D(GL_TEXTURE_2D, level,
                        format.internal.value,
                        texture.dimensions(level)[0], texture.dimensions(level)[1],
                        0,
                        format.external.value, format.type.value,
                        texture.data(level));
            }
            gl3.glBindTexture(GL_TEXTURE_2D, 0);

            gl3.glBindTexture(GL_TEXTURE_RENDERBUFFER_NV, textureName[Texture.COLOR]);
            ((GL2) gl3).glTexRenderbufferNV(GL_TEXTURE_RENDERBUFFER_NV, renderbufferName[Renderbuffer.COLOR]);
            gl3.glBindTexture(GL_TEXTURE_RENDERBUFFER_NV, 0);

        } catch (IOException ex) {
            Logger.getLogger(Gl_330_fbo_multisample_explicit_nv.class.getName()).log(Level.SEVERE, null, ex);
        }
        return checkError(gl3, "initTexture");
    }

    private boolean initRenderbuffer(GL3 gl3) {

        gl3.glGenRenderbuffers(Renderbuffer.MAX, renderbufferName, 0);
        gl3.glBindRenderbuffer(GL_RENDERBUFFER, renderbufferName[Renderbuffer.COLOR]);
        gl3.glRenderbufferStorageMultisample(GL_RENDERBUFFER, 4, GL_RGBA8, FRAMEBUFFER_SIZE.x, FRAMEBUFFER_SIZE.y);
        gl3.glBindRenderbuffer(GL_RENDERBUFFER, renderbufferName[Renderbuffer.DEPTH]);
        gl3.glRenderbufferStorageMultisample(GL_RENDERBUFFER, 4, GL_DEPTH_COMPONENT24, FRAMEBUFFER_SIZE.x, FRAMEBUFFER_SIZE.y);
        gl3.glBindRenderbuffer(GL_RENDERBUFFER, 0);

        return checkError(gl3, "initRenderbuffer");
    }

    private boolean initFramebuffer(GL3 gl3) {

        gl3.glGenFramebuffers(1, framebufferName, 0);
        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName[0]);
        gl3.glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER,
                renderbufferName[Renderbuffer.COLOR]);
        gl3.glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER,
                renderbufferName[Renderbuffer.DEPTH]);
        if (!isFramebufferComplete(gl3, framebufferName[0])) {
            return false;
        }
        gl3.glBindFramebuffer(GL_FRAMEBUFFER, 0);

        return checkError(gl3, "initFramebuffer");
    }

    private boolean initVertexArray(GL3 gl3) {

        gl3.glGenVertexArrays(1, vertexArrayName, 0);
        gl3.glBindVertexArray(vertexArrayName[0]);
        {
            gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName[0]);
            gl3.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, 2 * 2 * Float.BYTES, 0);
            gl3.glVertexAttribPointer(Semantic.Attr.TEXCOORD, 2, GL_FLOAT, false, 2 * 2 * Float.BYTES, 2 * Float.BYTES);
            gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

            gl3.glEnableVertexAttribArray(Semantic.Attr.POSITION);
            gl3.glEnableVertexAttribArray(Semantic.Attr.TEXCOORD);
        }
        gl3.glBindVertexArray(0);

        return checkError(gl3, "initVertexArray");
    }

    @Override
    protected boolean render(GL gl) {

        GL3 gl3 = (GL3) gl;

        // Clear the framebuffer
        gl3.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl3.glClearBufferfv(GL_COLOR, 0, new float[]{0.0f, 0.5f, 1.0f, 1.0f}, 0);

        // Pass 1
        // Render the scene in a multisampled framebuffer
        gl3.glEnable(GL_MULTISAMPLE);
        renderFBO(gl3);
        gl3.glDisable(GL_MULTISAMPLE);

        // Pass 2
        // Resolved and render the colorbuffer from the multisampled framebuffer
        resolveMultisampling(gl3);

        return true;
    }

    private void renderFBO(GL3 gl3) {

        Mat4 perspective = glm.perspective_((float) Math.PI * 0.25f, (float) FRAMEBUFFER_SIZE.x / FRAMEBUFFER_SIZE.y,
                0.1f, 100.0f);
        Mat4 model = new Mat4(1.0f).scale(new Vec3(1, -1, 1));
        Mat4 mvp = perspective.mul(viewMat4()).mul(model);

        gl3.glEnable(GL_DEPTH_TEST);

        gl3.glUseProgram(programName[Program.THROUGH]);
        gl3.glUniform1i(uniformDiffuse[Program.THROUGH], 0);
        gl3.glUniformMatrix4fv(uniformMvp[Program.THROUGH], 1, false, mvp.toFa_(), 0);

        gl3.glViewport(0, 0, FRAMEBUFFER_SIZE.x, FRAMEBUFFER_SIZE.y);

        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName[0]);
        float[] depth = {1.0f};
        gl3.glClearBufferfv(GL_DEPTH, 0, depth, 0);
        gl3.glClearBufferfv(GL_COLOR, 0, new float[]{1.0f, 0.5f, 0.0f, 1.0f}, 0);

        gl3.glActiveTexture(GL_TEXTURE0);
        gl3.glBindTexture(GL_TEXTURE_2D, textureName[Texture.DIFFUSE]);
        gl3.glBindSampler(0, samplerName[0]);
        gl3.glBindVertexArray(vertexArrayName[0]);

        gl3.glDrawArraysInstanced(GL_TRIANGLES, 0, vertexCount, 5);

        gl3.glDisable(GL_DEPTH_TEST);

        checkError(gl3, "renderFBO");
    }

    private void resolveMultisampling(GL3 gl3) {

        Mat4 perspective = glm.perspective_((float) Math.PI * 0.25f, (float) windowSize.x / windowSize.y, 0.1f, 100.0f);
        Mat4 model = new Mat4(1.0f);
        Mat4 mvp = perspective.mul(viewMat4()).mul(model);

        gl3.glViewport(0, 0, windowSize.x, windowSize.y);
        gl3.glBindFramebuffer(GL_FRAMEBUFFER, 0);

        gl3.glClearBufferfv(GL_COLOR, 0, new float[]{1.0f, 1.0f, 1.0f, 1.0f}, 0);

        gl3.glActiveTexture(GL_TEXTURE0);
        gl3.glBindTexture(GL_TEXTURE_RENDERBUFFER_NV, textureName[Texture.COLOR]);
        gl3.glBindSampler(0, samplerName[0]);

        gl3.glBindVertexArray(vertexArrayName[0]);

        gl3.glEnable(GL_SCISSOR_TEST);

        // Box
        {
            gl3.glScissor(1, 1, windowSize.x / 2 - 2, windowSize.y - 2);
            gl3.glUseProgram(programName[Program.RESOLVE_BOX]);
            gl3.glUniform1i(uniformDiffuse[Program.RESOLVE_BOX], 0);
            gl3.glUniformMatrix4fv(uniformMvp[Program.RESOLVE_BOX], 1, false, mvp.toFa_(), 0);
            gl3.glDrawArraysInstanced(GL_TRIANGLES, 0, vertexCount, 5);
        }

        // Near
        {
            gl3.glScissor(windowSize.x / 2 + 1, 1, windowSize.x / 2 - 2, windowSize.y - 2);
            gl3.glUseProgram(programName[Program.RESOLVE_NEAR]);
            gl3.glUniform1i(uniformDiffuse[Program.RESOLVE_NEAR], 0);
            gl3.glUniformMatrix4fv(uniformMvp[Program.RESOLVE_NEAR], 1, false, mvp.toFa_(), 0);
            gl3.glDrawArraysInstanced(GL_TRIANGLES, 0, vertexCount, 5);
        }

        gl3.glDisable(GL_SCISSOR_TEST);
    }

    @Override
    protected boolean end(GL gl) {

        GL3 gl3 = (GL3) gl;

        gl3.glDeleteBuffers(1, bufferName, 0);
        for (int i = 0; i < Program.MAX; ++i) {
            gl3.glDeleteProgram(programName[i]);
        }
        gl3.glDeleteSamplers(1, samplerName, 0);
        gl3.glDeleteTextures(Renderbuffer.MAX, renderbufferName, 0);
        gl3.glDeleteTextures(Texture.MAX, textureName, 0);
        gl3.glDeleteFramebuffers(1, framebufferName, 0);
        gl3.glDeleteVertexArrays(1, vertexArrayName, 0);

        return checkError(gl3, "end");
    }
}
