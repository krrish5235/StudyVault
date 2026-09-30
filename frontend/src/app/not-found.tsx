import Link from "next/link";

export default function NotFound() {
  return (
    <div className="mx-auto max-w-xl px-4 py-28 text-center">
      <p className="text-7xl font-black text-accent-500/40">404</p>
      <h1 className="mt-4 text-2xl font-bold text-white">This page wandered off</h1>
      <p className="mt-2 text-slate-400">The link may be broken or the product may no longer exist.</p>
      <Link href="/" className="mt-6 inline-block rounded-xl bg-accent-600 px-6 py-3 font-semibold text-white hover:bg-accent-500">
        Back to home
      </Link>
    </div>
  );
}
