"use client";

import { useMemo, useRef } from "react";
import { Canvas, useFrame } from "@react-three/fiber";
import type { Group, Mesh } from "three";

/** Geometry-only 3D hero: floating "books" (boxes) + particle field. No external assets. */

function FloatingBook({ position, color, speed }: { position: [number, number, number]; color: string; speed: number }) {
  const ref = useRef<Mesh>(null);
  useFrame((state) => {
    const t = state.clock.elapsedTime;
    if (!ref.current) return;
    ref.current.rotation.x = t * 0.25 * speed;
    ref.current.rotation.y = t * 0.18 * speed;
    ref.current.position.y = position[1] + Math.sin(t * speed) * 0.35;
  });
  return (
    <mesh ref={ref} position={position}>
      <boxGeometry args={[1.1, 1.5, 0.22]} />
      <meshStandardMaterial color={color} roughness={0.35} metalness={0.25} />
    </mesh>
  );
}

function Particles({ count = 160 }: { count?: number }) {
  const ref = useRef<Group>(null);
  const positions = useMemo(() => {
    const arr = new Float32Array(count * 3);
    for (let i = 0; i < count * 3; i += 1) {
      arr[i] = (Math.random() - 0.5) * 16;
      arr[i + 1] = (Math.random() - 0.5) * 9;
      arr[i + 2] = (Math.random() - 0.5) * 8 - 2;
    }
    return arr;
  }, [count]);

  useFrame((state) => {
    if (ref.current) ref.current.rotation.y = state.clock.elapsedTime * 0.03;
  });

  return (
    <group ref={ref}>
      <points>
        <bufferGeometry>
          <bufferAttribute attach="attributes-position" args={[positions, 3]} />
        </bufferGeometry>
        <pointsMaterial size={0.035} color="#8fa0ff" transparent opacity={0.65} sizeAttenuation />
      </points>
    </group>
  );
}

export default function HeroScene() {
  return (
    <Canvas
      camera={{ position: [0, 0, 7], fov: 55 }}
      dpr={[1, 1.75]}
      gl={{ antialias: true, alpha: true, powerPreference: "low-power" }}
      aria-hidden="true"
    >
      <ambientLight intensity={0.7} />
      <directionalLight position={[4, 6, 5]} intensity={1.4} color="#c3ccff" />
      <directionalLight position={[-5, -3, -4]} intensity={0.4} color="#ffc861" />
      <FloatingBook position={[-3.4, 1.1, 0]} color="#5b6cff" speed={0.7} />
      <FloatingBook position={[3.2, -0.6, -1]} color="#f5a623" speed={0.55} />
      <FloatingBook position={[-2.1, -1.7, 1.2]} color="#34d399" speed={0.9} />
      <FloatingBook position={[2.4, 1.8, 0.8]} color="#8b5cf6" speed={0.6} />
      <Particles />
      <fog attach="fog" args={["#05070f", 9, 16]} />
    </Canvas>
  );
}
