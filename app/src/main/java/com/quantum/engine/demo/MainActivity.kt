package com.quantum.engine.demo

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import timber.log.Timber
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import com.quantum.engine.core.*
import com.quantum.engine.core.ecs.*
import com.quantum.engine.core.components.*
import com.quantum.engine.math.*
import com.quantum.engine.physics.*
import com.quantum.engine.renderer.*
import com.quantum.engine.renderer.gles.GLESRenderer
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : Activity() {
    
    private lateinit var glView: GLSurfaceView
    private lateinit var renderer: GameRenderer
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar Timber
        Timber.plant(Timber.DebugTree())
        
        // Crear GLSurfaceView
        glView = GLSurfaceView(this)
        glView.setEGLContextClientVersion(3) // OpenGL ES 3.0
        
        renderer = GameRenderer()
        glView.setRenderer(renderer)
        glView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        
        setContentView(glView)
        
        Timber.i("Quantum Engine Demo started")
    }
    
    override fun onResume() {
        super.onResume()
        glView.onResume()
        renderer.resume()
    }
    
    override fun onPause() {
        super.onPause()
        glView.onPause()
        renderer.pause()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        renderer.handleTouch(event)
        return true
    }
}

/**
 * GameRenderer - Renderizador principal del juego
 */
class GameRenderer : GLSurfaceView.Renderer {
    
    private lateinit var engine: QuantumEngine
    private lateinit var glesRenderer: GLESRenderer
    private lateinit var entityManager: EntityManager
    private lateinit var physicsSystem: PhysicsSystem
    
    // Entidades de la escena
    private var camera: Entity? = null
    private var cube: Entity? = null
    private var ground: Entity? = null
    private var sphere: Entity? = null
    
    // Meshes
    private val cubeMesh = Mesh.createCube(1f)
    private val sphereMesh = Mesh.createSphere(0.5f, 16)
    private val planeMesh = Mesh.createPlane(20f)
    
    // Tiempo
    private var lastTime = System.nanoTime()
    private var rotation = 0f
    
    // Touch
    private var touchX = 0f
    private var touchY = 0f
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Timber.i("Surface created")
        
        // Crear motor
        engine = QuantumEngine.builder()
            .config {
                fixedTimeStep = 1f / 60f
                targetFPS = 60
                enableMultiThreading = true
            }
            .build()
        
        engine.initialize()
        
        entityManager = engine.entityManager
        
        // Crear renderer
        glesRenderer = GLESRenderer()
        glesRenderer.initialize()
        
        // Registrar sistemas
        physicsSystem = PhysicsSystem()
        engine.systemManager.registerSystem(physicsSystem)
        engine.systemManager.registerSystem(TransformSystem())
        
        // Crear escena
        createScene()
        
        // Iniciar motor
        engine.start()
        
        // Callback de frame
        engine.onFrame = { frameInfo ->
            renderScene(frameInfo)
        }
        
        Timber.i("Engine initialized")
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Timber.i("Surface changed: ${width}x${height}")
        glesRenderer.setViewport(0, 0, width, height)
        
        // Actualizar aspect ratio de la cámara
        camera?.let { cameraEntity ->
            val cameraComponent = entityManager.getComponent<CameraComponent>(cameraEntity)
            cameraComponent?.aspect = width.toFloat() / height.toFloat()
        }
    }
    
    override fun onDrawFrame(gl: GL10?) {
        val currentTime = System.nanoTime()
        val deltaTime = (currentTime - lastTime) / 1_000_000_000f
        lastTime = currentTime
        
        // No hacemos nada aquí, el engine maneja todo
        // El rendering real se hace en renderScene()
    }
    
    private fun createScene() {
        // Cámara
        camera = entityManager.createEntity("MainCamera")
        val cameraTransform = TransformComponent(
            localPosition = Vector3(0f, 3f, 8f)
        )
        cameraTransform.lookAt(Vector3(0f, 0f, 0f))
        
        entityManager.addComponent(camera!!, cameraTransform)
        entityManager.addComponent(camera!!, CameraComponent(
            fieldOfView = 60f,
            nearClipPlane = 0.1f,
            farClipPlane = 100f
        ))
        
        // Suelo
        ground = entityManager.createEntity("Ground")
        entityManager.addComponent(ground!!, TransformComponent(
            localPosition = Vector3(0f, 0f, 0f)
        ))
        entityManager.addComponent(ground!!, MeshFilterComponent())
        entityManager.addComponent(ground!!, MeshRendererComponent())
        entityManager.addComponent(ground!!, BoxColliderComponent(
            size = Vector3(20f, 0.1f, 20f)
        ))
        
        // Cubo giratorio con física
        cube = entityManager.createEntity("Cube")
        entityManager.addComponent(cube!!, TransformComponent(
            localPosition = Vector3(0f, 3f, 0f)
        ))
        entityManager.addComponent(cube!!, MeshFilterComponent())
        entityManager.addComponent(cube!!, MeshRendererComponent())
        entityManager.addComponent(cube!!, RigidbodyComponent(
            mass = 1f,
            useGravity = true
        ))
        entityManager.addComponent(cube!!, BoxColliderComponent(
            size = Vector3.ONE
        ))
        
        // Esfera
        sphere = entityManager.createEntity("Sphere")
        entityManager.addComponent(sphere!!, TransformComponent(
            localPosition = Vector3(2f, 5f, 0f)
        ))
        entityManager.addComponent(sphere!!, MeshFilterComponent())
        entityManager.addComponent(sphere!!, MeshRendererComponent())
        entityManager.addComponent(sphere!!, RigidbodyComponent(
            mass = 0.5f,
            useGravity = true
        ))
        entityManager.addComponent(sphere!!, SphereColliderComponent(
            radius = 0.5f
        ))
        
        Timber.i("Scene created with ${entityManager.entityCount} entities")
    }
    
    private fun renderScene(frameInfo: FrameInfo) {
        // Limpiar pantalla
        glesRenderer.clear(Color(0.1f, 0.1f, 0.15f, 1f))
        
        glesRenderer.beginFrame()
        
        // Obtener cámara
        camera?.let { cameraEntity ->
            val cameraComponent = entityManager.getComponent<CameraComponent>(cameraEntity)!!
            val cameraTransform = entityManager.getComponent<TransformComponent>(cameraEntity)!!
            
            // Actualizar matrices de cámara
            cameraComponent.updateMatrices(cameraTransform)
            
            glesRenderer.setViewProjection(
                cameraComponent.viewMatrix,
                cameraComponent.projectionMatrix
            )
        }
        
        // Renderizar suelo
        ground?.let { entity ->
            val transform = entityManager.getComponent<TransformComponent>(entity)!!
            transform.updateMatrix()
            
            glesRenderer.submit(RenderCommand(
                mesh = planeMesh,
                material = Material(color = Color(0.3f, 0.3f, 0.3f, 1f)),
                transform = transform.localMatrix
            ))
        }
        
        // Renderizar cubo
        cube?.let { entity ->
            val transform = entityManager.getComponent<TransformComponent>(entity)!!
            transform.updateMatrix()
            
            // Rotar el cubo
            rotation += frameInfo.deltaTime * 50f
            transform.localRotation = Quaternion.fromEulerAngles(rotation, rotation * 0.7f, 0f)
            
            glesRenderer.submit(RenderCommand(
                mesh = cubeMesh,
                material = Material(color = Color(1f, 0.5f, 0.2f, 1f)),
                transform = transform.localMatrix
            ))
        }
        
        // Renderizar esfera
        sphere?.let { entity ->
            val transform = entityManager.getComponent<TransformComponent>(entity)!!
            transform.updateMatrix()
            
            glesRenderer.submit(RenderCommand(
                mesh = sphereMesh,
                material = Material(color = Color(0.2f, 0.7f, 1f, 1f)),
                transform = transform.localMatrix
            ))
        }
        
        glesRenderer.endFrame()
        
        // Stats
        if (frameInfo.frameNumber % 60 == 0L) {
            Timber.d("FPS: %.1f | DrawCalls: %d | Triangles: %d",
                frameInfo.fps,
                glesRenderer.stats.drawCalls,
                glesRenderer.stats.triangles
            )
        }
    }
    
    fun resume() {
        if (::engine.isInitialized) {
            engine.resume()
        }
    }
    
    fun pause() {
        if (::engine.isInitialized) {
            engine.pause()
        }
    }
    
    fun handleTouch(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                touchX = event.x
                touchY = event.y
                
                // Aplicar impulso al cubo
                cube?.let { entity ->
                    val rb = entityManager.getComponent<RigidbodyComponent>(entity)
                    rb?.addForce(Vector3(0f, 5f, 0f), ForceMode.IMPULSE)
                }
            }
        }
    }
}
