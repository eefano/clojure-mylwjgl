kernel void drum(const unsigned int w,      // width
                 const unsigned int h,      // height
                 global const float* coef,  // material coefficients
                 global const float* prev,  // previous state
                 global float* next)        // next state
{
  const unsigned int i = get_global_id(0);
  const unsigned int x = i % w;
  const unsigned int y = i / w;
  const unsigned int yxw = i - x;

  const unsigned int y0 = (y-1) % h;
  const unsigned int y1 = (y+1) % h;
  const unsigned int x0 = (x-1) % w;
  const unsigned int x1 = (x+1) % w;

  const float py0 = prev[x+y0*w];
  const float py1 = prev[x+y1*w];
  const float px0 = prev[x0+yxw];
  const float px1 = prev[x1+yxw];

  next[i] = ((py0+py1+px0+px1) * 0.5 - next[i]) * coef[i];
}
