(ns mylwjgl.core
  (:import java.nio.ByteBuffer)
  (:import java.nio.IntBuffer)
  (:import org.lwjgl.BufferUtils)
  (:import org.lwjgl.openal.AL10)
  (:import org.lwjgl.openal.ALDevice)
  (:import org.lwjgl.openal.ALContext)
  (:import org.lwjgl.openal.ALCapabilities)
  (:import org.lwjgl.openal.EXTFloat32)
  (:import org.lwjgl.glfw.GLFW)
  (:import org.lwjgl.glfw.GLFWWindowCloseCallback)
  (:import org.lwjgl.glfw.GLFWWindowSizeCallback)
  (:import org.lwjgl.glfw.GLFWWindowRefreshCallback)
  (:import org.lwjgl.glfw.GLFWMouseButtonCallback)
  (:import org.lwjgl.glfw.GLFWCursorPosCallback)
  (:import org.lwjgl.opengl.GLContext)
  (:import org.lwjgl.opengl.GL11)
  (:import org.lwjgl.opengl.GL12)
  (:import org.lwjgl.opengl.GL13)
  (:import org.lwjgl.opengl.GL15)
  (:import org.lwjgl.opengl.GL20)
  (:import org.lwjgl.opengl.GL21)
  (:import org.lwjgl.opengl.GL30)
)

(def SAMPLERATE 44100)
(def BUFSIZE 8192)
(def SMPSIZE 2048)

(defn createshader
  [source type]
  (let [shader (GL20/glCreateShader type)]
    (GL20/glShaderSource shader source )
    (GL20/glCompileShader shader)
    (println (GL20/glGetShaderInfoLog shader))
    shader))

(defn varpars
  [program aflex adamp bflex bdamp]
  (GL20/glUseProgram program)
  (GL20/glUniform1f (GL20/glGetUniformLocation program "aflex") aflex)
  (GL20/glUniform1f (GL20/glGetUniformLocation program "adamp") adamp)
  (GL20/glUniform1f (GL20/glGetUniformLocation program "bflex") bflex)
  (GL20/glUniform1f (GL20/glGetUniformLocation program "bdamp") bdamp)
  (GL20/glUseProgram 0)
  )

(defn createprogram
  [vsh fsh w h]
  (let [program (GL20/glCreateProgram)]
    (GL20/glAttachShader program vsh)
    (GL20/glAttachShader program fsh)
    (GL20/glLinkProgram program)
    (GL20/glUseProgram program)
    (GL20/glUniform1i (GL20/glGetUniformLocation program "tex") 0) 
    (GL20/glUniform1f (GL20/glGetUniformLocation program "du") (/ 1.0 w))
    (GL20/glUniform1f (GL20/glGetUniformLocation program "dv") (/ 1.0 h))
    (GL20/glUseProgram 0)
    (varpars program 0.51 0.995 0.5 0.995)
    program))

(defn createtexture
  [w h]
  (let [texture (GL11/glGenTextures)]
    (GL11/glBindTexture GL11/GL_TEXTURE_2D texture)
    (GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_MIN_FILTER GL11/GL_NEAREST)
    (GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_MAG_FILTER GL11/GL_NEAREST)
   ; IMPORTANT: texture must be COMPLETE (mipmaps must be specified..
   ; or the following two parameters can be used if there are none)
   (GL11/glTexParameteri GL11/GL_TEXTURE_2D GL12/GL_TEXTURE_BASE_LEVEL 0)
   (GL11/glTexParameteri GL11/GL_TEXTURE_2D GL12/GL_TEXTURE_MAX_LEVEL 0)
   (GL11/glTexImage2D GL11/GL_TEXTURE_2D 0 GL11/GL_RGBA w h 0
                       GL11/GL_RGBA GL11/GL_FLOAT 0)
    (println (str "glTexImage2D " (GL11/glGetError)))
    texture))

(defn createpb
  []
  (let [pb (GL15/glGenBuffers)]
    (GL15/glBindBuffer GL21/GL_PIXEL_PACK_BUFFER pb)
    (GL15/glBufferData GL21/GL_PIXEL_PACK_BUFFER BUFSIZE GL15/GL_DYNAMIC_READ)
    (println (str "glBufferData " (GL11/glGetError)))
    (GL15/glBindBuffer GL21/GL_PIXEL_PACK_BUFFER 0)
    pb))

(defn createfb
  [tex1 tex2 texs]
  (let [fb (GL30/glGenFramebuffers)]
    (GL30/glBindFramebuffer GL30/GL_FRAMEBUFFER fb)
    (GL30/glFramebufferTexture2D GL30/GL_FRAMEBUFFER GL30/GL_COLOR_ATTACHMENT0 GL11/GL_TEXTURE_2D tex1 0)
    (GL30/glFramebufferTexture2D GL30/GL_FRAMEBUFFER GL30/GL_COLOR_ATTACHMENT1 GL11/GL_TEXTURE_2D tex2 0)
    (GL30/glFramebufferTexture2D GL30/GL_FRAMEBUFFER GL30/GL_COLOR_ATTACHMENT2 GL11/GL_TEXTURE_2D texs 0)
    (println (str "glFramebufferTexture2D " (GL11/glGetError)))
    (println (str "glCheckFramebufferStatus " (GL30/glCheckFramebufferStatus GL30/GL_FRAMEBUFFER)))
    (GL11/glClearColor 0.5 0.5 0.5 1.0)
    (GL20/glDrawBuffers GL30/GL_COLOR_ATTACHMENT0)
    (GL11/glClear GL11/GL_COLOR_BUFFER_BIT)
    (GL20/glDrawBuffers GL30/GL_COLOR_ATTACHMENT1)
    (GL11/glClear GL11/GL_COLOR_BUFFER_BIT)
    (GL11/glClearColor 0.25 0.0 0.0 0.0)
    (GL20/glDrawBuffers GL30/GL_COLOR_ATTACHMENT2)
    (GL11/glClear GL11/GL_COLOR_BUFFER_BIT)
    (GL30/glBindFramebuffer GL30/GL_FRAMEBUFFER 0)
    fb))

(defn createlist
  []
  (let [list (GL11/glGenLists 1)]
    (GL11/glNewList list GL11/GL_COMPILE)
    (GL11/glBegin GL11/GL_QUADS)
    (GL11/glColor3f 1.0 1.0 1.0)
    (GL11/glTexCoord2f 0.0 1.0)  (GL11/glVertex2f -1.0 1.0)
    (GL11/glTexCoord2f 0.0 0.0)  (GL11/glVertex2f -1.0 -1.0)
    (GL11/glTexCoord2f 1.0 0.0)  (GL11/glVertex2f 1.0 -1.0)
    (GL11/glTexCoord2f 1.0 1.0)  (GL11/glVertex2f 1.0 1.0)
    (GL11/glEnd)
    (GL11/glEndList)
    list))


(defn createminilist
  [w h i u0 v0]
  (let [list (GL11/glGenLists 1) 
        x0 (- (* 2.0 (/ (mod i w) w)) 1.0) 
        y0 (- (* 2.0 (/ (quot i w) h)) 1.0)
        u1 (+ u0 (/ 1.0 w))
        v1 (+ v0 (/ 1.0 h))
        x1 (+ x0 (/ 2.0 w))
        y1 (+ y0 (/ 2.0 h))]
    (GL11/glNewList list GL11/GL_COMPILE)
    (GL11/glBegin GL11/GL_QUADS)
    (GL11/glColor3f 1.0 1.0 1.0)
    (GL11/glTexCoord2f u0 v1)  (GL11/glVertex2f x0 y1)
    (GL11/glTexCoord2f u0 v0)  (GL11/glVertex2f x0 y0)
    (GL11/glTexCoord2f u1 v0)  (GL11/glVertex2f x1 y0)
    (GL11/glTexCoord2f u1 v1)  (GL11/glVertex2f x1 y1)
    (GL11/glEnd)
    (GL11/glEndList)
    ;(println (str "x0:" x0 " y0:" y0 " x1:" x1 " y1:" y1))
    list))

(defn blit
  [texture list]
  (GL11/glBindTexture GL11/GL_TEXTURE_2D texture)
  (GL11/glCallList list)
  )


(defn runfb
  [fb texture list]
 )

(defn context
  [window func]
  (locking context
    (GLFW/glfwMakeContextCurrent window)
    (GLContext/createFromCurrent)
    (func window)
    (GLFW/glfwMakeContextCurrent 0)))

(def vshader (into-array String [
                                 "varying vec2 v_texCoord;"
                                 "void main() {"
                                 "v_texCoord = gl_MultiTexCoord0.xy;"
                                 "gl_Position=ftransform();"
                                 "}"
                                 ]))

(defn reshade [window w h]
  (def rprogram (createprogram (createshader vshader GL20/GL_VERTEX_SHADER)
                               (createshader (slurp (clojure.java.io/resource "render.frag")) GL20/GL_FRAGMENT_SHADER)
                               w h))
  (def fprogram (createprogram (createshader vshader GL20/GL_VERTEX_SHADER)
                               (createshader (slurp (clojure.java.io/resource "shader.frag")) GL20/GL_FRAGMENT_SHADER)
                               w h))
  (def sprogram (createprogram (createshader vshader GL20/GL_VERTEX_SHADER)
                               (createshader (slurp (clojure.java.io/resource "sound.frag")) GL20/GL_FRAGMENT_SHADER)
                               w h)))

(defn prepare [window w h]
 	(GL11/glEnable GL11/GL_TEXTURE_2D)
  (def tex1 (createtexture w h))
  (def tex2 (createtexture w h))
  (def texs (createtexture w h))
  (reshade window w h)
  (def fb (createfb tex1 tex2 texs))
  (def pb (createpb))
  (def minilist (IntBuffer/allocate SMPSIZE))
  (loop [i 0]
    (when (< i SMPSIZE)
      (.put minilist i (createminilist w h i 0.5 0.0))
      (recur (inc i))))

  (def drawlist (createlist)))

(def mousec (BufferUtils/createFloatBuffer 4))

(def windoww (atom 0))
(def windowh (atom 0))
(def mousex (atom 0.0))
(def mousey (atom 0.0))
(def mousel (atom false))
(def flipper (atom 0))

;IMPORTANT CALLBACKS MUST BE DEFINED STATIC TO AVOID GARBAGE COLLECTING THEM
(def close-callback
  (proxy [GLFWWindowCloseCallback] []
    (invoke [window] (GLFW/glfwSetWindowShouldClose window GL11/GL_TRUE))))

(def size-callback 
  (proxy [GLFWWindowSizeCallback] []
    (invoke [window w h]
      (reset! windoww w)
      (reset! windowh h)
      (context window (fn [window] (GL11/glViewport 0 0 w h))))))

(def cursor-callback
  (proxy [GLFWCursorPosCallback] []
    (invoke [window x y]
      (reset! mousex x)
      (reset! mousey y))))

(def mouse-callback
  (proxy [GLFWMouseButtonCallback] []
    (invoke [window b a m]
      (if (and (= b GLFW/GLFW_MOUSE_BUTTON_LEFT) (= a GLFW/GLFW_PRESS)) (reset! mousel true))
      (if (and (= b GLFW/GLFW_MOUSE_BUTTON_LEFT) (= a GLFW/GLFW_RELEASE)) (reset! mousel false)))))


(defn event-loop [window w h]
  (let [alcontext (ALContext/create)
        ornt (BufferUtils/createFloatBuffer 6)
        buffers (BufferUtils/createIntBuffer 2)
        source (AL10/alGenSources)
        ]

    (.put mousec 0 1.0)
    (.put mousec 1 1.0)
    (.put mousec 2 1.0)
    (.put mousec 3 1.0)
    (reset! windoww w)
    (reset! windowh h)
    (reset! mousel false)

    (.put ornt (float-array [0.0 0.0 -1.0 0.0 1.0 0.0]))
    (.flip ornt)

    (AL10/alListener3f AL10/AL_POSITION 0.0 0.0 0.0)
    (AL10/alListener3f AL10/AL_VELOCITY 0.0 0.0 0.0)
    (AL10/alListenerfv AL10/AL_ORIENTATION ornt)
 
    (AL10/alSourcef source AL10/AL_PITCH 1.0)
    (AL10/alSourcef source AL10/AL_GAIN 1.0)
    (AL10/alSource3f source AL10/AL_POSITION 0.0 0.0 0.0)
    (AL10/alSource3f source AL10/AL_VELOCITY 0.0 0.0 0.0)

    (AL10/alGenBuffers buffers)

    (while (= (GLFW/glfwWindowShouldClose window) GL11/GL_FALSE)
      (do
        (let [b (.get buffers (swap! flipper (fn [x] (bit-xor x 1))))]

          (if (= AL10/AL_PLAYING (AL10/alGetSourcei source AL10/AL_SOURCE_STATE))
            (while (= 0 (AL10/alGetSourcei source AL10/AL_BUFFERS_PROCESSED))))

          (AL10/alSourceUnqueueBuffers source)

          (context window
                   (fn [window]
                     (GL11/glPushAttrib GL11/GL_VIEWPORT_BIT)
                     (GL11/glViewport 0 0 w h)
                     (GL30/glBindFramebuffer GL30/GL_FRAMEBUFFER fb)
                     (GL20/glUseProgram fprogram)
                     (time
                     (loop [i 0
                            flip false]
                       (when (< i SMPSIZE)
                         (GL20/glDrawBuffers
                          (int (if flip GL30/GL_COLOR_ATTACHMENT0 GL30/GL_COLOR_ATTACHMENT1)))
                         (GL11/glReadBuffer
                          (int (if flip GL30/GL_COLOR_ATTACHMENT1 GL30/GL_COLOR_ATTACHMENT0)))

                         (GL11/glBindTexture GL11/GL_TEXTURE_2D (if flip tex2 tex1))
                         (GL11/glCallList drawlist)

                        (GL20/glDrawBuffers GL30/GL_COLOR_ATTACHMENT2)
                        (GL11/glCallList (.get minilist i))

                         (recur (inc i) (not flip))))
                     )
                   ;  (time (do
                     (GL15/glBindBuffer GL21/GL_PIXEL_PACK_BUFFER pb)
                     (GL11/glReadBuffer GL30/GL_COLOR_ATTACHMENT2)
                     (GL11/glReadPixels 0 0 256 8 GL11/GL_RED GL11/GL_FLOAT 0)
                     (let [mappy (GL15/glMapBuffer GL21/GL_PIXEL_PACK_BUFFER GL15/GL_READ_ONLY)]
                       (AL10/alBufferData b EXTFloat32/AL_FORMAT_MONO_FLOAT32 mappy SAMPLERATE))
                     (GL15/glUnmapBuffer GL21/GL_PIXEL_PACK_BUFFER)
                     (GL15/glBindBuffer GL21/GL_PIXEL_PACK_BUFFER 0)
                     (GL11/glReadBuffer GL11/GL_FRONT)
                    ; ))
                     (GL11/glPopAttrib)
                     (GL30/glBindFramebuffer GL30/GL_FRAMEBUFFER 0)
                     ))

          (AL10/alSourceQueueBuffers source b)

          (if-not (= AL10/AL_PLAYING (AL10/alGetSourcei source AL10/AL_SOURCE_STATE))
            (if (= 2 (AL10/alGetSourcei source AL10/AL_BUFFERS_QUEUED))
              (AL10/alSourcePlay source)))


          (GLFW/glfwPollEvents)
          (context window
                   (fn [window]
                     (if @mousel
                            (let [x (quot (* (int @mousex) w) @windoww)
                                  y (- h (quot (* (int @mousey) h) @windowh) 1)]
                              (GL11/glBindTexture GL11/GL_TEXTURE_2D tex1)
                              (GL11/glTexSubImage2D GL11/GL_TEXTURE_2D 0
                                                    x y 1 1
                                                    GL11/GL_RGBA GL11/GL_FLOAT mousec)))
                     (GL20/glUseProgram rprogram)
                     (blit texs drawlist)
                          ;(GL11/glBlendFunc GL11/GL_SRC_ALPHA GL11/GL_ONE_MINUS_SRC_ALPHA)
                          ;(GL11/glEnable GL11/GL_BLEND)
                          ;(GL20/glUseProgram rprogram)
                          ;(blit texs drawlist)
                          ;(GL11/glDisable GL11/GL_BLEND)
                          ))


          (GLFW/glfwSwapBuffers window)

          (println (str " seq " @flipper
                        " queued " (AL10/alGetSourcei source AL10/AL_BUFFERS_QUEUED)
                        " processed " (AL10/alGetSourcei source AL10/AL_BUFFERS_PROCESSED)
                        ))
       )))
  (println "closing")
  (.destroy alcontext)
  (GLFW/glfwDestroyWindow window)
  ))



(defn init
  [w h]
  (let [g (GLFW/glfwInit)
        window (GLFW/glfwCreateWindow w h "Mio" 0 0)]
 
    (GLFW/glfwSetWindowCloseCallback window close-callback)
    (GLFW/glfwSetWindowSizeCallback window size-callback)
    (GLFW/glfwSetMouseButtonCallback window mouse-callback)
    (GLFW/glfwSetCursorPosCallback window cursor-callback)
    (GLFW/glfwSwapInterval 0)
    (context window (fn [window] (prepare window w h)))

    ;(event-loop window w h)
    (future (event-loop window w h))
    window))


;(context window (fn [window] (varpars fprogram 0.50 0.996093 0.50 0.9986))) 


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
