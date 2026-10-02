"use client";

import { motion, useMotionValue, useSpring, useTransform } from "framer-motion";
import { useRef } from "react";

/**
 * 3D perspective tilt on hover - used for product cards and feature panels.
 * Disabled when the user prefers reduced motion.
 */
export default function Tilt({ children, className = "", max = 10 }: { children: React.ReactNode; className?: string; max?: number }) {
  const ref = useRef<HTMLDivElement>(null);
  const reduce = typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  const px = useMotionValue(0.5);
  const py = useMotionValue(0.5);
  const rotateX = useSpring(useTransform(py, [0, 1], [max, -max]), { stiffness: 260, damping: 24 });
  const rotateY = useSpring(useTransform(px, [0, 1], [-max, max]), { stiffness: 260, damping: 24 });

  if (reduce) {
    return <div className={className}>{children}</div>;
  }

  return (
    <div className={`tilt-wrap ${className}`}>
      <motion.div
        ref={ref}
        className="tilt"
        style={{ rotateX, rotateY }}
        onMouseMove={(e) => {
          const rect = ref.current?.getBoundingClientRect();
          if (!rect) return;
          px.set((e.clientX - rect.left) / rect.width);
          py.set((e.clientY - rect.top) / rect.height);
        }}
        onMouseLeave={() => {
          px.set(0.5);
          py.set(0.5);
        }}
      >
        {children}
      </motion.div>
    </div>
  );
}
