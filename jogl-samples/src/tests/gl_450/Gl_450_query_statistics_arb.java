/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_450;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL2GL3.*;
import static com.jogamp.opengl.GL3ES3.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import glm.glm;
import glm.mat._4.Mat4;
import glm.vec._2.Vec2;
import framework.BufferUtils;
import framework.Profile;
import framework.Semantic;
import framework.Test;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jgli.Texture2d;

/**
 *
 * @author GBarbieri
 */
public class Gl_450_query_statistics_arb extends Test {

    public static void main(String[] args) {
        Gl_450_query_statistics_arb gl_450_query_statistics_arb = new Gl_450_query_statistics_arb();
    }

    public Gl_450_query_statistics_arb() {
        super("gl-450-query-statistics-arb", Profile.CORE, 4, 5);
    }

    private final String SHADERS_SOURCE = "query-statistics";
    private final String SHADERS_ROOT = "src/data/gl_450";
    private final String TEXTURE_DIFFUSE = "kueken7_rgba8_srgb.dds";

    private int vertexCount = 4;
    private int vertexSize = vertexCount * glf.Vertex_v2fv2f.SIZE;
    private float[] vertexData = {
        -1.0f, -1.0f,/**/ 0.0f, 1.0f,
        +1.0f, -1.0f,/**/ 1.0f, 1.0f,
        +1.0f, +1.0f,/**/ 1.0f, 0.0f,
        -1.0f, +1.0f,/**/ 0.0f, 0.0f};

    private int elementCount = 6;
    private int elementSize = elementCount * Short.BYTES;
    private short[] elementData = {
        0, 1, 2,
        2, 3, 0};

    private class Program {

        public static final int VERTEX = 0;
        public static final int FRAGMENT = 1;
        public static final int MAX = 2;
    }

    private class Buffer {

        public static final int VERTEX = 0;
        public static final int ELEMENT = 1;
        public static final int TRANSFORM = 2;
        public static final int MAX = 3;
    }

    private class Statistics {

        public static final int VERTICES_SUBMITTED = 0;
        public static final int PRIMITIVES_SUBMITTED = 1;
        public static final int VERTEX_SHADER_INVOCATIONS = 2;
        public static final int TESS_CONTROL_SHADER_PATCHES = 3;
        public static final int TESS_EVALUATION_SHADER_INVOCATIONS = 4;
        public static final int GEOMETRY_SHADER_INVOCATIONS = 5;
        public static final int GEOMETRY_SHADER_PRIMITIVES_EMITTED = 6;
        public static final int FRAGMENT_SHADER_INVOCATIONS = 7;
        public static final int COMPUTE_SHADER_INVOCATIONS = 8;
        public static final int CLIPPING_INPUT_PRIMITIVES = 9;
        public static final int CLIPPING_OUTPUT_PRIMITIVES = 10;
        public static final int MAX = 11;
    }

    private IntBuffer pipelineName = GLBuffers.newDirectIntBuffer(1), vertexArrayName = GLBuffers.newDirectIntBuffer(1),
            textureName = GLBuffers.newDirectIntBuffer(1), bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX),
            queryName = GLBuffers.newDirectIntBuffer(Statistics.MAX);
    private int[] programName = new int[Program.MAX];

    @Override
    protected boolean begin(GL gl) {

        GL4 gl4 = (GL4) gl;

        boolean validated = gl4.isExtensionAvailable("GL_ARB_pipeline_statistics_query");

        if (validated) {
            validated = initQuery(gl4);
        }
        if (validated) {
            validated = initProgram(gl4);
        }
        if (validated) {
            validated = initBuffer(gl4);
        }
        if (validated) {
            validated = initVertexArray(gl4);
        }
        if (validated) {
            validated = initTexture(gl4);
        }

        return validated;
    }

    private boolean initProgram(GL4 gl4) {

        boolean validated = true;

        if (validated) {

            ShaderCode vertShaderCode = ShaderCode.create(gl4, GL_VERTEX_SHADER,
                    this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE, "vert", null, true);
            ShaderCode fragShaderCode = ShaderCode.create(gl4, GL_FRAGMENT_SHADER,
                    this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE, "frag", null, true);

            ShaderProgram shaderProgram = new ShaderProgram();
            shaderProgram.init(gl4);

            programName[Program.VERTEX] = shaderProgram.program();

            gl4.glProgramParameteri(programName[Program.VERTEX], GL_PROGRAM_SEPARABLE, GL_TRUE);

            shaderProgram.add(vertShaderCode);
            shaderProgram.link(gl4, System.out);

            shaderProgram = new ShaderProgram();
            shaderProgram.init(gl4);

            programName[Program.FRAGMENT] = shaderProgram.program();

            gl4.glProgramParameteri(programName[Program.FRAGMENT], GL_PROGRAM_SEPARABLE, GL_TRUE);

            shaderProgram.add(fragShaderCode);
            shaderProgram.link(gl4, System.out);
        }

        if (validated) {

            gl4.glGenProgramPipelines(1, pipelineName);
            gl4.glUseProgramStages(pipelineName.get(0), GL_VERTEX_SHADER_BIT, programName[Program.VERTEX]);
            gl4.glUseProgramStages(pipelineName.get(0), GL_FRAGMENT_SHADER_BIT, programName[Program.FRAGMENT]);
        }

        return validated & checkError(gl4, "initProgram");
    }

    private boolean initBuffer(GL4 gl4) {

        boolean validated = true;

        gl4.glGenBuffers(Buffer.MAX, bufferName);

        gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        ShortBuffer elementBuffer = GLBuffers.newDirectShortBuffer(elementData);
        gl4.glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementSize, elementBuffer, GL_STATIC_DRAW);
        BufferUtils.destroyDirectBuffer(elementBuffer);
        gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);
        gl4.glBufferData(GL_ARRAY_BUFFER, vertexSize, vertexBuffer, GL_STATIC_DRAW);
        BufferUtils.destroyDirectBuffer(vertexBuffer);
        gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);

        int[] uniformBufferOffset = {0};
        gl4.glGetIntegerv(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT, uniformBufferOffset, 0);
        int uniformBlockSize = Math.max(Mat4.SIZE, uniformBufferOffset[0]);

        gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
        gl4.glBufferData(GL_UNIFORM_BUFFER, uniformBlockSize, null, GL_DYNAMIC_DRAW);
        gl4.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        return validated;
    }

    private boolean initTexture(GL4 gl4) {

        boolean validated = true;

        try {

            jgli.Texture2d texture = new Texture2d(jgli.Load.load(TEXTURE_ROOT + "/" + TEXTURE_DIFFUSE));
            jgli.Gl.Format format = jgli.Gl.translate(texture.format());

            gl4.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

            gl4.glGenTextures(1, textureName);
            gl4.glActiveTexture(GL_TEXTURE0);
            gl4.glBindTexture(GL_TEXTURE_2D_ARRAY, textureName.get(0));
            gl4.glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_SWIZZLE_R, GL_RED);
            gl4.glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_SWIZZLE_G, GL_GREEN);
            gl4.glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_SWIZZLE_B, GL_BLUE);
            gl4.glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_SWIZZLE_A, GL_ALPHA);
            gl4.glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_BASE_LEVEL, 0);
            gl4.glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAX_LEVEL, texture.levels() - 1);
            gl4.glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            gl4.glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            gl4.glTexStorage3D(GL_TEXTURE_2D_ARRAY, texture.levels(),
                    format.internal.value,
                    texture.dimensions(0)[0], texture.dimensions(0)[1], 1);

            for (int level = 0; level < texture.levels(); ++level) {
                gl4.glTexSubImage3D(GL_TEXTURE_2D_ARRAY, level,
                        0, 0, 0,
                        texture.dimensions(level)[0], texture.dimensions(level)[1], 1,
                        format.external.value, format.type.value,
                        texture.data(level));
            }

            gl4.glPixelStorei(GL_UNPACK_ALIGNMENT, 4);

        } catch (IOException ex) {
            Logger.getLogger(Gl_450_query_statistics_arb.class.getName()).log(Level.SEVERE, null, ex);
        }
        return validated;
    }

    private boolean initVertexArray(GL4 gl4) {

        boolean validated = true;

        gl4.glGenVertexArrays(1, vertexArrayName);
        gl4.glBindVertexArray(vertexArrayName.get(0));
        {
            gl4.glVertexAttribBinding(Semantic.Attr.POSITION, Semantic.Buffer.STATIC);
            gl4.glVertexAttribFormat(Semantic.Attr.POSITION, 2, GL_FLOAT, false, 0);

            gl4.glVertexAttribBinding(Semantic.Attr.TEXCOORD, Semantic.Buffer.STATIC);
            gl4.glVertexAttribFormat(Semantic.Attr.TEXCOORD, 2, GL_FLOAT, false, Vec2.SIZE);

            gl4.glEnableVertexAttribArray(Semantic.Attr.POSITION);
            gl4.glEnableVertexAttribArray(Semantic.Attr.TEXCOORD);

            gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
            gl4.glBindVertexBuffer(0, bufferName.get(Buffer.VERTEX), 0, glf.Vertex_v2fv2f.SIZE);
        }
        gl4.glBindVertexArray(0);

        return validated;
    }

    private boolean initQuery(GL4 gl4) {

        gl4.glGenQueries(Statistics.MAX, queryName);

        int[] queryCounterBits = new int[Statistics.MAX];

        gl4.glGetQueryiv(GL_VERTICES_SUBMITTED_ARB, GL_QUERY_COUNTER_BITS, queryCounterBits,
                Statistics.VERTICES_SUBMITTED);
        gl4.glGetQueryiv(GL_PRIMITIVES_SUBMITTED_ARB, GL_QUERY_COUNTER_BITS, queryCounterBits,
                Statistics.PRIMITIVES_SUBMITTED);
        gl4.glGetQueryiv(GL_VERTEX_SHADER_INVOCATIONS_ARB, GL_QUERY_COUNTER_BITS, queryCounterBits,
                Statistics.VERTEX_SHADER_INVOCATIONS);
        gl4.glGetQueryiv(GL_TESS_CONTROL_SHADER_PATCHES_ARB, GL_QUERY_COUNTER_BITS, queryCounterBits,
                Statistics.TESS_CONTROL_SHADER_PATCHES);
        gl4.glGetQueryiv(GL_TESS_EVALUATION_SHADER_INVOCATIONS_ARB, GL_QUERY_COUNTER_BITS, queryCounterBits,
                Statistics.TESS_EVALUATION_SHADER_INVOCATIONS);
        gl4.glGetQueryiv(GL_GEOMETRY_SHADER_INVOCATIONS, GL_QUERY_COUNTER_BITS, queryCounterBits,
                Statistics.GEOMETRY_SHADER_INVOCATIONS);
        gl4.glGetQueryiv(GL_GEOMETRY_SHADER_PRIMITIVES_EMITTED_ARB, GL_QUERY_COUNTER_BITS, queryCounterBits,
                Statistics.GEOMETRY_SHADER_PRIMITIVES_EMITTED);
        gl4.glGetQueryiv(GL_FRAGMENT_SHADER_INVOCATIONS_ARB, GL_QUERY_COUNTER_BITS, queryCounterBits,
                Statistics.FRAGMENT_SHADER_INVOCATIONS);
        gl4.glGetQueryiv(GL_COMPUTE_SHADER_INVOCATIONS_ARB, GL_QUERY_COUNTER_BITS, queryCounterBits,
                Statistics.COMPUTE_SHADER_INVOCATIONS);
        gl4.glGetQueryiv(GL_CLIPPING_INPUT_PRIMITIVES_ARB, GL_QUERY_COUNTER_BITS, queryCounterBits,
                Statistics.CLIPPING_INPUT_PRIMITIVES);
        gl4.glGetQueryiv(GL_CLIPPING_OUTPUT_PRIMITIVES_ARB, GL_QUERY_COUNTER_BITS, queryCounterBits,
                Statistics.CLIPPING_OUTPUT_PRIMITIVES);

        boolean validated = true;
        for (int i = 0; i < queryCounterBits.length; ++i) {
            validated = validated && queryCounterBits[i] >= 18;
        }

        return validated;
    }

    @Override
    protected boolean render(GL gl) {

        GL4 gl4 = (GL4) gl;

        {
            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
            ByteBuffer pointer = gl4.glMapBufferRange(GL_UNIFORM_BUFFER, 0, Mat4.SIZE,
                    GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

            Mat4 projection = glm.perspectiveFov_((float) Math.PI * 0.25f, windowSize.x, windowSize.y, 0.1f, 100.0f);
            Mat4 model = new Mat4(1.0f);
            pointer.asFloatBuffer().put(projection.mul(viewMat4()).mul(model).toFa_());

            // Make sure the uniform buffer is uploaded
            gl4.glUnmapBuffer(GL_UNIFORM_BUFFER);
        }

        gl4.glViewportIndexedf(0, 0, 0, windowSize.x, windowSize.y);
        gl4.glClearBufferfv(GL_COLOR, 0, new float[]{1.0f, 1.0f, 1.0f, 1.0f}, 0);

        gl4.glBindProgramPipeline(pipelineName.get(0));
        gl4.glActiveTexture(GL_TEXTURE0);
        gl4.glBindTexture(GL_TEXTURE_2D_ARRAY, textureName.get(0));
        gl4.glBindVertexArray(vertexArrayName.get(0));
        gl4.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM0, bufferName.get(Buffer.TRANSFORM));

        gl4.glBeginQuery(GL_VERTICES_SUBMITTED_ARB, queryName.get(Statistics.VERTICES_SUBMITTED));
        gl4.glBeginQuery(GL_PRIMITIVES_SUBMITTED_ARB, queryName.get(Statistics.PRIMITIVES_SUBMITTED));
        gl4.glBeginQuery(GL_VERTEX_SHADER_INVOCATIONS_ARB, queryName.get(Statistics.VERTEX_SHADER_INVOCATIONS));
        gl4.glBeginQuery(GL_TESS_CONTROL_SHADER_PATCHES_ARB, queryName.get(Statistics.TESS_CONTROL_SHADER_PATCHES));
        gl4.glBeginQuery(GL_TESS_EVALUATION_SHADER_INVOCATIONS_ARB,
                queryName.get(Statistics.TESS_EVALUATION_SHADER_INVOCATIONS));
        gl4.glBeginQuery(GL_GEOMETRY_SHADER_INVOCATIONS, queryName.get(Statistics.GEOMETRY_SHADER_INVOCATIONS));
        gl4.glBeginQuery(GL_GEOMETRY_SHADER_PRIMITIVES_EMITTED_ARB,
                queryName.get(Statistics.GEOMETRY_SHADER_PRIMITIVES_EMITTED));
        gl4.glBeginQuery(GL_FRAGMENT_SHADER_INVOCATIONS_ARB, queryName.get(Statistics.FRAGMENT_SHADER_INVOCATIONS));
        gl4.glBeginQuery(GL_COMPUTE_SHADER_INVOCATIONS_ARB, queryName.get(Statistics.COMPUTE_SHADER_INVOCATIONS));
        gl4.glBeginQuery(GL_CLIPPING_INPUT_PRIMITIVES_ARB, queryName.get(Statistics.CLIPPING_INPUT_PRIMITIVES));
        gl4.glBeginQuery(GL_CLIPPING_OUTPUT_PRIMITIVES_ARB, queryName.get(Statistics.CLIPPING_OUTPUT_PRIMITIVES));
        {
            gl4.glDrawElementsInstancedBaseVertexBaseInstance(GL_TRIANGLES, elementCount, GL_UNSIGNED_SHORT, 0, 1, 0, 0);
        }
        gl4.glEndQuery(GL_VERTICES_SUBMITTED_ARB);
        gl4.glEndQuery(GL_PRIMITIVES_SUBMITTED_ARB);
        gl4.glEndQuery(GL_VERTEX_SHADER_INVOCATIONS_ARB);
        gl4.glEndQuery(GL_TESS_CONTROL_SHADER_PATCHES_ARB);
        gl4.glEndQuery(GL_TESS_EVALUATION_SHADER_INVOCATIONS_ARB);
        gl4.glEndQuery(GL_GEOMETRY_SHADER_INVOCATIONS);
        gl4.glEndQuery(GL_GEOMETRY_SHADER_PRIMITIVES_EMITTED_ARB);
        gl4.glEndQuery(GL_FRAGMENT_SHADER_INVOCATIONS_ARB);
        gl4.glEndQuery(GL_COMPUTE_SHADER_INVOCATIONS_ARB);
        gl4.glEndQuery(GL_CLIPPING_INPUT_PRIMITIVES_ARB);
        gl4.glEndQuery(GL_CLIPPING_OUTPUT_PRIMITIVES_ARB);

        IntBuffer queryResult = GLBuffers.newDirectIntBuffer(Statistics.MAX);
        for (int i = 0; i < queryResult.capacity(); ++i) {
            gl4.glGetQueryObjectuiv(queryName.get(i), GL_QUERY_RESULT, queryResult);
        }
        System.out.println("Verts: " + queryResult.get(Statistics.VERTICES_SUBMITTED)
                + "; Prims: (" + queryResult.get(Statistics.PRIMITIVES_SUBMITTED) + ", "
                + queryResult.get(Statistics.GEOMETRY_SHADER_PRIMITIVES_EMITTED)
                + "); Shaders(" + queryResult.get(Statistics.VERTEX_SHADER_INVOCATIONS) + ", "
                + queryResult.get(Statistics.TESS_CONTROL_SHADER_PATCHES) + ", "
                + queryResult.get(Statistics.TESS_EVALUATION_SHADER_INVOCATIONS) + ", "
                + queryResult.get(Statistics.GEOMETRY_SHADER_INVOCATIONS) + ", "
                + queryResult.get(Statistics.FRAGMENT_SHADER_INVOCATIONS) + ", "
                + queryResult.get(Statistics.COMPUTE_SHADER_INVOCATIONS)
                + "); Clip(" + queryResult.get(Statistics.CLIPPING_INPUT_PRIMITIVES) + ", "
                + queryResult.get(Statistics.CLIPPING_OUTPUT_PRIMITIVES) + ")\r");

        return true;
    }

    @Override
    protected boolean end(GL gl) {

        GL4 gl4 = (GL4) gl;

        gl4.glDeleteProgramPipelines(1, pipelineName);
        BufferUtils.destroyDirectBuffer(pipelineName);
        gl4.glDeleteProgram(programName[Program.FRAGMENT]);
        gl4.glDeleteProgram(programName[Program.VERTEX]);
        gl4.glDeleteBuffers(Buffer.MAX, bufferName);
        BufferUtils.destroyDirectBuffer(bufferName);
        gl4.glDeleteTextures(1, textureName);
        BufferUtils.destroyDirectBuffer(textureName);
        gl4.glDeleteVertexArrays(1, vertexArrayName);
        BufferUtils.destroyDirectBuffer(vertexArrayName);

        return true;
    }
}
