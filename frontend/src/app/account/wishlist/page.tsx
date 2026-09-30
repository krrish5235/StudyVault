import { Heart } from "lucide-react";
import Link from "next/link";

export default function WishlistPage() {
  return (
    <div className="py-10 text-center">
      <Heart className="mx-auto h-12 w-12 text-slate-600" aria-hidden />
      <h1 className="mt-4 text-2xl font-bold text-white">Wishlist</h1>
      <p className="mx-auto mt-2 max-w-md text-slate-400">Save products you want to come back to.</p>
      <Link href="/products" className="mt-6 inline-block rounded-xl bg-accent-600 px-6 py-3 font-semibold text-white hover:bg-accent-500">
        Browse the catalog
      </Link>
    </div>
  );
}
