import numpy as np, os
from PIL import Image
B='/home/claude/civ/src/main/resources/assets/pvpciv/textures/'
os.makedirs(B+'block',exist_ok=True); os.makedirs(B+'misc',exist_ok=True)
def save(a,p): Image.fromarray(np.clip(a,0,255).astype(np.uint8),'RGBA').save(B+p)
def hexc(h): return np.array([int(h[i:i+2],16) for i in (1,3,5)],float)
N=16
yy,xx=np.mgrid[0:N,0:N]
edge=np.minimum(np.minimum(xx,N-1-xx),np.minimum(yy,N-1-yy))
def tile(col,dim=1.0,bg='#0a0a12'):
    c=hexc(col); b=hexc(bg)
    a=np.zeros((N,N,4)); a[...,3]=255
    k=np.where(edge==0,1.0,np.where(edge==1,0.45,np.where(edge==2,0.14,0.0)))*dim
    # центр: мягкий ромб
    d=np.abs(xx-7.5)+np.abs(yy-7.5)
    k=np.maximum(k,np.where(np.abs(d-3.5)<0.6,0.55*dim,0)+np.where(d<1.6,0.3*dim,0))
    for i in range(3): a[...,i]=b[i]*(1-k)+c[i]*k + 255*np.where(edge==0,0.25*dim,0)
    return a
cols={'red':'#ff3050','blue':'#3a7bff','yellow':'#ffd23a','green':'#39ff7a'}
for n,c in cols.items(): save(tile(c),f'block/rift_neon_{n}.png')
save(tile('#8080a0',0.28),'block/rift_neon_off.png')
# пол лазеров: тёмный с тонкой сеткой
a=np.zeros((N,N,4)); a[...,3]=255; a[...,:3]=hexc('#07060c')
g=hexc('#ff2fd0')
m=((xx==0)|(yy==0))*0.55
m=np.maximum(m,((xx%4==0)&(yy%4==0))*0.25)
for i in range(3): a[...,i]=a[...,i]*(1-m)+g[i]*m
save(a,'block/rift_floor.png')
# стена: фиолетовая, рамка, диагональные полосы
a=np.zeros((N,N,4)); a[...,3]=255; a[...,:3]=hexc('#1a0b2c')
g=hexc('#c040ff')
m=np.where(edge==0,1.0,np.where(edge==1,0.35,0))
m=np.maximum(m,(((xx+yy)%6==0)&(edge>1))*0.28)
for i in range(3): a[...,i]=a[...,i]*(1-m)+g[i]*m
save(a,'block/rift_wall.png')
# хрупкая стена: бирюзовая, трещины
a=np.zeros((N,N,4)); a[...,3]=255; a[...,:3]=hexc('#06222a')
g=hexc('#30f0ff')
m=np.where((edge==0)&(((xx+yy)%3)!=0),0.9,0.0)
crack=[(8,2),(7,3),(7,4),(8,5),(9,6),(9,7),(8,8),(7,9),(6,10),(6,11),(7,12),(8,13),(4,7),(5,8),(6,8),(10,8),(11,9),(12,9),(11,4),(10,5),(3,12),(4,11),(5,11)]
for (x,y) in crack: m[y,x]=1.0
for (x,y) in crack:
    for dx,dy in ((1,0),(0,1),(-1,0),(0,-1)):
        if 0<=x+dx<N and 0<=y+dy<N: m[y+dy,x+dx]=max(m[y+dy,x+dx],0.3)
for i in range(3): a[...,i]=a[...,i]*(1-m)+g[i]*m+255*(m==1.0)*0.2
save(a,'block/rift_wall_weak.png')
# ступенька: жёлтые шевроны вверх
a=np.zeros((N,N,4)); a[...,3]=255; a[...,:3]=hexc('#141006')
g=hexc('#ffcc20')
m=np.where(edge==0,0.8,0.0)
for base in (5,11):
    for x in range(2,14):
        y=base-(7-abs(x-7.5))*0.6
        yi=int(round(y))
        for t in (0,1):
            if 0<=yi+t<N: m[yi+t,x]=max(m[yi+t,x],1.0 if t==0 else 0.4)
for i in range(3): a[...,i]=a[...,i]*(1-m)+g[i]*m
save(a,'block/rift_step.png')
# телепорт-плита: кольца
a=np.zeros((N,N,4)); a[...,3]=255; a[...,:3]=hexc('#0a0618')
g=hexc('#b070ff')
r=np.hypot(xx-7.5,yy-7.5)
m=np.maximum(np.clip(1-np.abs(r-6)*1.2,0,1),np.clip(1-np.abs(r-3)*1.4,0,1)*0.8)
m=np.maximum(m,(r<1.2)*1.0)
for i in range(3): a[...,i]=a[...,i]*(1-m)+g[i]*m+255*(r<1.2)*0.4
save(a,'block/rift_pad.png')
# луч: 64x16, по u — бегущие сгустки, по v — гаусс; тайлится по u
W,H=64,16
u=np.arange(W)[None,:]; v=np.arange(H)[:,None]
prof=np.exp(-((v-7.5)/4.2)**2)
flow=0.55+0.25*np.sin(u/W*2*np.pi*2)+0.2*np.sin(u/W*2*np.pi*5+1.3)
streak=np.clip(np.sin(u/W*2*np.pi*4 - (v-7.5)*0.0),0,1)**8*0.6
core=np.exp(-((v-7.5)/1.3)**2)
val=np.clip(prof*flow+core*0.6+streak*prof,0,1)
a=np.zeros((H,W,4)); a[...,0]=a[...,1]=a[...,2]=255*val; a[...,3]=255*val
save(a,'misc/rift_beam.png')
print('ok')
