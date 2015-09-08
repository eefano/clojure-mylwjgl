(ns mylwjgl.core
  (:import java.nio.ByteBuffer)
  (:import java.nio.IntBuffer)
  (:import org.lwjgl.BufferUtils)
  (:import org.lwjgl.LWJGLUtil)
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
  (:import org.lwjgl.opencl.CL)
  (:import org.lwjgl.opencl.CL10)
  (:import org.lwjgl.opencl.CLUtil)
  (:import org.lwjgl.opencl.CLPlatform)
)

(def SAMPLERATE 44100)
(def BUFSIZE 2048)
(def SMPSIZE (/ BUFSIZE 4))
(def SHORTSIZE (/ BUFSIZE 2))
(def BIGRATE (double 0x40000000))
(def RATEBIG (/ 1.0 BIGRATE))
(def RATESAMPLE (/ 1.0 SAMPLERATE))
(def BIGSAMPLE (* BIGRATE RATESAMPLE))


(defn initializecl []
  (let [cld (CL/destroy)
        clc (CL/create)
        ret (BufferUtils/createIntBuffer 1)
        platforms (BufferUtils/createPointerBuffer 1)
        ids (CL10/clGetPlatformIDs platforms nil)
        platform (.get platforms 0)
        devices (BufferUtils/createPointerBuffer 1)
        did (CL10/clGetDeviceIDs platform CL10/CL_DEVICE_TYPE_ALL devices nil)
        device (.get devices 0)
        properties (doto (BufferUtils/createPointerBuffer 3)
                     (.put CL10/CL_CONTEXT_PLATFORM)
                     (.put platform)
                     (.rewind))
        context (CL10/clCreateContext properties device nil 0 ret)
        rt1 (.get ret 0)
        queue (CL10/clCreateCommandQueue context device 0 ret)
        rt2 (.get ret 0)
        ]
    (println (str "ids " ids " platform " platform))
    (println (str "did " did " device " device))
    (println (str "clCreateContext " (CLUtil/getErrcodeName rt1)))
    (println (str "clCreateCommandQueue " (CLUtil/getErrcodeName rt2)))
    {:context context :device device :queue queue})
  )

(defn loadcl [env]
  (let [
        src (slurp (clojure.java.io/resource "sum.cls")) 
        ret (BufferUtils/createIntBuffer 1)
        program (CL10/clCreateProgramWithSource (:context env) src ret)
        rt1 (.get ret 0)
        rt2 (CL10/clBuildProgram ^Long program ^Long (:device env) "" nil 0)
        kernel (CL10/clCreateKernel program "sum" ret)
        rt3 (.get ret 0)
        ]
    (println (str "program " program " kernel " kernel))
    (println (str "clCreateProgramWithSource " (CLUtil/getErrcodeName rt1)))
    (println (str "clBuildProgram " (CLUtil/getErrcodeName rt2)))
    (println (str "clCreateKernel " (CLUtil/getErrcodeName rt3)))
    {:program program :kernel kernel})
  )

(defn allocl [env nio flg]
  (let [
        ret (BufferUtils/createIntBuffer 1)
        ctx ^Long (:context env)
        lflg (long flg)
        buffer (CL10/clCreateBuffer ctx lflg nio ret)
        rt1 (.get ret 0)
        ]
    (println (str "clCreateBuffer " (CLUtil/getErrcodeName rt1)))
    buffer)
  )


(defn testcl [env pgm n]
  (let [
        k (:kernel pgm)
        q (:queue env)
        size (* n 4)
        f1 (doto (BufferUtils/createFloatBuffer n)
             (.put (float-array (range n)))
             (.rewind))
        f3 (BufferUtils/createFloatBuffer n)
        b1 (allocl env f1 CL10/CL_MEM_COPY_HOST_PTR)
        b2 (allocl env f1 CL10/CL_MEM_COPY_HOST_PTR)
        b3 (allocl env size CL10/CL_MEM_READ_ONLY)
        pb (BufferUtils/createPointerBuffer 1)
        ]
    (.put pb b1)
    (.rewind pb)
    (print (CL10/clSetKernelArg k 0 pb) " ")
    (.put pb b2)
    (.rewind pb)
    (print (CL10/clSetKernelArg k 1 pb) " ")
    (.put pb b3)
    (.rewind pb)
    (print (CL10/clSetKernelArg k 2 pb) " ")
    (print (CL10/clSetKernelArg1i k 3 n) " ")

    (.put pb n)
    (.rewind pb)
    (time (do
    (print (CL10/clEnqueueNDRangeKernel q k 1 nil pb nil nil nil) " ")
    (println (CL10/clEnqueueReadBuffer q b3 CL10/CL_TRUE 0 f3 nil nil))
    ))
    (doseq [x (range n)] (println (str (.get f1 x) " " (.get f1 x) " " (.get f3 x)))))
  )

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
    (GL15/glBufferData GL21/GL_PIXEL_PACK_BUFFER BUFSIZE GL15/GL_STREAM_READ)
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
    (GL11/glClearColor 0.0 0.0 0.0 0.0)
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
  [w h u0 v0 x y]
  (let [list (GL11/glGenLists 1)
        x0 (- (/ (* 2.0 x) w) 1.0)
        y0 (- (/ (* 2.0 y) h) 1.0)
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
  (GL15/glBindBuffer GL21/GL_PIXEL_PACK_BUFFER pb)
  (def minilist (IntBuffer/allocate SMPSIZE))
  (loop [i 0]
    (when (< i SMPSIZE)
      (.put minilist i (createminilist w h 0.5 0.0 0 i))
      (recur (inc i))))

  (def drawlist (createlist)))

(def mousec (BufferUtils/createFloatBuffer 4))
(def mappy (BufferUtils/createByteBuffer BUFSIZE))


(def windoww (atom 0))
(def windowh (atom 0))
(def mousex (atom 0.0))
(def mousey (atom 0.0))
(def mousel (atom false))
(def flipper (atom false))

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

  (.put mousec 0 1.0)
  (.put mousec 1 1.0)
  (.put mousec 2 1.0)
  (.put mousec 3 1.0)
  (reset! windoww w)
  (reset! windowh h)
  (reset! mousel false)

  (while (= (GLFW/glfwWindowShouldClose window) GL11/GL_FALSE)
    (do (GLFW/glfwPollEvents)
        (context window (fn [window]
                          (GL11/glPushAttrib GL11/GL_VIEWPORT_BIT)
                          (GL11/glViewport 0 0 w h)
                          (GL30/glBindFramebuffer GL30/GL_DRAW_FRAMEBUFFER fb)
                          (GL20/glUseProgram fprogram)
                         ; (time
                           (loop [i 0
                                  flip false]
                             (when (< i SMPSIZE)
                               (GL20/glDrawBuffers
                                (int (if flip GL30/GL_COLOR_ATTACHMENT0 GL30/GL_COLOR_ATTACHMENT1)))
                               (GL11/glBindTexture GL11/GL_TEXTURE_2D (if flip tex2 tex1))
                               (GL11/glCallList drawlist)

                               (GL20/glDrawBuffers GL30/GL_COLOR_ATTACHMENT2)
                               (GL11/glCallList (.get minilist i))

                               (recur (inc i) (not flip))))
                          ; )
           ;               (time
                           (do
                            (GL11/glReadBuffer GL30/GL_COLOR_ATTACHMENT2)
                            (GL11/glReadPixels 20 0 1 1 GL11/GL_GREEN GL11/GL_FLOAT 0)
                       ;     (GL15/glMapBuffer GL21/GL_PIXEL_PACK_BUFFER GL15/GL_READ_ONLY mappy)
                       ;     (GL15/glUnmapBuffer GL21/GL_PIXEL_PACK_BUFFER)
                            
                            )
                           ;)
                          (GL11/glPopAttrib)
                          (if @mousel
                            (let [x (quot (* (int @mousex) w) @windoww)
                                  y (- h (quot (* (int @mousey) h) @windowh) 1)]
                              (GL11/glBindTexture GL11/GL_TEXTURE_2D tex1)
                              (GL11/glTexSubImage2D GL11/GL_TEXTURE_2D 0
                                                    x y 1 1
                                                    GL11/GL_RGBA GL11/GL_FLOAT mousec)))
                          (GL30/glBindFramebuffer GL30/GL_FRAMEBUFFER 0)
                          (GL20/glUseProgram rprogram)
                          (blit tex1 drawlist)
                          (GL11/glBlendFunc GL11/GL_SRC_ALPHA GL11/GL_ONE_MINUS_SRC_ALPHA)
                          (GL11/glEnable GL11/GL_BLEND)
                          (GL20/glUseProgram sprogram)
                          (blit texs drawlist)
                          (GL11/glDisable GL11/GL_BLEND)
                          ))
        (GLFW/glfwSwapBuffers window)))
  (println "closing")
  (GLFW/glfwDestroyWindow window))

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

(defn audio []
  (let [context (ALContext/create)
        ornt (BufferUtils/createFloatBuffer 6)]

    (.put ornt (float-array [0.0 0.0 -1.0 0.0 1.0 0.0]))
    (.flip ornt)

    (AL10/alListener3f AL10/AL_POSITION 0.0 0.0 0.0)
    (AL10/alListener3f AL10/AL_VELOCITY 0.0 0.0 0.0)
    (AL10/alListenerfv AL10/AL_ORIENTATION ornt)
    context))

(defn stream []
  (let [
        buffers (BufferUtils/createIntBuffer 2)
        source (AL10/alGenSources)
        onda (BufferUtils/createByteBuffer 40000)
        ondaf (.asFloatBuffer onda)
        ]

    (AL10/alSourcef source AL10/AL_PITCH 1.0)
    (AL10/alSourcef source AL10/AL_GAIN 1.0)
    (AL10/alSource3f source AL10/AL_POSITION 0.0 0.0 0.0)
    (AL10/alSource3f source AL10/AL_VELOCITY 0.0 0.0 0.0)


    (AL10/alGenBuffers buffers)


    (doseq [x (range 10)]
      (let [b (.get buffers (mod x 2))]

        (if (= AL10/AL_PLAYING (AL10/alGetSourcei source AL10/AL_SOURCE_STATE))
          (while (= 0 (AL10/alGetSourcei source AL10/AL_BUFFERS_PROCESSED))))

        (AL10/alSourceUnqueueBuffers source)
        (doseq [i (range 10000)] (.put ondaf (* 0.5 (Math/sin (* i (+ 0.10 (* x 0.01)))))))
        (.flip ondaf)

        (AL10/alBufferData b EXTFloat32/AL_FORMAT_MONO_FLOAT32 onda SAMPLERATE)
        (AL10/alSourceQueueBuffers source b)

        (if-not (= AL10/AL_PLAYING (AL10/alGetSourcei source AL10/AL_SOURCE_STATE))
          (if (= 2 (AL10/alGetSourcei source AL10/AL_BUFFERS_QUEUED))
            (AL10/alSourcePlay source)))

        (println (str " seq " x
                      " queued " (AL10/alGetSourcei source AL10/AL_BUFFERS_QUEUED)
                      " processed " (AL10/alGetSourcei source AL10/AL_BUFFERS_PROCESSED)
                      ))

        ))

   ))

;(context window (fn [window] (varpars fprogram 0.50 0.996093 0.50 0.9986))) 


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
