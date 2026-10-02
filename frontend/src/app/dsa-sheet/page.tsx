import type { Metadata } from "next";
import DsaSheetClient from "./DsaSheetClient";

export const metadata: Metadata = {
  title: "FAANG DSA 150 Sheet — free, trackable interview prep",
  description:
    "150 hand-picked DSA questions that cover everything FAANG-style interviews ask: arrays, strings, trees, graphs, DP and more. Track your progress for free.",
  alternates: { canonical: "/dsa-sheet" },
};

export default function DsaSheetPage() {
  return <DsaSheetClient />;
}
