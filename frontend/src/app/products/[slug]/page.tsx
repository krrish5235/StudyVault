import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Star, ShieldCheck, FileDown, RefreshCcw } from "lucide-react";
import ProductCard from "@/components/product/ProductCard";
import BuyPanel from "./BuyPanel";
import { api, API_URL } from "@/lib/api";
import { formatINR } from "@/lib/format";
import type { ProductDetail, ProductSummary, Page } from "@/lib/types";

export const revalidate = 30;

async function loadProduct(slug: string): Promise<ProductDetail | null> {
  try {
    return await api.get<ProductDetail>(`/api/v1/products/${encodeURIComponent(slug)}`);
  } catch {
    return null;
  }
}

export async function generateMetadata({ params }: { params: Promise<{ slug: string }> }): Promise<Metadata> {
  const { slug } = await params;
  const product = await loadProduct(slug);
  if (!product) return { title: "Product not found" };
  return {
    title: product.title,
    description: product.shortDescription,
    alternates: { canonical: `/products/${product.slug}` },
    openGraph: {
      title: product.title,
      description: product.shortDescription,
      type: "website",
    },
  };
}

export default async function ProductPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const product = await loadProduct(slug);
  if (!product) notFound();

  const related = await api
    .get<Page<ProductSummary>>(`/api/v1/products?category=${product.categorySlug}&size=5`)
    .catch(() => null);

  const jsonLd = {
    "@context": "https://schema.org",
    "@type": "Product",
    name: product.title,
    description: product.shortDescription,
    category: product.categoryName,
    offers: {
      "@type": "Offer",
      priceCurrency: product.currency,
      price: (product.priceMinor / 100).toFixed(2),
      availability: "https://schema.org/InStock",
    },
    ...(product.reviewCount > 0
      ? {
          aggregateRating: {
            "@type": "AggregateRating",
            ratingValue: product.avgRating,
            reviewCount: product.reviewCount,
          },
        }
      : {}),
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-10">
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />

      <nav className="mb-6 text-sm text-slate-400" aria-label="Breadcrumb">
        <Link href="/products" className="hover:text-white">Catalog</Link>
        <span className="mx-2">/</span>
        <Link href={`/products?category=${product.categorySlug}`} className="hover:text-white">
          {product.categoryName}
        </Link>
      </nav>

      <div className="grid gap-10 lg:grid-cols-[1fr_380px]">
        <div>
          <div className="flex flex-wrap gap-2">
            {product.tags.map((t) => (
              <span key={t} className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs text-slate-300">
                {t}
              </span>
            ))}
          </div>
          <h1 className="mt-4 text-3xl font-black tracking-tight text-white md:text-4xl">{product.title}</h1>
          <p className="mt-3 text-lg text-slate-300">{product.shortDescription}</p>

          {product.reviewCount > 0 && (
            <p className="mt-3 flex items-center gap-1.5 text-gold-400">
              <Star className="h-5 w-5 fill-gold-400" aria-hidden />
              <span className="font-semibold">{product.avgRating.toFixed(1)}</span>
              <span className="text-sm text-slate-500">({product.reviewCount} reviews)</span>
            </p>
          )}

          <div className="prose prose-invert mt-8 max-w-none text-slate-300">
            {product.description.split("\n").filter(Boolean).map((para, i) => (
              <p key={i} className="mb-3 leading-relaxed">{para}</p>
            ))}
          </div>
        </div>

        <aside className="lg:sticky lg:top-24 lg:self-start">
          <div className="glass rounded-2xl p-6">
            <div className="flex items-baseline gap-3">
              <span className="text-4xl font-black text-white">{formatINR(product.priceMinor)}</span>
              {product.comparePriceMinor != null && product.comparePriceMinor > product.priceMinor && (
                <span className="text-lg text-slate-500 line-through">{formatINR(product.comparePriceMinor)}</span>
              )}
            </div>
            <p className="mt-1 text-xs text-slate-500">
              One-time payment · Lifetime access · Version {product.contentVersion}
            </p>
            <BuyPanel product={product} />
            <ul className="mt-6 space-y-3 text-sm text-slate-300">
              <li className="flex items-center gap-2">
                <FileDown className="h-4 w-4 text-accent-400" aria-hidden /> Instant secure download
              </li>
              <li className="flex items-center gap-2">
                <RefreshCcw className="h-4 w-4 text-accent-400" aria-hidden /> Free future updates
              </li>
              <li className="flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-accent-400" aria-hidden /> Verified purchaser reviews only
              </li>
            </ul>
          </div>
        </aside>
      </div>

      {related && related.content.filter((p) => p.slug !== product.slug).length > 0 && (
        <section className="mt-16">
          <h2 className="mb-6 text-2xl font-bold text-white">More in {product.categoryName}</h2>
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {related.content.filter((p) => p.slug !== product.slug).slice(0, 4).map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
