# gp.py X Y [panelW panelH] -> экранные координаты GUI-точки внутри окна (масштаб 2, окно 854x480 со сдвигом 213,120)
import sys
x,y=float(sys.argv[1]),float(sys.argv[2]); pw=int(sys.argv[3]) if len(sys.argv)>3 else 0; ph=int(sys.argv[4]) if len(sys.argv)>4 else 0
left=(427-pw)//2 if pw else 0; top=(240-ph)//2 if ph else 0
print(f"M:{int(213+2*(left+x))},{int(120+2*(top+y))}")
