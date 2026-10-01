import type { ApiResponse } from "./types";

export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "";

export class ApiError extends Error {
  status: number;
  fieldErrors?: Record<string, string>;

  constructor(status: number, message: string, fieldErrors?: Record<string, string>) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

let csrfToken: string | null = null;

/** Fetches (or refreshes) the CSRF double-submit token from the backend. */
export async function ensureCsrf(): Promise<string> {
  const res = await fetch(`${API_URL}/api/v1/auth/csrf`, {
    credentials: "include",
    cache: "no-store",
  });
  const body = (await res.json()) as ApiResponse<{ token: string }>;
  csrfToken = body.data?.token ?? null;
  return csrfToken ?? "";
}

async function request<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const headers = new Headers(init.headers);
  const method = (init.method ?? "GET").toUpperCase();
  if (!headers.has("Content-Type") && init.body) {
    headers.set("Content-Type", "application/json");
  }
  if (["POST", "PUT", "PATCH", "DELETE"].includes(method)) {
    if (!csrfToken) await ensureCsrf();
    if (csrfToken) headers.set("X-XSRF-TOKEN", csrfToken);
  }

  const res = await fetch(`${API_URL}${path}`, {
    ...init,
    headers,
    credentials: "include",
  });

  if (res.status === 403 && retry && (await res.text()).includes("CSRF")) {
    await ensureCsrf();
    return request<T>(path, init, false);
  }

  let body: ApiResponse<T> | null = null;
  try {
    body = (await res.json()) as ApiResponse<T>;
  } catch {
    body = null;
  }

  if (!res.ok || !body?.success) {
    throw new ApiError(
      res.status,
      body?.message ?? "Something went wrong. Please try again.",
      body?.errors
    );
  }
  return body.data;
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, data?: unknown) =>
    request<T>(path, { method: "POST", body: data === undefined ? undefined : JSON.stringify(data) }),
  patch: <T>(path: string, data?: unknown) =>
    request<T>(path, { method: "PATCH", body: data === undefined ? undefined : JSON.stringify(data) }),
  delete: <T>(path: string) => request<T>(path, { method: "DELETE" }),
  /** Multipart file upload with CSRF handling. Returns the parsed JSON body. */
  upload: async <T>(path: string, file: File): Promise<T> => {
    const doUpload = async () => {
      if (!csrfToken) await ensureCsrf();
      const body = new FormData();
      body.append("file", file);
      const res = await fetch(`${API_URL}${path}`, {
        method: "POST",
        credentials: "include",
        headers: csrfToken ? { "X-XSRF-TOKEN": csrfToken } : {},
        body,
      });
      let parsed: ApiResponse<T> | null = null;
      try {
        parsed = (await res.json()) as ApiResponse<T>;
      } catch {
        parsed = null;
      }
      if (!res.ok || !parsed?.success) {
        throw new ApiError(res.status, parsed?.message ?? "Upload failed.", parsed?.errors);
      }
      return parsed.data;
    };
    try {
      return await doUpload();
    } catch (err) {
      // CSRF cookie may have expired - refresh once and retry.
      if (err instanceof ApiError && err.status === 403) {
        await ensureCsrf();
        return doUpload();
      }
      throw err;
    }
  },
  /** Raw DELETE/POST/PATCH for endpoints that return non-envelope bodies. */
  raw: async (path: string, method: "POST" | "PATCH" | "DELETE" = "POST") => {
    if (!csrfToken) await ensureCsrf();
    const res = await fetch(`${API_URL}${path}`, {
      method,
      credentials: "include",
      headers: { "X-XSRF-TOKEN": csrfToken ?? "" },
    });
    if (!res.ok) {
      let message = "Request failed.";
      try {
        const body = (await res.json()) as ApiResponse<never>;
        message = body.message ?? message;
      } catch {
        /* ignore */
      }
      throw new ApiError(res.status, message);
    }
    try {
      const body = (await res.json()) as ApiResponse<never>;
      return body;
    } catch {
      return null;
    }
  },
};
