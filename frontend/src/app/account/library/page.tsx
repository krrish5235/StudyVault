import { BookMarked } from "lucide-react";
import Link from "next/link";

export default function LibraryPage() {
  return (
    <div className="py-10 text-center">
      <BookMarked className="mx-auto h-12 w-12 text-slate-600" aria-hidden />
      <h1 className="mt-4 text-2xl font-bold text-white">My Library</h1>
      <p className="mx-auto mt-2 max-w-md text-slate-400">
        Products you purchase appear here with secure, always-available downloads.
      </p>
      <Link href="/products" className="mt-6 inline-block rounded-xl bg-accent-600 px-6 py-3 font-semibold text-white hover:bg-accent-500">
        Browse the catalog
      </Link>
    </div>
  );
}
