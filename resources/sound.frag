uniform sampler2D tex;
varying vec2 v_texCoord;

void main()
{
  //gl_FragColor = texture2D(tex, v_texCoord);
  
  float G = texture2D(tex, vec2(0.0,v_texCoord.y)).g;
  if(G>=v_texCoord.x-0.001 && G<=v_texCoord.x+0.001)
    gl_FragColor = vec4(0.0,1.0,0.0,1.0);
  else
    gl_FragColor = vec4(0.0,0.0,0.0,0.0);
  
    }
