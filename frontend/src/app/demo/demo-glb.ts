/**
 * MODO DEMONSTRAÇÃO — gera um GLB 2.0 válido (um prato com uma porção) em memória,
 * para exercitar o visualizador 3D sem depender de arquivos externos.
 * Unidades em metros (padrão glTF): prato com 28 cm de diâmetro.
 */
interface Part { positions: number[]; normals: number[]; indices: number[]; color: [number, number, number, number]; }

export function createDemoPlateGlb(): ArrayBuffer {
  const plate = cylinder(0.14, 0.018, 48, [0.97, 0.96, 0.94, 1]);
  const food = dome(0.085, 0.05, 0.018, 32, 12, [0.93, 0.62, 0.22, 1]);
  return buildGlb([plate, food]);
}

function cylinder(radius: number, height: number, segments: number, color: Part['color']): Part {
  const positions: number[] = [];
  const normals: number[] = [];
  const indices: number[] = [];
  // Topo e base (leque) + lateral
  for (const [y, ny] of [[height, 1], [0, -1]] as const) {
    const center = positions.length / 3;
    positions.push(0, y, 0); normals.push(0, ny, 0);
    for (let i = 0; i <= segments; i++) {
      const a = (i / segments) * Math.PI * 2;
      positions.push(Math.cos(a) * radius, y, Math.sin(a) * radius); normals.push(0, ny, 0);
    }
    for (let i = 1; i <= segments; i++) {
      if (ny > 0) indices.push(center, center + i + 1, center + i); else indices.push(center, center + i, center + i + 1);
    }
  }
  const side = positions.length / 3;
  for (let i = 0; i <= segments; i++) {
    const a = (i / segments) * Math.PI * 2;
    const x = Math.cos(a), z = Math.sin(a);
    positions.push(x * radius, 0, z * radius, x * radius, height, z * radius);
    normals.push(x, 0, z, x, 0, z);
  }
  for (let i = 0; i < segments; i++) {
    const b = side + i * 2;
    indices.push(b, b + 1, b + 3, b, b + 3, b + 2);
  }
  return { positions, normals, indices, color };
}

function dome(radius: number, height: number, baseY: number, segments: number, rings: number, color: Part['color']): Part {
  const positions: number[] = [];
  const normals: number[] = [];
  const indices: number[] = [];
  for (let r = 0; r <= rings; r++) {
    const phi = (r / rings) * (Math.PI / 2);
    const ringRadius = Math.cos(phi) * radius;
    const y = baseY + Math.sin(phi) * height;
    for (let s = 0; s <= segments; s++) {
      const a = (s / segments) * Math.PI * 2;
      const nx = Math.cos(phi) * Math.cos(a), ny = Math.sin(phi), nz = Math.cos(phi) * Math.sin(a);
      positions.push(Math.cos(a) * ringRadius, y, Math.sin(a) * ringRadius);
      const len = Math.hypot(nx, ny, nz) || 1;
      normals.push(nx / len, ny / len, nz / len);
    }
  }
  const row = segments + 1;
  for (let r = 0; r < rings; r++) {
    for (let s = 0; s < segments; s++) {
      const a = r * row + s, b = a + row;
      indices.push(a, b, a + 1, a + 1, b, b + 1);
    }
  }
  return { positions, normals, indices, color };
}

function buildGlb(parts: Part[]): ArrayBuffer {
  const chunks: ArrayBuffer[] = [];
  const bufferViews: object[] = [];
  const accessors: object[] = [];
  const primitives: object[] = [];
  const materials: object[] = [];
  let offset = 0;
  const push = (data: ArrayBuffer, target: number) => {
    bufferViews.push({ buffer: 0, byteOffset: offset, byteLength: data.byteLength, target });
    chunks.push(data);
    offset += data.byteLength;
    return bufferViews.length - 1;
  };

  parts.forEach((part, index) => {
    const pos = new Float32Array(part.positions);
    const min = [Infinity, Infinity, Infinity], max = [-Infinity, -Infinity, -Infinity];
    for (let i = 0; i < pos.length; i += 3) for (let k = 0; k < 3; k++) { min[k] = Math.min(min[k], pos[i + k]); max[k] = Math.max(max[k], pos[i + k]); }
    const posView = push(pos.buffer, 34962);
    accessors.push({ bufferView: posView, componentType: 5126, count: pos.length / 3, type: 'VEC3', min, max });
    const normView = push(new Float32Array(part.normals).buffer, 34962);
    accessors.push({ bufferView: normView, componentType: 5126, count: part.normals.length / 3, type: 'VEC3' });
    const idx = new Uint16Array(part.indices);
    const padded = new Uint16Array(idx.length + (idx.length % 2));
    padded.set(idx);
    const idxView = push(padded.buffer, 34963);
    accessors.push({ bufferView: idxView, componentType: 5123, count: idx.length, type: 'SCALAR' });
    materials.push({ pbrMetallicRoughness: { baseColorFactor: part.color, metallicFactor: 0, roughnessFactor: 0.6 } });
    primitives.push({ attributes: { POSITION: index * 3, NORMAL: index * 3 + 1 }, indices: index * 3 + 2, material: index });
  });

  const json = {
    asset: { version: '2.0', generator: 'Boraê modo demonstração' },
    scene: 0, scenes: [{ nodes: [0] }], nodes: [{ mesh: 0, name: 'Prato' }],
    meshes: [{ primitives }], materials, accessors, bufferViews, buffers: [{ byteLength: offset }]
  };
  const jsonBytes = new TextEncoder().encode(JSON.stringify(json));
  const jsonLength = Math.ceil(jsonBytes.length / 4) * 4;
  const binLength = Math.ceil(offset / 4) * 4;
  const total = 12 + 8 + jsonLength + 8 + binLength;
  const out = new ArrayBuffer(total);
  const view = new DataView(out);
  const bytes = new Uint8Array(out);
  view.setUint32(0, 0x46546c67, true); // "glTF"
  view.setUint32(4, 2, true);
  view.setUint32(8, total, true);
  view.setUint32(12, jsonLength, true);
  view.setUint32(16, 0x4e4f534a, true); // "JSON"
  bytes.set(jsonBytes, 20);
  bytes.fill(0x20, 20 + jsonBytes.length, 20 + jsonLength);
  const binStart = 20 + jsonLength;
  view.setUint32(binStart, binLength, true);
  view.setUint32(binStart + 4, 0x004e4942, true); // "BIN\0"
  let cursor = binStart + 8;
  for (const chunk of chunks) { bytes.set(new Uint8Array(chunk), cursor); cursor += chunk.byteLength; }
  return out;
}
