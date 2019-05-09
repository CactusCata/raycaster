import tkinter
import time
from PIL import Image, ImageTk
from math import sin, cos
from typing import Tuple, Optional


class RaycasterWindow(tkinter.Tk):
    PIXEL_SCALE = 6
    PIXEL_WIDTH = 160
    PIXEL_HEIGHT = 100

    def __init__(self):
        super().__init__()
        self.perf_timestamp = time.monotonic()
        self.time_msec_epoch = int(time.monotonic() * 1000)
        self.raycaster = Raycaster(self.PIXEL_WIDTH, self.PIXEL_HEIGHT)
        self.imageTk = None
        self.resizable(0, 0)
        self.configure(borderwidth=self.PIXEL_SCALE, background="black")
        self.wm_title("pure Python raycaster")
        self.label = tkinter.Label(self, text="pixels", border=0)
        self.update_gui_image()
        self.label.pack()
        self.after(20, self.redraw)

    def update_gui_image(self):
        self.imageTk = ImageTk.PhotoImage(self.raycaster.image.resize(
            (self.PIXEL_WIDTH*self.PIXEL_SCALE, self.PIXEL_HEIGHT*self.PIXEL_SCALE), Image.NEAREST))
        self.label.configure(image=self.imageTk)
        self.update_idletasks()

    def redraw(self):
        self.raycaster.tick(int(time.monotonic() * 1000) - self.time_msec_epoch)
        self.update_gui_image()
        now = time.monotonic()
        fps = 1/(now - self.perf_timestamp)
        self.perf_timestamp = now
        self.wm_title(f"pure Python raycaster  -  {fps:.0f} fps")
        if fps < 30:
            self.after_idle(self.redraw)
        else:
            self.after(2, self.redraw)


class Texture:
    TEXTURE_SIZE = 64      # must be power of 2
    __TEX_SIZE_MASK = TEXTURE_SIZE-1

    def __init__(self, filename):
        with Image.open(filename) as img:
            if img.size != (self.TEXTURE_SIZE, self.TEXTURE_SIZE):
                raise IOError(f"texture {filename} is not {self.TEXTURE_SIZE}x{self.TEXTURE_SIZE}")
            self.pixels = []
            for x in range(self.TEXTURE_SIZE):
                column = [img.getpixel((x, y)) for y in range(self.TEXTURE_SIZE)]
                self.pixels.append(column)

    def get(self, x: float, y: float) -> Tuple[int, int, int]:
        return self.pixels[int(y) & self.__TEX_SIZE_MASK][int(x) & self.__TEX_SIZE_MASK]


class Raycaster:
    def __init__(self, pixwidth, pixheight):
        self.pixwidth = pixwidth
        self.pixheight = pixheight
        self.zbuffer = [[0.0] * pixheight for _ in range(pixwidth)]
        self.image = Image.new('RGB', (pixwidth, pixheight), color=0)
        self.textures = {
            "floor": Texture("floor.png"),
            "ceiling": Texture("ceiling.png"),
        }
        self.frame = 0

    def tick(self, walltime_msec: float) -> None:
        self.clear_zbuffer()
        self.frame += 1
        eye_height = self.pixheight * 2 // 3      # @todo real 3d coordinates
        for x in range(self.pixwidth):
            # draw ceiling
            tex = self.textures["ceiling"]
            for y in range(0, self.pixheight - eye_height):
                rgb = tex.get(x + walltime_msec//20, y)
                z = 1000*y/(self.pixheight - eye_height)        # @todo based on real 3d distance
                self.set_pixel((x, y), z, rgb)
            # draw floor
            tex = self.textures["floor"]
            for y in range(self.pixheight - eye_height, self.pixheight):
                rgb = tex.get(x - walltime_msec//20, y)
                z = 1000 * (self.pixheight - y) / eye_height     # @todo based on real 3d distance
                self.set_pixel((x, y), z, rgb)

    def clear_zbuffer(self) -> None:
        infinity = float("inf")
        for x in range(self.pixwidth):
            for y in range(self.pixheight):
                self.zbuffer[x][y] = infinity

    def set_pixel(self, xy: Tuple[int, int], z: float, rgb: Optional[Tuple[int, int, int]]) -> None:
        """Sets a pixel on the screen (if it is visible) and adjusts its z-buffer value.
        The pixel is darkened according to its z-value, the distance.
        If rgb is None, the pixel is transparent instead of having a color."""
        x, y = xy
        if z <= self.zbuffer[x][y]:
            if rgb:
                if z > 0:
                    bz = 1.0-min(1000.0, z)/1000.0        # @todo z=1000 is the distance of absolute black
                    rgb = self.rgb_brightness(rgb, bz)
                self.image.putpixel(xy, rgb)
            self.zbuffer[x][y] = z

    def rgb_brightness(self, rgb: Tuple[int, int, int], scale: float) -> Tuple[int, int, int]:
        """adjust brightness of the color. scale 0=black, 1=neutral, >1 = whiter. (clamped at 0..255)"""
        r, g, b = rgb
        return min(int(r*scale), 255), min(int(g*scale), 255), min(int(b*scale), 255)


window = RaycasterWindow()
window.mainloop()

