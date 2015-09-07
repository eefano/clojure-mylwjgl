varying vec2 v_texCoord;
uniform sampler2D tex; //the input texture
uniform float du; //the width of the cells
uniform float dv; //the height of the cells
uniform float aflex,adamp,bflex,bdamp;

void main() {

  float off = 0.5;
  float R = texture2D( tex, v_texCoord ).r;
  float G = texture2D( tex, v_texCoord ).g - off;

  vec4 E = texture2D( tex, vec2(v_texCoord.x + du, v_texCoord.y) );
  vec4 N = texture2D( tex, vec2(v_texCoord.x, v_texCoord.y + dv) );
  vec4 W = texture2D( tex, vec2(v_texCoord.x - du, v_texCoord.y) );
  vec4 S = texture2D( tex, vec2(v_texCoord.x, v_texCoord.y - dv) );

  float Er = E.r - off;
  float Nr = N.r - off;
  float Wr = W.r - off;
  float Sr = S.r - off;

  float X = ((Nr+Wr+Sr+Er) * bflex - G) * bdamp  + off;
  
  gl_FragColor = vec4(X,R,0.0,1.0);
}

