uniform sampler2D tex;
varying vec2 v_texCoord;

void main()
{
  float r = texture2D(tex, v_texCoord);
  gl_FragColor = vec4(r,r,r,r);
}
