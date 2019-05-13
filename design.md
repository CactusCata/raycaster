# Column based Raycaster

This is an attempt at a classic '2d'-ish raycaster, drawing the screen in pixel columns.
Think oldskool original Wolfenstein.

# textures

All textures (walls, ceiling, floors) are squares of 64x64 pixels.
Could be another power of 2, but settled on 64x64 considering the display size and
 the number of pixels the engine has to push.



# world coordinate system

The world is an infinite 2d plane (x,y) in *meter* units (or 'squares' if you wish)
(x,y) are coordinates on the ground plane, the fictional z coordinate
is the height above the ground.
It uses the regular mathematical axis orientation so the positive X axis is to the right,
and the positive Y axis is up.  This usually means the (0, 0) word map coordinate
corresponds to the bottom left corner in a minimap display on the screen.

In describing the world, the Z coordinate is not used at all because the ray caster
is only able to draw corridors that all have the same floor level and height.


# camera coordinates and viewing angle

The camera is the only thing that has a 3d coordinate in the world (x,y,z):
the position in the world is (x,y) and the Z coordinate is the height of the camera
above the ground level, in meters.
To simplify rendering, we assume the camera is at half height so that
the ceiling and floor are the same sizes and perspective.

Where the camera is pointing to is given by another -normalized- 2d vector (x,y). 
The implicit Z axis of the viewing angle vector is always zero because
it is not possible to look up or down. You can only rotate the camera horizontally. 
