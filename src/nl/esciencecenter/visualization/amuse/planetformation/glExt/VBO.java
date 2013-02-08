package nl.esciencecenter.visualization.amuse.planetformation.glExt;

import java.nio.Buffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL3;

import nl.esciencecenter.visualization.openglCommon.datastructures.GLSLAttrib;

import com.jogamp.common.nio.Buffers;

public class VBO {
    private final IntBuffer vboPointer;
    private final IntBuffer bufferPointer;

    private GLSLAttrib[]    attribs;

    public VBO(GL3 gl, GLSLAttrib... attribs) {
        this.attribs = attribs;

        this.vboPointer = Buffers.newDirectIntBuffer(1);
        gl.glGenVertexArrays(1, this.vboPointer);
        gl.glBindVertexArray(this.vboPointer.get(0));

        this.bufferPointer = Buffers.newDirectIntBuffer(1);
        gl.glGenBuffers(1, this.bufferPointer);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, this.bufferPointer.get(0));

        int size = 0;
        for (final GLSLAttrib attrib : attribs) {
            size += attrib.buffer.capacity() * Buffers.SIZEOF_FLOAT;
        }

        gl.glBufferData(GL3.GL_ARRAY_BUFFER, size, (Buffer) null,
                GL3.GL_STATIC_DRAW);

        int nextStart = 0;
        for (final GLSLAttrib attrib : attribs) {
            gl.glBufferSubData(GL3.GL_ARRAY_BUFFER, nextStart,
                    attrib.buffer.capacity() * Buffers.SIZEOF_FLOAT,
                    attrib.buffer);
            nextStart += attrib.buffer.capacity() * Buffers.SIZEOF_FLOAT;
        }
    }

    public void bind(GL3 gl) {
        gl.glBindVertexArray(this.vboPointer.get(0));
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, bufferPointer.get(0));
    }

    public void delete(GL3 gl) {
        // gl.glBindVertexArray(0);
        gl.glDeleteVertexArrays(1, this.vboPointer);
        gl.glDeleteBuffers(1, this.bufferPointer);
    }

    public GLSLAttrib[] getAttribs() {
        return attribs;
    }

    public void update(GL3 gl, GLSLAttrib... attribs) {
        this.attribs = attribs;

        gl.glBindVertexArray(this.vboPointer.get(0));
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, this.bufferPointer.get(0));

        int size = 0;
        for (final GLSLAttrib attrib : attribs) {
            size += attrib.buffer.capacity() * Buffers.SIZEOF_FLOAT;
        }

        gl.glBufferData(GL3.GL_ARRAY_BUFFER, size, (Buffer) null,
                GL3.GL_STATIC_DRAW);

        int nextStart = 0;
        for (final GLSLAttrib attrib : attribs) {
            gl.glBufferSubData(GL3.GL_ARRAY_BUFFER, nextStart,
                    attrib.buffer.capacity() * Buffers.SIZEOF_FLOAT,
                    attrib.buffer);
            nextStart += attrib.buffer.capacity() * Buffers.SIZEOF_FLOAT;
        }
    }
}
