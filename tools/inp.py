import sys, time
from Xlib import X, display, XK
from Xlib.ext import xtest
d = display.Display(':99')
def key(name, hold=0.08):
    kc = d.keysym_to_keycode(XK.string_to_keysym(name))
    xtest.fake_input(d, X.KeyPress, kc); d.sync(); time.sleep(hold)
    xtest.fake_input(d, X.KeyRelease, kc); d.sync()
def click(btn=1, hold=0.08):
    xtest.fake_input(d, X.ButtonPress, btn); d.sync(); time.sleep(hold)
    xtest.fake_input(d, X.ButtonRelease, btn); d.sync()
def move(dx, dy):
    xtest.fake_input(d, X.MotionNotify, True, x=dx, y=dy); d.sync()
def moveto(x, y):
    xtest.fake_input(d, X.MotionNotify, False, x=x, y=y); d.sync()
for cmd in sys.argv[1:]:
    op, _, arg = cmd.partition(':')
    if op == 'k': key(arg)
    elif op == 'h': n, t = arg.split(','); key(n, float(t))
    elif op == 'c': click(int(arg or 1))
    elif op == 'm': a, b = arg.split(','); move(int(a), int(b))
    elif op == 'M': a, b = arg.split(','); moveto(int(a), int(b))
    elif op == 's': time.sleep(float(arg))
    elif op == 'S':
        kc = d.keysym_to_keycode(XK.string_to_keysym('Shift_L'))
        xtest.fake_input(d, X.KeyPress, kc); d.sync(); time.sleep(0.05)
        click(1); time.sleep(0.05)
        xtest.fake_input(d, X.KeyRelease, kc); d.sync()
