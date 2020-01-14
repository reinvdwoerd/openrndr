package org.openrndr.draw

import mu.KotlinLogging
import org.openrndr.internal.Driver

import java.util.*

private val logger = KotlinLogging.logger {}
private val sessionStack = mutableMapOf<Long, Stack<Session>>()

class SessionStatistics(val renderTargets: Int, val colorBuffers: Int, val bufferTextures: Int, val vertexBuffers: Int, val shaders: Int, val cubemaps: Int, val arrayTextures: Int)

class Session {
    val context = Driver.instance.contextID

    companion object {
        val active: Session
            get() = sessionStack.getOrPut(Driver.instance.contextID) { Stack<Session>().apply { push(Session()) } }.peek()

        val root: Session
            get() = sessionStack.getOrPut(Driver.instance.contextID) { Stack<Session>().apply { push(Session()) } }.first()
    }

    private val renderTargets = mutableSetOf<RenderTarget>()
    private val colorBuffers = mutableSetOf<ColorBuffer>()
    private val bufferTextures = mutableSetOf<BufferTexture>()
    private val vertexBuffers = mutableSetOf<VertexBuffer>()
    private val shaders = mutableSetOf<Shader>()
    private val computeShaders = mutableSetOf<ComputeShader>()
    private val cubemaps = mutableSetOf<Cubemap>()
    private val arrayTextures = mutableSetOf<ArrayTexture>()

    val statistics
        get() =
            SessionStatistics(renderTargets.size,
                    colorBuffers.size,
                    bufferTextures.size,
                    vertexBuffers.size,
                    shaders.size,
                    cubemaps.size,
                    arrayTextures.size)

    fun track(renderTarget: RenderTarget) = renderTargets.add(renderTarget)
    fun untrack(renderTarget: RenderTarget) = renderTargets.remove(renderTarget)

    fun track(colorBuffer: ColorBuffer) = colorBuffers.add(colorBuffer)
    fun untrack(colorBuffer: ColorBuffer) = colorBuffers.remove(colorBuffer)

    fun track(vertexBuffer: VertexBuffer) = vertexBuffers.add(vertexBuffer)
    fun untrack(vertexBuffer: VertexBuffer) = vertexBuffers.remove(vertexBuffer)

    fun track(shader: Shader) = shaders.add(shader)
    fun untrack(shader: Shader) = shaders.remove(shader)

    fun track(computeShader:ComputeShader) = computeShaders.add(computeShader)
    fun untrack(computeShader:ComputeShader) = computeShaders.remove(computeShader)

    fun track(cubemap: Cubemap) = cubemaps.add(cubemap)
    fun untrack(cubemap: Cubemap) = cubemaps.remove(cubemap)

    fun track(bufferTexture: BufferTexture) = bufferTextures.add(bufferTexture)
    fun untrack(bufferTexture: BufferTexture) = bufferTextures.remove(bufferTexture)

    fun track(arrayTexture: ArrayTexture) = arrayTextures.add(arrayTexture)
    fun untrack(arrayTexture: ArrayTexture) = arrayTextures.remove(arrayTexture)

    fun start() = sessionStack[Driver.instance.contextID]?.push(this)

    fun end() {
        sessionStack[Driver.instance.contextID]?.pop()

        logger.debug {
            """
                session ended for context [id=${context}]
                destroying ${renderTargets.size} render targets
                destroying ${colorBuffers.size} color buffers
                destroying ${vertexBuffers.size} vertex buffers
                destroying ${cubemaps.size} cubemaps
                destroying ${bufferTextures.size} buffer textures
                destroying ${arrayTextures.size} array textures
            """.trimIndent()
        }

        renderTargets.map { it }.forEach {
            it.detachColorBuffers()
            it.detachDepthBuffer()
            it.destroy()
        }
        renderTargets.clear()

        colorBuffers.map { it }.forEach {
            it.destroy()
        }
        colorBuffers.clear()

        vertexBuffers.map { it }.forEach {
            it.destroy()
        }
        vertexBuffers.clear()

        cubemaps.map { it }.forEach {
            it.destroy()
        }
        cubemaps.clear()

        bufferTextures.map { it }.forEach {
            it.destroy()
        }
        bufferTextures.clear()

        shaders.map { it }.forEach {
            it.destroy()
        }
        shaders.clear()

        arrayTextures.map { it }.forEach {
            it.destroy()
        }
        arrayTextures.clear()
    }
}