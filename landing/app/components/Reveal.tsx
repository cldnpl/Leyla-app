"use client";

import { useEffect, useRef, type ReactNode } from "react";

/**
 * Scroll-reveal wrapper. Deliberately does not rely on IntersectionObserver
 * alone: in some environments (and for instant/anchor-link jumps straight to
 * a section) it never fires for elements that are already in view by the
 * time they're observed. This checks the element's own position directly on
 * scroll/resize (rAF-throttled) and also carries a hard timeout fallback, so
 * content can never get stuck invisible.
 */
export default function Reveal({
  children,
  delay = 0,
  className = "",
}: {
  children: ReactNode;
  delay?: number;
  className?: string;
}) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    let done = false;
    let ticking = false;

    const reveal = () => {
      if (done) return;
      done = true;
      el.classList.add("is-visible");
      window.removeEventListener("scroll", onScroll);
      window.removeEventListener("resize", onScroll);
      clearTimeout(fallback);
    };

    const check = () => {
      ticking = false;
      const rect = el.getBoundingClientRect();
      const viewportH = window.innerHeight || document.documentElement.clientHeight;
      if (rect.top < viewportH * 0.92 && rect.bottom > 0) reveal();
    };

    const onScroll = () => {
      if (ticking) return;
      ticking = true;
      requestAnimationFrame(check);
    };

    // Absolute safety net: never leave content invisible.
    const fallback = setTimeout(reveal, 1800);

    check();
    window.addEventListener("scroll", onScroll, { passive: true });
    window.addEventListener("resize", onScroll);

    return () => {
      window.removeEventListener("scroll", onScroll);
      window.removeEventListener("resize", onScroll);
      clearTimeout(fallback);
    };
  }, []);

  return (
    <div
      ref={ref}
      className={`reveal ${className}`}
      style={{ animationDelay: `${delay}ms` }}
    >
      {children}
    </div>
  );
}
