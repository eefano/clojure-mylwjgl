(ns mylwjgl.core
  (:import org.lwjgl.BufferUtils)
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
  (:import org.lwjgl.opengl.GL20)
  (:import org.lwjgl.opengl.GL30))

(defn createshader
  [source type]
  (let [shader (GL20/glCreateShader type)]
    (GL20/glShaderSource shader source )
    (GL20/glCompileShader shader)
    (println (GL20/glGetShaderInfoLog shader))
    shader))

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

(defn createfb
  [texture]
  (let [fb (GL30/glGenFramebuffers)]
    (GL30/glBindFramebuffer GL30/GL_FRAMEBUFFER fb)
    (GL30/glFramebufferTexture2D GL30/GL_FRAMEBUFFER GL30/GL_COLOR_ATTACHMENT0 GL11/GL_TEXTURE_2D texture 0)
    (println (str "glFramebufferTexture2D " (GL11/glGetError)))
    (println (str "glCheckFramebufferStatus " (GL30/glCheckFramebufferStatus GL30/GL_FRAMEBUFFER)))
    (GL11/glClearColor 0.5 0.5 0.5 0.5)
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

(defn blit
  [texture list]
  (GL11/glBindTexture GL11/GL_TEXTURE_2D texture)
  (GL11/glCallList list)
  )


(defn runfb
  [fb texture list]
  (GL30/glBindFramebuffer GL30/GL_FRAMEBUFFER fb)
  (blit texture list)
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

(def rshader (into-array String [
                                 "uniform sampler2D tex;"
                                 "varying vec2 v_texCoord;"
                                 "void main() {"
                                 "float r = texture2D(tex, v_texCoord);"
                                 "gl_FragColor = vec4(r,r,r,r);"
                                 "}"
                                 ]))

(defn reshade [window w h]
  (def rprogram (createprogram (createshader vshader GL20/GL_VERTEX_SHADER)
                               (createshader rshader GL20/GL_FRAGMENT_SHADER)
                               w h))
  (def fprogram (createprogram (createshader vshader GL20/GL_VERTEX_SHADER)
                               (createshader (slurp (clojure.java.io/resource "shader.frag")) GL20/GL_FRAGMENT_SHADER)
                               w h)))

(defn prepare [window w h]
 	(GL11/glEnable GL11/GL_TEXTURE_2D)
 (def tex1 (createtexture w h))
  (def tex2 (createtexture w h))
  (reshade window w h)
  (def fb1 (createfb tex1))
  (def fb2 (createfb tex2))
  (def drawlist (createlist)))

(def mousec (BufferUtils/createFloatBuffer 4))


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
                          (let [flip (swap! flipper not)
                                fb (if flip fb1 fb2)
                                ftex (if flip tex2 tex1)
                                rtex (if flip tex1 tex2)]
                            (GL11/glPushAttrib GL11/GL_VIEWPORT_BIT)
                            (GL11/glViewport 0 0 w h)
                            (GL20/glUseProgram fprogram)
                            (time (loop [i 0] (when (< i 44100)
                                                (runfb fb ftex drawlist)
                                                (recur (inc i)))))
                            (GL11/glPopAttrib)
                            (if @mousel
                              (let [x (quot (* (int @mousex) w) @windoww)
                                    y (- h (quot (* (int @mousey) h) @windowh) 1)]
                                (GL11/glBindTexture GL11/GL_TEXTURE_2D rtex)
                                (GL11/glTexSubImage2D GL11/GL_TEXTURE_2D 0
                                                      x y 1 1
                                                      GL11/GL_RGBA GL11/GL_FLOAT mousec)))
                            (GL30/glBindFramebuffer GL30/GL_FRAMEBUFFER 0)
                            (GL20/glUseProgram rprogram)
                            (blit rtex drawlist))))
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
    (GLFW/glfwSwapInterval 1)
    (context window (fn [window] (prepare window w h)))
    ;(event-loop window w h)
    (future (event-loop window w h))
    window))



(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
