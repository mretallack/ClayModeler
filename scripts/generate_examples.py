#!/usr/bin/env python3
import struct
import math
from pathlib import Path
from zlib import crc32

class Vector3:
    def __init__(self, x, y, z):
        self.x, self.y, self.z = x, y, z
    
    def __add__(self, other):
        return Vector3(self.x + other.x, self.y + other.y, self.z + other.z)
    
    def __sub__(self, other):
        return Vector3(self.x - other.x, self.y - other.y, self.z - other.z)
    
    def __mul__(self, scalar):
        return Vector3(self.x * scalar, self.y * scalar, self.z * scalar)
    
    def __truediv__(self, scalar):
        return Vector3(self.x / scalar, self.y / scalar, self.z / scalar)
    
    def length(self):
        return math.sqrt(self.x**2 + self.y**2 + self.z**2)
    
    def normalize(self):
        l = self.length()
        return self / l if l > 0 else self
    
    def dot(self, other):
        return self.x * other.x + self.y * other.y + self.z * other.z
    
    def cross(self, other):
        return Vector3(
            self.y * other.z - self.z * other.y,
            self.z * other.x - self.x * other.z,
            self.x * other.y - self.y * other.x
        )

class Face:
    def __init__(self, v1, v2, v3):
        self.v1, self.v2, self.v3 = v1, v2, v3

class ClayModel:
    def __init__(self):
        self.vertices = []
        self.faces = []
        self.normals = []
        self.light_position = Vector3(2, 3, 2)
        self.light_intensity = 1.0
    
    def initialize(self, subdivisions=2):
        self.vertices.clear()
        self.faces.clear()
        self.normals.clear()
        
        t = (1 + math.sqrt(5)) / 2
        verts = [
            Vector3(-1, t, 0), Vector3(1, t, 0), Vector3(-1, -t, 0), Vector3(1, -t, 0),
            Vector3(0, -1, t), Vector3(0, 1, t), Vector3(0, -1, -t), Vector3(0, 1, -t),
            Vector3(t, 0, -1), Vector3(t, 0, 1), Vector3(-t, 0, -1), Vector3(-t, 0, 1)
        ]
        self.vertices = [v.normalize() for v in verts]
        
        face_indices = [
            (0, 11, 5), (0, 5, 1), (0, 1, 7), (0, 7, 10), (0, 10, 11),
            (1, 5, 9), (5, 11, 4), (11, 10, 2), (10, 7, 6), (7, 1, 8),
            (3, 9, 4), (3, 4, 2), (3, 2, 6), (3, 6, 8), (3, 8, 9),
            (4, 9, 5), (2, 4, 11), (6, 2, 10), (8, 6, 7), (9, 8, 1)
        ]
        self.faces = [Face(a, b, c) for a, b, c in face_indices]
        
        for _ in range(subdivisions):
            self._subdivide()
        
        self.recalculate_normals()
    
    def _subdivide(self):
        new_faces = []
        midpoint_cache = {}
        
        def get_midpoint(v1, v2):
            key = (min(v1, v2), max(v1, v2))
            if key not in midpoint_cache:
                mid = (self.vertices[v1] + self.vertices[v2]) / 2
                self.vertices.append(mid.normalize())
                midpoint_cache[key] = len(self.vertices) - 1
            return midpoint_cache[key]
        
        for face in self.faces:
            a = get_midpoint(face.v1, face.v2)
            b = get_midpoint(face.v2, face.v3)
            c = get_midpoint(face.v3, face.v1)
            
            new_faces.append(Face(face.v1, a, c))
            new_faces.append(Face(face.v2, b, a))
            new_faces.append(Face(face.v3, c, b))
            new_faces.append(Face(a, b, c))
        
        self.faces = new_faces
    
    def recalculate_normals(self):
        self.normals = [Vector3(0, 0, 0) for _ in self.vertices]
        
        for face in self.faces:
            v1 = self.vertices[face.v1]
            v2 = self.vertices[face.v2]
            v3 = self.vertices[face.v3]
            normal = (v2 - v1).cross(v3 - v1).normalize()
            
            self.normals[face.v1] = self.normals[face.v1] + normal
            self.normals[face.v2] = self.normals[face.v2] + normal
            self.normals[face.v3] = self.normals[face.v3] + normal
        
        self.normals = [n.normalize() for n in self.normals]
    
    def apply_remove_tool(self, hit_point, strength, radius):
        for i in range(len(self.vertices)):
            dist = (self.vertices[i] - hit_point).length()
            if dist < radius:
                falloff = 1 - (dist / radius)
                displacement = self.normals[i] * (-strength * falloff * 0.1)
                self.vertices[i] = self.vertices[i] + displacement
        self.recalculate_normals()
    
    def apply_add_tool(self, hit_point, strength, radius, direction=None):
        for i in range(len(self.vertices)):
            dist = (self.vertices[i] - hit_point).length()
            if dist < radius:
                falloff = 1 - (dist / radius)
                dir = direction if direction else self.normals[i]
                displacement = dir.normalize() * (strength * falloff * 0.1)
                self.vertices[i] = self.vertices[i] + displacement
        self.recalculate_normals()
    
    def apply_flatten_tool(self, hit_point, strength, radius, plane_normal):
        for i in range(len(self.vertices)):
            dist = (self.vertices[i] - hit_point).length()
            if dist < radius:
                falloff = 1 - (dist / radius)
                to_plane = (self.vertices[i] - hit_point).dot(plane_normal)
                projected = self.vertices[i] - plane_normal * to_plane
                self.vertices[i] = self.vertices[i] + (projected - self.vertices[i]) * (strength * falloff)
        self.recalculate_normals()
    
    def apply_pinch_tool(self, hit_point, strength, radius):
        for i in range(len(self.vertices)):
            dist = (self.vertices[i] - hit_point).length()
            if dist < radius:
                falloff = (1 - (dist / radius)**2)**2
                direction = (hit_point - self.vertices[i]).normalize()
                displacement = direction * (strength * falloff * 0.15)
                self.vertices[i] = self.vertices[i] + displacement
        self.recalculate_normals()

def save_model(model, filename, output_dir):
    filepath = Path(output_dir) / f"{filename}.clay"
    
    metadata = f"""name={filename}
created={int(1000000000)}
vertexCount={len(model.vertices)}
faceCount={len(model.faces)}
light_x={model.light_position.x}
light_y={model.light_position.y}
light_z={model.light_position.z}
light_intensity={model.light_intensity}
"""
    metadata_bytes = metadata.encode('utf-8')
    
    # Build all data first
    data_parts = [metadata_bytes]
    
    for v in model.vertices:
        data_parts.append(struct.pack('<fff', v.x, v.y, v.z))
    
    for face in model.faces:
        data_parts.append(struct.pack('<III', face.v1, face.v2, face.v3))
    
    for n in model.normals:
        data_parts.append(struct.pack('<fff', n.x, n.y, n.z))
    
    all_data = b''.join(data_parts)
    checksum = crc32(all_data) & 0xFFFFFFFF
    
    # Write file
    with open(filepath, 'wb') as f:
        f.write(struct.pack('<I', 0x434C4159))  # CLAY magic
        f.write(struct.pack('<I', 1))  # version
        f.write(struct.pack('<I', len(metadata_bytes)))
        f.write(struct.pack('<I', checksum))
        f.write(all_data)
    
    print(f"Generated: {filename}.clay ({len(model.vertices)} vertices, {len(model.faces)} faces)")

# Generate examples
output_dir = "app/src/main/assets/examples"

print("Generating sphere...")
sphere = ClayModel()
sphere.initialize(2)
save_model(sphere, "sphere", output_dir)

print("Generating cube...")
cube = ClayModel()
cube.initialize(3)
cube_points = [
    Vector3(0, 0, 1), Vector3(0, 0, -1),
    Vector3(1, 0, 0), Vector3(-1, 0, 0),
    Vector3(0, 1, 0), Vector3(0, -1, 0)
]
for point in cube_points:
    for _ in range(15):
        cube.apply_flatten_tool(point, 1.0, 1.0, point)
save_model(cube, "cube", output_dir)

print("Generating vase...")
vase = ClayModel()
vase.initialize(3)
# Flatten bottom
for _ in range(8):
    vase.apply_flatten_tool(Vector3(0, -1, 0), 1.0, 0.8, Vector3(0, -1, 0))
# Remove bottom to create base
for _ in range(6):
    vase.apply_remove_tool(Vector3(0, -0.9, 0), 0.8, 0.5)
# Pinch middle to create waist
for _ in range(10):
    vase.apply_pinch_tool(Vector3(0, -0.2, 0), 0.9, 0.7)
# Extend and widen top to create opening
for _ in range(8):
    vase.apply_add_tool(Vector3(0, 0.9, 0), 0.9, 0.4, Vector3(0, 1, 0))
# Flare out the opening
for _ in range(6):
    vase.apply_add_tool(Vector3(0.5, 0.8, 0), 0.8, 0.35, Vector3(1, 0.2, 0).normalize())
    vase.apply_add_tool(Vector3(-0.5, 0.8, 0), 0.8, 0.35, Vector3(-1, 0.2, 0).normalize())
    vase.apply_add_tool(Vector3(0, 0.8, 0.5), 0.8, 0.35, Vector3(0, 0.2, 1).normalize())
    vase.apply_add_tool(Vector3(0, 0.8, -0.5), 0.8, 0.35, Vector3(0, 0.2, -1).normalize())
# Hollow out the top opening
for _ in range(5):
    vase.apply_remove_tool(Vector3(0, 1.0, 0), 0.7, 0.4)
save_model(vase, "vase", output_dir)

print("Generating character...")
character = ClayModel()
character.initialize(3)
# Nose
for _ in range(6):
    character.apply_add_tool(Vector3(0, 0.2, 0.9), 0.9, 0.4, Vector3(0, 0, 1))
# Eyes
for _ in range(3):
    character.apply_pinch_tool(Vector3(-0.3, 0.4, 0.8), 0.9, 0.3)
    character.apply_pinch_tool(Vector3(0.3, 0.4, 0.8), 0.9, 0.3)
# Ears
for _ in range(4):
    character.apply_add_tool(Vector3(-0.9, 0, 0), 0.8, 0.4, Vector3(-1, 0, 0))
    character.apply_add_tool(Vector3(0.9, 0, 0), 0.8, 0.4, Vector3(1, 0, 0))
save_model(character, "character", output_dir)

print("Generating abstract...")
abstract = ClayModel()
abstract.initialize(3)
for _ in range(5):
    abstract.apply_add_tool(Vector3(0.7, 0.5, 0.3), 1.0, 0.6, Vector3(1, 0.5, 0.3).normalize())
for _ in range(4):
    abstract.apply_pinch_tool(Vector3(-0.6, -0.4, 0.5), 0.9, 0.5)
for _ in range(5):
    abstract.apply_add_tool(Vector3(0.3, -0.7, -0.4), 0.9, 0.5, Vector3(0.3, -1, -0.4).normalize())
for _ in range(4):
    abstract.apply_remove_tool(Vector3(-0.5, 0.6, -0.3), 0.8, 0.45)
for _ in range(5):
    abstract.apply_add_tool(Vector3(0, 0.9, 0.5), 0.9, 0.5, Vector3(0, 1, 0.5).normalize())
save_model(abstract, "abstract", output_dir)

print("\nAll examples generated!")
