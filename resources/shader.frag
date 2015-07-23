varying vec2 v_texCoord;
uniform sampler2D tex; //the input texture
uniform float du; //the width of the cells
uniform float dv; //the height of the cells
 
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

  float X;
  if(v_texCoord.x<du)
    {
      X = off;
    }
  else if(v_texCoord.x>=(1.0-du))
    {
      X = off;
    }
  else if(v_texCoord.y<dv || v_texCoord.y>=(1.0-dv))
    {
      X = ((Er+Wr) * 0.502 - G) * 0.993 + off;
    }
  else X = ((Nr+Wr+Sr+Er) * 0.5 - G) * 0.995  + off;
  
  gl_FragColor = vec4(X,R,0.0,1.0);
}

