package com.quantum.engine.timeline

import com.quantum.engine.core.ecs.*
import com.quantum.engine.math.*
import kotlinx.coroutines.*

/**
 * TimelineSystem - Sistema de secuencias cinemáticas
 * 
 * Similar a Unity Timeline / Unreal Sequencer:
 * - Tracks de animación
 * - Control de cámara
 * - Audio y efectos
 * - Activación de eventos
 * - Interpolación automática
 * - Editable en tiempo real
 */
class TimelineSystem : System() {
    
    override val systemName = "TimelineSystem"
    override val requiredComponents = emptyList<ComponentType>()
    
    private val timelines = mutableMapOf<String, Timeline>()
    private val activeTimelines = mutableListOf<Timeline>()
    
    override fun onUpdate(entityManager: EntityManager, deltaTime: Float) {
        activeTimelines.forEach { timeline ->
            timeline.update(deltaTime, entityManager)
        }
    }
    
    fun createTimeline(name: String): Timeline {
        val timeline = Timeline(name)
        timelines[name] = timeline
        return timeline
    }
    
    fun play(name: String) {
        timelines[name]?.let { timeline ->
            timeline.play()
            if (timeline !in activeTimelines) {
                activeTimelines.add(timeline)
            }
        }
    }
    
    fun stop(name: String) {
        timelines[name]?.stop()
    }
}

/**
 * Timeline - Secuencia temporal
 */
class Timeline(val name: String) {
    
    var duration: Float = 10f
    var currentTime: Float = 0f
    var isPlaying: Boolean = false
    var loop: Boolean = false
    
    private val tracks = mutableListOf<Track>()
    
    fun addTrack(track: Track) {
        tracks.add(track)
    }
    
    fun play() {
        isPlaying = true
        currentTime = 0f
    }
    
    fun pause() {
        isPlaying = false
    }
    
    fun stop() {
        isPlaying = false
        currentTime = 0f
    }
    
    fun update(deltaTime: Float, entityManager: EntityManager) {
        if (!isPlaying) return
        
        currentTime += deltaTime
        
        if (currentTime >= duration) {
            if (loop) {
                currentTime = 0f
            } else {
                stop()
                return
            }
        }
        
        // Evaluar todos los tracks
        tracks.forEach { track ->
            track.evaluate(currentTime, entityManager)
        }
    }
}

/**
 * Track - Pista de animación
 */
abstract class Track(val name: String) {
    val clips = mutableListOf<TimelineClip>()
    
    abstract fun evaluate(time: Float, entityManager: EntityManager)
    
    fun addClip(clip: TimelineClip) {
        clips.add(clip)
        clips.sortBy { it.startTime }
    }
}

/**
 * AnimationTrack - Track de animación
 */
class AnimationTrack(
    name: String,
    val targetEntity: Entity
) : Track(name) {
    
    override fun evaluate(time: Float, entityManager: EntityManager) {
        clips.forEach { clip ->
            if (time >= clip.startTime && time <= clip.endTime) {
                val localTime = time - clip.startTime
                clip.evaluate(localTime, entityManager, targetEntity)
            }
        }
    }
}

/**
 * CameraTrack - Track de cámara
 */
class CameraTrack(
    name: String,
    val cameraEntity: Entity
) : Track(name) {
    
    private val positionKeys = mutableListOf<PositionKey>()
    private val rotationKeys = mutableListOf<RotationKey>()
    
    override fun evaluate(time: Float, entityManager: EntityManager) {
        // Interpolar posición
        val position = interpolatePosition(time)
        val rotation = interpolateRotation(time)
        
        // Aplicar a cámara
        val transform = entityManager.getComponent<com.quantum.engine.core.components.TransformComponent>(cameraEntity)
        transform?.let {
            it.localPosition = position
            it.localRotation = rotation
            it.isDirty = true
        }
    }
    
    fun addPositionKey(time: Float, position: Vector3) {
        positionKeys.add(PositionKey(time, position))
        positionKeys.sortBy { it.time }
    }
    
    fun addRotationKey(time: Float, rotation: Quaternion) {
        rotationKeys.add(RotationKey(time, rotation))
        rotationKeys.sortBy { it.time }
    }
    
    private fun interpolatePosition(time: Float): Vector3 {
        if (positionKeys.isEmpty()) return Vector3.ZERO
        if (positionKeys.size == 1) return positionKeys[0].value
        
        // Encontrar keys anterior y siguiente
        var prevKey = positionKeys[0]
        var nextKey = positionKeys.last()
        
        for (i in 0 until positionKeys.size - 1) {
            if (time >= positionKeys[i].time && time <= positionKeys[i + 1].time) {
                prevKey = positionKeys[i]
                nextKey = positionKeys[i + 1]
                break
            }
        }
        
        // Interpolar
        val t = (time - prevKey.time) / (nextKey.time - prevKey.time)
        return Vector3.lerp(prevKey.value, nextKey.value, t)
    }
    
    private fun interpolateRotation(time: Float): Quaternion {
        if (rotationKeys.isEmpty()) return Quaternion.IDENTITY
        if (rotationKeys.size == 1) return rotationKeys[0].value
        
        var prevKey = rotationKeys[0]
        var nextKey = rotationKeys.last()
        
        for (i in 0 until rotationKeys.size - 1) {
            if (time >= rotationKeys[i].time && time <= rotationKeys[i + 1].time) {
                prevKey = rotationKeys[i]
                nextKey = rotationKeys[i + 1]
                break
            }
        }
        
        val t = (time - prevKey.time) / (nextKey.time - prevKey.time)
        return Quaternion.slerp(prevKey.value, nextKey.value, t)
    }
}

/**
 * AudioTrack - Track de audio
 */
class AudioTrack(name: String) : Track(name) {
    
    override fun evaluate(time: Float, entityManager: EntityManager) {
        clips.forEach { clip ->
            if (time >= clip.startTime && time <= clip.endTime) {
                // TODO: Reproducir audio
            }
        }
    }
}

/**
 * EventTrack - Track de eventos
 */
class EventTrack(name: String) : Track(name) {
    
    private val events = mutableListOf<TimelineEvent>()
    private val firedEvents = mutableSetOf<TimelineEvent>()
    
    override fun evaluate(time: Float, entityManager: EntityManager) {
        events.forEach { event ->
            if (time >= event.time && event !in firedEvents) {
                event.callback()
                firedEvents.add(event)
            }
        }
    }
    
    fun addEvent(time: Float, callback: () -> Unit) {
        events.add(TimelineEvent(time, callback))
        events.sortBy { it.time }
    }
}

/**
 * VFX Track - Track de efectos visuales
 */
class VFXTrack(name: String) : Track(name) {
    
    override fun evaluate(time: Float, entityManager: EntityManager) {
        clips.forEach { clip ->
            if (time >= clip.startTime && time <= clip.endTime) {
                // TODO: Activar efectos visuales
            }
        }
    }
}

/**
 * TimelineClip - Clip temporal
 */
abstract class TimelineClip(
    val startTime: Float,
    val duration: Float
) {
    val endTime: Float get() = startTime + duration
    
    abstract fun evaluate(localTime: Float, entityManager: EntityManager, targetEntity: Entity)
}

/**
 * AnimationClip - Clip de animación
 */
class AnimationTimelineClip(
    startTime: Float,
    duration: Float,
    val animationData: AnimationData
) : TimelineClip(startTime, duration) {
    
    override fun evaluate(localTime: Float, entityManager: EntityManager, targetEntity: Entity) {
        val t = localTime / duration
        // TODO: Aplicar animación
    }
}

data class AnimationData(
    val positionCurve: AnimationCurve?,
    val rotationCurve: AnimationCurve?,
    val scaleCurve: AnimationCurve?
)

/**
 * AnimationCurve - Curva de animación tipo Unity
 */
class AnimationCurve {
    
    private val keyframes = mutableListOf<Keyframe>()
    
    fun addKey(time: Float, value: Float, inTangent: Float = 0f, outTangent: Float = 0f) {
        keyframes.add(Keyframe(time, value, inTangent, outTangent))
        keyframes.sortBy { it.time }
    }
    
    fun evaluate(time: Float): Float {
        if (keyframes.isEmpty()) return 0f
        if (keyframes.size == 1) return keyframes[0].value
        
        // Encontrar keyframes
        var prev = keyframes[0]
        var next = keyframes.last()
        
        for (i in 0 until keyframes.size - 1) {
            if (time >= keyframes[i].time && time <= keyframes[i + 1].time) {
                prev = keyframes[i]
                next = keyframes[i + 1]
                break
            }
        }
        
        // Interpolación hermite
        val t = (time - prev.time) / (next.time - prev.time)
        return hermiteInterpolate(prev, next, t)
    }
    
    private fun hermiteInterpolate(k0: Keyframe, k1: Keyframe, t: Float): Float {
        val t2 = t * t
        val t3 = t2 * t
        
        val h00 = 2 * t3 - 3 * t2 + 1
        val h10 = t3 - 2 * t2 + t
        val h01 = -2 * t3 + 3 * t2
        val h11 = t3 - t2
        
        val dt = k1.time - k0.time
        
        return h00 * k0.value + h10 * dt * k0.outTangent +
               h01 * k1.value + h11 * dt * k1.inTangent
    }
}

data class Keyframe(
    val time: Float,
    val value: Float,
    val inTangent: Float,
    val outTangent: Float
)

data class PositionKey(val time: Float, val value: Vector3)
data class RotationKey(val time: Float, val value: Quaternion)
data class ScaleKey(val time: Float, val value: Vector3)

data class TimelineEvent(
    val time: Float,
    val callback: () -> Unit
)

/**
 * Ejemplo de uso
 */
object TimelineExamples {
    
    fun createCinematicSequence(timelineSystem: TimelineSystem): Timeline {
        val timeline = timelineSystem.createTimeline("Opening Cinematic")
        timeline.duration = 30f
        
        // Track de cámara
        val cameraTrack = CameraTrack("Main Camera", Entity(1))
        cameraTrack.addPositionKey(0f, Vector3(0f, 5f, -10f))
        cameraTrack.addPositionKey(10f, Vector3(10f, 5f, 0f))
        cameraTrack.addPositionKey(20f, Vector3(0f, 10f, 10f))
        cameraTrack.addPositionKey(30f, Vector3(-10f, 5f, 0f))
        
        timeline.addTrack(cameraTrack)
        
        // Track de audio
        val audioTrack = AudioTrack("Music")
        timeline.addTrack(audioTrack)
        
        // Track de eventos
        val eventTrack = EventTrack("Events")
        eventTrack.addEvent(5f) {
            println("Event triggered at 5 seconds")
        }
        eventTrack.addEvent(15f) {
            println("Halfway through!")
        }
        timeline.addTrack(eventTrack)
        
        return timeline
    }
}
