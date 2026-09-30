import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

/**
 * UX-level guard only: bounces requests with no session cookie. All real
 * authorization happens in the backend API on every call.
 */
export function middleware(request: NextRequest) {
  const hasSession = request.cookies.has("sv_session");
  if (!hasSession) {
    const login = new URL("/login", request.url);
    login.searchParams.set("next", request.nextUrl.pathname);
    return NextResponse.redirect(login);
  }
  return NextResponse.next();
}

export const config = {
  matcher: ["/account/:path*", "/checkout"],
};
