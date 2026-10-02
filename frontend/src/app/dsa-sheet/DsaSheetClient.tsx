"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import {
  Target, ChevronDown, ChevronUp, ExternalLink, Loader2, Flame,
  Trophy, UserRoundCheck, Search,
} from "lucide-react";
import { api } from "@/lib/api";
import { useSession } from "@/components/providers/SessionProvider";

interface Question {
  id: number;
  position: number;
  title: string;
  slug: string;
  difficulty: "EASY" | "MEDIUM" | "HARD";
  leetcodeUrl: string | null;
}

interface Topic {
  id: number;
  name: string;
  slug: string;
  questions: Question[];
}

interface Sheet {
  total: number;
  topics: Topic[];
}

const DIFF_STYLES: Record<string, string> = {
  EASY: "bg-mint-400/10 text-mint-400",
  MEDIUM: "bg-gold-500/10 text-gold-400",
  HARD: "bg-red-500/10 text-red-400",
};

export default function DsaSheetClient() {
  const { user } = useSession();
  const [sheet, setSheet] = useState<Sheet | null>(null);
  const [doneIds, setDoneIds] = useState<Set<number>>(new Set());
  const [openTopic, setOpenTopic] = useState<number | null>(null);
  const [filter, setFilter] = useState<"ALL" | "EASY" | "MEDIUM" | "HARD">("ALL");
  const [search, setSearch] = useState("");
  const [onlyUndone, setOnlyUndone] = useState(false);

  const load = useCallback(async () => {
    const s = await api.get<Sheet>("/api/v1/dsa-sheet");
    setSheet(s);
    setOpenTopic(s.topics[0]?.id ?? null);
    if (user) {
      try {
        const p = await api.get<{ doneIds: number[] }>("/api/v1/dsa-sheet/progress");
        setDoneIds(new Set(p.doneIds));
      } catch {
        setDoneIds(new Set());
      }
    } else {
      setDoneIds(new Set());
    }
  }, [user]);

  useEffect(() => {
    void load();
  }, [load]);

  const doneCount = useMemo(() => {
    if (!sheet) return 0;
    return sheet.topics.reduce(
      (sum, t) => sum + t.questions.filter((q) => doneIds.has(q.id)).length,
      0
    );
  }, [sheet, doneIds]);

  async function toggle(questionId: number) {
    if (!user) return;
    // optimistic
    setDoneIds((prev) => {
      const next = new Set(prev);
      if (next.has(questionId)) next.delete(questionId);
      else next.add(questionId);
      return next;
    });
    try {
      await api.post(`/api/v1/dsa-sheet/${questionId}/toggle`);
    } catch {
      setDoneIds((prev) => {
        const next = new Set(prev);
        if (next.has(questionId)) next.delete(questionId);
        else next.add(questionId);
        return next;
      });
    }
  }

  if (!sheet) {
    return <div className="flex items-center justify-center gap-2 py-32 text-slate-400"><Loader2 className="h-5 w-5 animate-spin" aria-hidden /> Loading the sheet…</div>;
  }

  const pct = Math.round((doneCount / sheet.total) * 100);
  const q = (text: string) => text.toLowerCase();

  return (
    <div className="mx-auto max-w-5xl px-4 py-10">
      {/* header */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="flex items-center gap-2 text-3xl font-black tracking-tight text-white">
            <Target className="h-7 w-7 text-accent-400" aria-hidden /> FAANG DSA 150
          </h1>
          <p className="mt-1 text-slate-400">
            150 hand-picked questions. Master these and you are interview-ready — nothing extra, nothing wasted.
          </p>
        </div>
        <span className="rounded-full border border-mint-400/40 bg-mint-400/10 px-4 py-1.5 text-sm font-bold text-mint-400">
          FREE
        </span>
      </div>

      {/* progress */}
      <div className="glass mt-6 rounded-2xl p-6">
        {user ? (
          <>
            <div className="flex items-center justify-between text-sm">
              <span className="flex items-center gap-2 font-semibold text-white">
                <Trophy className="h-5 w-5 text-gold-400" aria-hidden /> Your progress
              </span>
              <span className="text-slate-300">{doneCount} / {sheet.total} solved · {pct}%</span>
            </div>
            <div className="mt-3 h-3 overflow-hidden rounded-full bg-white/5" role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
              <div className="h-full rounded-full bg-gradient-to-r from-accent-500 to-mint-400 transition-all duration-500" style={{ width: `${Math.max(2, pct)}%` }} />
            </div>
          </>
        ) : (
          <div className="flex flex-wrap items-center justify-between gap-3">
            <p className="flex items-center gap-2 text-sm text-slate-300">
              <UserRoundCheck className="h-5 w-5 text-accent-400" aria-hidden />
              Create a free account to track your progress — checkboxes save automatically.
            </p>
            <Link href="/register?next=/dsa-sheet" className="btn-primary rounded-xl px-5 py-2.5 text-sm font-semibold text-white">
              Track my progress
            </Link>
          </div>
        )}
      </div>

      {/* filters */}
      <div className="mt-6 flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-48">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" aria-hidden />
          <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search questions…"
            aria-label="Search questions"
            className="w-full rounded-xl border border-white/10 bg-ink-800/60 py-2.5 pl-10 pr-4 text-sm text-white placeholder:text-slate-600" />
        </div>
        <div className="flex gap-1.5" role="group" aria-label="Filter by difficulty">
          {(["ALL", "EASY", "MEDIUM", "HARD"] as const).map((d) => (
            <button key={d} onClick={() => setFilter(d)}
              className={`rounded-lg px-3.5 py-2 text-xs font-bold transition ${
                filter === d ? "bg-accent-600 text-white" : "bg-white/5 text-slate-400 hover:text-white"}`}>
              {d}
            </button>
          ))}
        </div>
        {user && (
          <label className="flex cursor-pointer items-center gap-2 text-xs text-slate-400">
            <input type="checkbox" checked={onlyUndone} onChange={(e) => setOnlyUndone(e.target.checked)} />
            Unsolved only
          </label>
        )}
      </div>

      {/* topics */}
      <div className="mt-6 space-y-3">
        {sheet.topics.map((topic) => {
          const visible = topic.questions.filter((qq) =>
            (filter === "ALL" || qq.difficulty === filter)
            && (!search || q(qq.title).includes(q(search)))
            && (!onlyUndone || !user || !doneIds.has(qq.id)));
          if (visible.length === 0) return null;
          const topicDone = topic.questions.filter((qq) => doneIds.has(qq.id)).length;
          const isOpen = openTopic === topic.id;
          return (
            <div key={topic.id} className="glass card-glow overflow-hidden rounded-2xl">
              <button
                onClick={() => setOpenTopic(isOpen ? null : topic.id)}
                className="flex w-full items-center justify-between gap-4 px-5 py-4 text-left"
                aria-expanded={isOpen}
              >
                <div className="flex flex-1 items-center gap-3">
                  <Flame className="h-5 w-5 shrink-0 text-gold-400" aria-hidden />
                  <span className="font-semibold text-white">{topic.name}</span>
                  <span className="text-xs text-slate-500">{topic.questions.length} questions</span>
                </div>
                <div className="flex items-center gap-3">
                  {user && (
                    <span className={`text-xs font-bold ${topicDone === topic.questions.length ? "text-mint-400" : "text-slate-500"}`}>
                      {topicDone}/{topic.questions.length}
                    </span>
                  )}
                  {isOpen ? <ChevronUp className="h-5 w-5 text-slate-400" aria-hidden /> : <ChevronDown className="h-5 w-5 text-slate-400" aria-hidden />}
                </div>
              </button>
              {isOpen && (
                <ul className="divide-y divide-white/5 border-t border-white/5">
                  {visible.map((qq) => {
                    const isDone = doneIds.has(qq.id);
                    return (
                      <li key={qq.id} className="flex items-center gap-3 px-5 py-3 text-sm">
                        {user ? (
                          <button
                            onClick={() => void toggle(qq.id)}
                            aria-label={isDone ? `Mark ${qq.title} as unsolved` : `Mark ${qq.title} as solved`}
                            className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-md border transition ${
                              isDone ? "border-mint-400 bg-mint-400 text-ink-900" : "border-white/20 hover:border-accent-400"}`}
                          >
                            {isDone && "✓"}
                          </button>
                        ) : (
                          <span className="w-6 shrink-0 text-right font-mono text-xs text-slate-600">{qq.position}</span>
                        )}
                        <span className={`flex-1 ${isDone ? "text-slate-500 line-through" : "text-slate-200"}`}>
                          {qq.title}
                        </span>
                        <span className={`hidden rounded-full px-2.5 py-0.5 text-[11px] font-bold sm:inline ${DIFF_STYLES[qq.difficulty]}`}>
                          {qq.difficulty}
                        </span>
                        {qq.leetcodeUrl && (
                          <a href={qq.leetcodeUrl} target="_blank" rel="noopener noreferrer"
                            aria-label={`Open ${qq.title} on LeetCode`}
                            className="rounded-lg p-1.5 text-slate-500 hover:bg-white/5 hover:text-accent-400">
                            <ExternalLink className="h-4 w-4" aria-hidden />
                          </a>
                        )}
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>
          );
        })}
      </div>

      {/* 1:1 interview CTA */}
      <div className="glass mt-10 flex flex-wrap items-center justify-between gap-4 rounded-2xl p-6">
        <div>
          <h2 className="font-semibold text-white">Want one-on-one interview prep?</h2>
          <p className="mt-1 text-sm text-slate-400">
            Mock interviews, personalized feedback and a plan built around your target companies.
          </p>
        </div>
        <Link href="/contact" className="btn-primary rounded-xl px-6 py-3 text-sm font-semibold text-white">
          Book a 1:1 session →
        </Link>
      </div>
    </div>
  );
}
