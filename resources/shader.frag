varying vec2 v_texCoord;
uniform sampler2D tex; //the input texture
uniform float du; //the width of the cells
uniform float dv; //the height of the cells
 
void main() {
 
  vec4 C = texture2D( tex, v_texCoord );
  vec4 E = texture2D( tex, vec2(v_texCoord.x + du, v_texCoord.y) );
  vec4 N = texture2D( tex, vec2(v_texCoord.x, v_texCoord.y + dv) );
  vec4 W = texture2D( tex, vec2(v_texCoord.x - du, v_texCoord.y) );
  vec4 S = texture2D( tex, vec2(v_texCoord.x, v_texCoord.y - dv) );
  vec4 NE = texture2D( tex, vec2(v_texCoord.x + du, v_texCoord.y + dv) );
  vec4 NW = texture2D( tex, vec2(v_texCoord.x - du, v_texCoord.y + dv) );
  vec4 SE = texture2D( tex, vec2(v_texCoord.x + du, v_texCoord.y - dv) );
  vec4 SW = texture2D( tex, vec2(v_texCoord.x - du, v_texCoord.y - dv) );
 
  gl_FragColor = N;
}

