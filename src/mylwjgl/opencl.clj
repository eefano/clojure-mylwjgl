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
  (:import org.lwjgl.opencl.CL)
  (:import org.lwjgl.opencl.CL10)
  (:import org.lwjgl.opencl.CLUtil)
  (:import org.lwjgl.opencl.CLPlatform)
)

(def SAMPLERATE 44100)
(def BUFSIZE 4192)
(def SMPSIZE (/ BUFSIZE 4))


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
        src (slurp (clojure.java.io/resource "drum.c")) 
        ret (BufferUtils/createIntBuffer 1)
        program (CL10/clCreateProgramWithSource (:context env) src ret)
        rt1 (.get ret 0)
        rt2 (CL10/clBuildProgram ^Long program ^Long (:device env) "" nil 0)
        kernel (CL10/clCreateKernel program "drum" ret)
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


(defn testcl [env pgm w h]
  (let [
        k (:kernel pgm)
        q (:queue env)
        n (* w h)
        size (* n 4)
        f1 (doto (BufferUtils/createFloatBuffer n)
             (.put (float-array (range n)))
             (.rewind))
        f3 (BufferUtils/createFloatBuffer n)
        b1 (allocl env f1 CL10/CL_MEM_COPY_HOST_PTR)
        b2 (allocl env f1 CL10/CL_MEM_COPY_HOST_PTR)
        b3 (allocl env f1 CL10/CL_MEM_COPY_HOST_PTR)
        pb (BufferUtils/createPointerBuffer 1)
        ]
    (print (CL10/clSetKernelArg1i k 0 w) " ")
    (print (CL10/clSetKernelArg1i k 1 h) " ")
    (.put pb b1)
    (.rewind pb)
    (print (CL10/clSetKernelArg k 2 pb) " ")
    (.put pb b2)
    (.rewind pb)
    (print (CL10/clSetKernelArg k 3 pb) " ")
    (.put pb b3)
    (.rewind pb)
    (print (CL10/clSetKernelArg k 4 pb) " ")

    (.put pb n)
    (.rewind pb)
    (time (do
    (print (CL10/clEnqueueNDRangeKernel q k 1 nil pb nil nil nil) " ")
    (println (CL10/clEnqueueReadBuffer q b3 CL10/CL_TRUE 0 f3 nil nil))
    ))
    (doseq [x (range n)] (println (str (.get f1 x) " " (.get f1 x) " " (.get f3 x)))))
  )



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
    texture))

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

(defn blit
  [texture list]
  (GL11/glBindTexture GL11/GL_TEXTURE_2D texture)
  (GL11/glCallList list)
  )

(defn context
  [window func]
  (locking context
    (GLFW/glfwMakeContextCurrent window)
    (GLContext/createFromCurrent)
    (func window)
    (GLFW/glfwMakeContextCurrent 0)))

(defn prepare [window w h]
 	(GL11/glEnable GL11/GL_TEXTURE_2D)
  (def tex1 (createtexture w h))
  (def drawlist (createlist))
  (def mousec (BufferUtils/createFloatBuffer 4))
  (def windoww (atom 0))
  (def windowh (atom 0))
  (def mousex (atom 0.0))
  (def mousey (atom 0.0))
  (def mousel (atom false))
  (def flipper (atom 1))
  )


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


(defn sound-loop [window w h]
  (let [env (initializecl)
        pgm (loadcl env)
        alcontext (ALContext/create)
        ornt (BufferUtils/createFloatBuffer 6)
        buffers (BufferUtils/createIntBuffer 3)
        source (AL10/alGenSources)
        mappy (BufferUtils/createByteBuffer BUFSIZE)
        mappyf (.asFloatBuffer mappy)

        k (:kernel pgm)
        q (:queue env)
        n (* w h)
        size (* n 4)
        f1 (doto (BufferUtils/createFloatBuffer n)
             (.put (float-array (range n)))
             (.rewind))
        f3 (BufferUtils/createFloatBuffer n)
        b1 (allocl env f1 CL10/CL_MEM_COPY_HOST_PTR)
        b2 (allocl env f1 CL10/CL_MEM_COPY_HOST_PTR)
        b3 (allocl env f1 CL10/CL_MEM_COPY_HOST_PTR)
        pb (BufferUtils/createPointerBuffer 1)
        ]

    (print (CL10/clSetKernelArg1i k 0 w) " ")
    (print (CL10/clSetKernelArg1i k 1 h) " ")
    (.put pb b1)
    (.rewind pb)
    (print (CL10/clSetKernelArg k 2 pb) " ")
    (.put pb b2)
    (.rewind pb)
    (CL10/clSetKernelArg k 3 pb)
    (.put pb b3)
    (.rewind pb)
    (CL10/clSetKernelArg k 4 pb)

    (.put pb n)
    (.rewind pb)
 


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

    (doseq [x (range 3)]
      (AL10/alBufferData (.get buffers x) EXTFloat32/AL_FORMAT_MONO_FLOAT32 mappy SAMPLERATE))

    (AL10/alSourceQueueBuffers source buffers)
    (AL10/alSourcePlay source)

    (while (= (GLFW/glfwWindowShouldClose window) GL11/GL_FALSE)
      (do
        (while (> (AL10/alGetSourcei source AL10/AL_BUFFERS_PROCESSED) 0)
          (let [
                b (AL10/alSourceUnqueueBuffers source)
                ]
(time
            (loop [i 0]
              (when (< i SMPSIZE)
               (CL10/clEnqueueNDRangeKernel q k 1 nil pb nil nil nil)
               (CL10/clFinish q)
                (.put mappyf i 1.0)
                (recur (inc i))))
)
           (AL10/alBufferData b EXTFloat32/AL_FORMAT_MONO_FLOAT32 mappy SAMPLERATE)
            (AL10/alSourceQueueBuffers source b)

            (if-not (= AL10/AL_PLAYING (AL10/alGetSourcei source AL10/AL_SOURCE_STATE))
              (AL10/alSourcePlay source))

            (println (str " buffer " b
                        ))
            ))))
  (println "closing audio")
  (.destroy alcontext)
  ))



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
                         (if @mousel
                            (let [x (quot (* (int @mousex) w) @windoww)
                                  y (- h (quot (* (int @mousey) h) @windowh) 1)]
                              (GL11/glBindTexture GL11/GL_TEXTURE_2D tex1)
                              (GL11/glTexSubImage2D GL11/GL_TEXTURE_2D 0
                                                    x y 1 1
                                                    GL11/GL_RGBA GL11/GL_FLOAT mousec)))
                          (blit tex1 drawlist)
                          ))
        (GLFW/glfwSwapBuffers window)))
  (println "closing window")
  (GLFW/glfwDestroyWindow window))

(defn init
  [w h]
  (let [g (GLFW/glfwInit)
        window (GLFW/glfwCreateWindow w h "Mio" 0 0)]
 
    (GLFW/glfwSetWindowCloseCallback window close-callback)
    (GLFW/glfwSetWindowSizeCallback window size-callback)
    (GLFW/glfwSetMouseButtonCallback window mouse-callback)
    (GLFW/glfwSetCursorPosCallback window cursor-callback)
    (GLFW/glfwSwapInterval 2)
    (context window (fn [window] (prepare window w h)))

    ;(event-loop window w h)
    (future (event-loop window w h))
    (future (sound-loop window w h))
    window))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
