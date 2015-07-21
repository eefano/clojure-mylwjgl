varying vec2 v_texCoord;
uniform sampler2D tex; //the input texture
uniform float du; //the width of the cells
uniform float dv; //the height of the cells
 
void main() {

  float off = 0.5;
  float R = texture2D( tex, v_texCoord ).r;
  float G = texture2D( tex, v_texCoord ).g - off;

  float E = texture2D( tex, vec2(v_texCoord.x + du, v_texCoord.y) ).r - off;
  float N = texture2D( tex, vec2(v_texCoord.x, v_texCoord.y + dv) ).r - off;
  float W = texture2D( tex, vec2(v_texCoord.x - du, v_texCoord.y) ).r - off;
  float S = texture2D( tex, vec2(v_texCoord.x, v_texCoord.y - dv) ).r - off;
  
  float X = ((N+W+S+E)/2.0 - G) * 0.995  + off;
  
  gl_FragColor = vec4(X,R,off,off);
}

