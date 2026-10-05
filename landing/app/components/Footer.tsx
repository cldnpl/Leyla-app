import Link from "next/link";

export default function Footer() {
  return (
    <footer className="relative border-t border-black/5 bg-white">
      <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-[#ff6b6b]/30 to-transparent" />
      <div className="mx-auto flex max-w-5xl flex-col items-center gap-6 px-6 py-12 sm:flex-row sm:justify-between">
        <div className="flex flex-col items-center gap-1 sm:items-start">
          <span className="font-script text-3xl text-[#ff6b6b]">Leyla</span>
          <span className="text-sm text-neutral-500">
            © {new Date().getFullYear()} · Two people, one little world.
          </span>
        </div>
        <nav className="flex items-center gap-6 text-sm text-neutral-500">
          <Link href="/privacy" className="transition hover:text-[#ff6b6b]">Privacy</Link>
          <Link href="/terms" className="transition hover:text-[#ff6b6b]">Terms</Link>
          <Link href="/support" className="transition hover:text-[#ff6b6b]">Support</Link>
        </nav>
      </div>
    </footer>
  );
}
