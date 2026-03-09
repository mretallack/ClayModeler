#!/usr/bin/env python3
"""
Visualize .clay files as 3D renders
"""
import struct
import json
import sys
from pathlib import Path

def parse_clay_file(filepath):
    """Parse a .clay file and return vertices and faces"""
    with open(filepath, 'rb') as f:
        # Read header
        magic = struct.unpack('<I', f.read(4))[0]
        if magic != 0x434C4159:  # "CLAY" in little-endian
            raise ValueError(f"Invalid magic number: {hex(magic)}")
        
        version = struct.unpack('<I', f.read(4))[0]
        metadata_size = struct.unpack('<I', f.read(4))[0]
        checksum = struct.unpack('<I', f.read(4))[0]
        
        # Read metadata (key=value format)
        metadata_bytes = f.read(metadata_size)
        metadata_str = metadata_bytes.decode('utf-8')
        metadata = {}
        for line in metadata_str.strip().split('\n'):
            if '=' in line:
                key, value = line.split('=', 1)
                metadata[key] = value
        
        vertex_count = int(metadata.get('vertexCount', 0))
        face_count = int(metadata.get('faceCount', 0))
        
        # Read vertices
        vertices = []
        for _ in range(vertex_count):
            x, y, z = struct.unpack('<fff', f.read(12))
            vertices.append((x, y, z))
        
        # Read faces
        faces = []
        for _ in range(face_count):
            v1, v2, v3 = struct.unpack('<III', f.read(12))
            faces.append((v1, v2, v3))
        
        return vertices, faces, metadata

def render_clay_to_image(clay_file, output_image):
    """Render a .clay file to a PNG image"""
    import matplotlib.pyplot as plt
    from mpl_toolkits.mplot3d import Axes3D
    from mpl_toolkits.mplot3d.art3d import Poly3DCollection
    import numpy as np
    
    vertices, faces, metadata = parse_clay_file(clay_file)
    
    # Create figure
    fig = plt.figure(figsize=(10, 10))
    ax = fig.add_subplot(111, projection='3d')
    
    # Create mesh
    verts = np.array(vertices)
    triangles = [[vertices[f[0]], vertices[f[1]], vertices[f[2]]] for f in faces]
    
    # Create collection
    mesh = Poly3DCollection(triangles, alpha=0.8, edgecolor='none')
    mesh.set_facecolor([0.82, 0.41, 0.12])  # Terracotta color
    ax.add_collection3d(mesh)
    
    # Set limits
    max_range = np.array([verts[:,0].max()-verts[:,0].min(),
                          verts[:,1].max()-verts[:,1].min(),
                          verts[:,2].max()-verts[:,2].min()]).max() / 2.0
    mid_x = (verts[:,0].max()+verts[:,0].min()) * 0.5
    mid_y = (verts[:,1].max()+verts[:,1].min()) * 0.5
    mid_z = (verts[:,2].max()+verts[:,2].min()) * 0.5
    ax.set_xlim(mid_x - max_range, mid_x + max_range)
    ax.set_ylim(mid_y - max_range, mid_y + max_range)
    ax.set_zlim(mid_z - max_range, mid_z + max_range)
    
    # Set view angle
    ax.view_init(elev=20, azim=45)
    
    # Labels
    name = metadata.get('name', Path(clay_file).stem)
    ax.set_title(f"{name}\n{len(vertices)} vertices, {len(faces)} faces", fontsize=14)
    ax.set_xlabel('X')
    ax.set_ylabel('Y')
    ax.set_zlabel('Z')
    
    # Save
    plt.tight_layout()
    plt.savefig(output_image, dpi=150, bbox_inches='tight')
    plt.close()
    print(f"Rendered {clay_file} -> {output_image}")

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: visualize_clay.py <clay_file> [output_image]")
        sys.exit(1)
    
    clay_file = sys.argv[1]
    output_image = sys.argv[2] if len(sys.argv) > 2 else clay_file.replace('.clay', '.png')
    
    render_clay_to_image(clay_file, output_image)
