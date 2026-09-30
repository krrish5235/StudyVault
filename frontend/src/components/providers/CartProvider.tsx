"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";

export interface CartItem {
  productId: string;
  slug: string;
  title: string;
  priceMinor: number;
  coverImageKey: string | null;
}

interface CartContextValue {
  items: CartItem[];
  add: (item: CartItem) => void;
  remove: (productId: string) => void;
  clear: () => void;
  has: (productId: string) => boolean;
  totalMinor: number;
}

const CartContext = createContext<CartContextValue | null>(null);
const STORAGE_KEY = "studyvault.cart.v1";

/**
 * Cart is a UI convenience only. Prices and eligibility are recomputed
 * server-side at checkout - the cart is never authoritative.
 */
export function CartProvider({ children }: { children: React.ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([]);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) setItems(JSON.parse(raw) as CartItem[]);
    } catch {
      /* corrupt cart - start empty */
    }
    setHydrated(true);
  }, []);

  useEffect(() => {
    if (hydrated) localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  }, [items, hydrated]);

  const add = useCallback((item: CartItem) => {
    setItems((prev) =>
      prev.some((p) => p.productId === item.productId) ? prev : [...prev, item]
    );
  }, []);

  const remove = useCallback((productId: string) => {
    setItems((prev) => prev.filter((p) => p.productId !== productId));
  }, []);

  const clear = useCallback(() => setItems([]), []);

  const has = useCallback(
    (productId: string) => items.some((p) => p.productId === productId),
    [items]
  );

  const totalMinor = useMemo(() => items.reduce((sum, i) => sum + i.priceMinor, 0), [items]);

  return (
    <CartContext.Provider value={{ items, add, remove, clear, has, totalMinor }}>
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error("useCart must be used within CartProvider");
  return ctx;
}
