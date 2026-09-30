import Link from "next/link";
import { Star, Download, BadgeCheck } from "lucide-react";
import type { ProductSummary } from "@/lib/types";
import { formatINR } from "@/lib/format";

export default function ProductCard({ product }: { product: ProductSummary }) {
  return (
    <Link
      href={`/products/${product.slug}`}
      className="group glass flex flex-col overflow-hidden rounded-2xl transition-transform duration-200 hover:-translate-y-1 focus-visible:-translate-y-1"
    >
      <div className="relative h-40 overflow-hidden bg-gradient-to-br from-ink-700 via-ink-800 to-ink-900">
        {product.coverImageKey ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={`/api/v1/files/public/${product.coverImageKey}`}
            alt={`${product.title} cover`}
            className="h-full w-full object-cover"
            loading="lazy"
          />
        ) : (
          <div className="flex h-full items-center justify-center">
            <span className="select-none text-5xl font-black text-white/10" aria-hidden>
              {product.title.slice(0, 2).toUpperCase()}
            </span>
          </div>
        )}
        <div className="absolute left-3 top-3 flex gap-2">
          {product.featured && (
            <span className="flex items-center gap-1 rounded-full bg-gold-500/90 px-2 py-0.5 text-[11px] font-bold text-ink-900">
              <BadgeCheck className="h-3 w-3" aria-hidden /> Featured
            </span>
          )}
          {product.bestseller && (
            <span className="flex items-center gap-1 rounded-full bg-accent-500/90 px-2 py-0.5 text-[11px] font-bold text-white">
              <Download className="h-3 w-3" aria-hidden /> Bestseller
            </span>
          )}
        </div>
      </div>

      <div className="flex flex-1 flex-col gap-2 p-4">
        <p className="text-xs font-medium uppercase tracking-wide text-accent-400">{product.categoryName}</p>
        <h3 className="font-semibold leading-snug text-white group-hover:text-accent-400">{product.title}</h3>
        <p className="line-clamp-2 text-sm text-slate-400">{product.shortDescription}</p>

        <div className="mt-auto flex items-center justify-between pt-2">
          <div>
            {product.reviewCount > 0 ? (
              <span className="flex items-center gap-1 text-sm text-gold-400">
                <Star className="h-4 w-4 fill-gold-400" aria-hidden /> {product.avgRating.toFixed(1)}
                <span className="text-xs text-slate-500">({product.reviewCount})</span>
              </span>
            ) : (
              <span className="text-xs text-slate-500">New</span>
            )}
          </div>
          <div className="text-right">
            {product.comparePriceMinor != null && product.comparePriceMinor > product.priceMinor && (
              <span className="mr-2 text-xs text-slate-500 line-through">{formatINR(product.comparePriceMinor)}</span>
            )}
            <span className="text-lg font-bold text-white">{formatINR(product.priceMinor)}</span>
          </div>
        </div>
      </div>
    </Link>
  );
}
