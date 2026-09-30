"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Download, PackageOpen } from "lucide-react";
import { api, API_URL } from "@/lib/api";
import { formatDate } from "@/lib/format";

interface LibraryItem {
  productId: string;
  title: string;
  slug: string;
  categoryName: string;
  coverImageKey: string | null;
  contentVersion: number;
  grantedAt: string;
  downloadable: boolean;
}

export default function LibraryPage() {
  const [items, setItems] = useState<LibraryItem[] | null>(null);

  useEffect(() => {
    api.get<LibraryItem[]>("/api/v1/library").then(setItems).catch(() => setItems([]));
  }, []);

  return (
    <div>
      <h1 className="text-2xl font-bold text-white">My Library</h1>
      <p className="mt-1 text-slate-400">Everything you own, with secure downloads.</p>

      {items === null ? (
        <div className="mt-8 space-y-3" aria-busy="true">
          {Array.from({ length: 2 }).map((_, i) => (
            <div key={i} className="glass h-24 animate-pulse rounded-2xl" />
          ))}
        </div>
      ) : items.length === 0 ? (
        <div className="py-16 text-center">
          <PackageOpen className="mx-auto h-12 w-12 text-slate-600" aria-hidden />
          <p className="mt-4 text-slate-400">Your library is waiting for its first product.</p>
          <Link href="/products" className="mt-6 inline-block rounded-xl bg-accent-600 px-6 py-3 font-semibold text-white hover:bg-accent-500">
            Browse the catalog
          </Link>
        </div>
      ) : (
        <div className="mt-8 space-y-4">
          {items.map((item) => (
            <div key={item.productId} className="glass flex flex-wrap items-center gap-4 rounded-2xl p-5">
              <div className="flex h-14 w-20 items-center justify-center rounded-lg bg-gradient-to-br from-ink-700 to-ink-900 text-lg font-black text-white/20">
                {item.title.slice(0, 2).toUpperCase()}
              </div>
              <div className="flex-1">
                <Link href={`/products/${item.slug}`} className="font-semibold text-white hover:text-accent-400">
                  {item.title}
                </Link>
                <p className="text-sm text-slate-500">
                  {item.categoryName} · Version {item.contentVersion} · Purchased {formatDate(item.grantedAt)}
                </p>
              </div>
              {item.downloadable ? (
                <a
                  href={`${API_URL}/api/v1/library/${item.productId}/download`}
                  className="flex items-center gap-2 rounded-xl bg-accent-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-accent-500"
                >
                  <Download className="h-4 w-4" aria-hidden /> Download
                </a>
              ) : (
                <span className="text-sm text-slate-500">File coming soon</span>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
