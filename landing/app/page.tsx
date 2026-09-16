import Image from "next/image";
import Footer from "./components/Footer";
import IPhone from "./components/IPhone";
import {
  HomeMockup,
  GamesMockup,
  DrawMockup,
  GalleryMockup,
  MapMockup,
  JournalMockup,
  WidgetsMockup,
  CycleMockup,
} from "./components/Mockups";

const freePerks = [
  "Miss You, pairing & profile",
  "Shared gallery (up to 100 photos)",
  "Home & lock-screen widgets",
  "Draw together & mini-games",
  "Partner map (opt-in)",
  "Anniversaries & countdowns",
];

const premiumPerks = [
  "Unlimited photos & video (HD)",
  "Unlimited widgets & themes",
  "Live location + map widget",
  "Unlimited games & doodles",
  "Open-when letters & scheduled notes",
  "Bigger storage quota",
];

function AppStoreBadge({ className = "h-12" }: { className?: string }) {
  return (
    <a
      href="#"
      aria-label="Download on the App Store"
      className={`inline-block transition-transform hover:-translate-y-0.5 ${className}`}
    >
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src="/app-store-badge.svg"
        alt="Download on the App Store"
        className="h-full w-auto"
      />
    </a>
  );
}

function LeylaWordmark({ className = "" }: { className?: string }) {
  return (
    <span
      className={`text-[#ff6b6b] font-black leading-none ${className}`}
      style={{ fontFamily: '"Chopin Script", "Snell Roundhand", cursive' }}
    >
      Leyla
    </span>
  );
}

/* ─────────────────────────────  Feature block  ───────────────────────────── */

function Feature({
  eyebrow,
  title,
  body,
  mockup,
  reverse = false,
  bg = "bg-transparent",
}: {
  eyebrow: string;
  title: string;
  body: string;
  mockup: React.ReactNode;
  reverse?: boolean;
  bg?: string;
}) {
  return (
    <section className={bg}>
      <div
        className={`mx-auto max-w-6xl px-6 py-20 grid gap-12 md:grid-cols-2 items-center ${
          reverse ? "md:[&>*:first-child]:order-2" : ""
        }`}
      >
        <div className="flex justify-center">{mockup}</div>
        <div className="max-w-md">
          <span className="inline-block rounded-full bg-[#ff6b6b]/10 px-3 py-1 text-xs font-bold uppercase tracking-wide text-[#ff6b6b]">
            {eyebrow}
          </span>
          <h3 className="mt-4 text-3xl sm:text-4xl font-black tracking-tight text-[#2e2933]">
            {title}
          </h3>
          <p className="mt-4 text-lg text-neutral-600">{body}</p>
        </div>
      </div>
    </section>
  );
}

/* ────────────────────────────────  Page  ─────────────────────────────────── */

export default function Home() {
  return (
    <main className="relative text-[#2e2933] bg-gradient-to-b from-[#ffb0c0] via-white via-55% to-[#ffb0c0]">
      {/* Sticky nav */}
      <header className="sticky top-0 z-40 border-b border-black/5 bg-white/70 backdrop-blur-md">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-3">
          <div className="flex items-center gap-2">
            <Image src="/leyla-logo.png" alt="" width={28} height={28} className="rounded-md" />
            <LeylaWordmark className="text-3xl" />
          </div>
          <nav className="hidden md:flex items-center gap-8 text-sm font-semibold text-neutral-600">
            <a href="#features" className="hover:text-[#ff6b6b]">Features</a>
            <a href="#long-distance" className="hover:text-[#ff6b6b]">Long-distance</a>
            <a href="#pricing" className="hover:text-[#ff6b6b]">Pricing</a>
          </nav>
          <AppStoreBadge className="h-10" />
        </div>
      </header>

      {/* Hero */}
      <section className="relative overflow-hidden">
        {/* soft radial glows — kept subtle, same-family tones */}
        <div className="pointer-events-none absolute -left-24 -top-24 h-96 w-96 rounded-full bg-white/40 blur-3xl" />
        <div className="pointer-events-none absolute -right-24 top-40 h-96 w-96 rounded-full bg-white/30 blur-3xl" />

        <div className="relative mx-auto max-w-6xl px-6 pt-16 pb-24 grid gap-12 md:grid-cols-2 items-center">
          <div className="text-white">
            <p className="text-sm font-bold uppercase tracking-[0.2em] text-white/80">
              For two — and only two
            </p>
            <h1
              className="mt-4 text-8xl sm:text-9xl font-black tracking-tight drop-shadow-sm"
              style={{ fontFamily: '"Chopin Script", "Snell Roundhand", cursive' }}
            >
              Leyla
            </h1>
            <p className="mt-2 text-2xl sm:text-3xl font-semibold">Two people, one little world.</p>
            <p className="mt-6 max-w-lg text-lg text-white/90">
              A private app for couples — built for long-distance, lovely for close.
              Stay close with a tap, a photo, a glance at your widget.
            </p>
            <div className="mt-8 flex flex-wrap items-center gap-4">
              <AppStoreBadge className="h-14" />
              <div className="rounded-full bg-white/20 px-4 py-2 backdrop-blur-md">
                <p className="text-sm font-semibold text-white">
                  Free · Premium €2.99/mo
                </p>
              </div>
            </div>
            <div className="mt-8 flex items-center gap-3">
              <div className="flex -space-x-2">
                <div className="h-8 w-8 rounded-full border-2 border-white bg-gradient-to-br from-[#ffd9ba] to-[#ff8c92]" />
                <div className="h-8 w-8 rounded-full border-2 border-white bg-gradient-to-br from-[#c0e7ff] to-[#7a4bff]" />
                <div className="h-8 w-8 rounded-full border-2 border-white bg-gradient-to-br from-[#e6ffd6] to-[#5ac0a5]" />
              </div>
              <p className="text-sm text-white/90">Built quietly, only for the two of you.</p>
            </div>
          </div>

          <div className="relative flex justify-center md:justify-end">
            {/* Back phone (widgets) */}
            <div className="absolute -left-2 top-8 rotate-[-8deg] opacity-90 hidden sm:block">
              <IPhone width="w-[220px]">
                <WidgetsMockup />
              </IPhone>
            </div>
            {/* Front phone (home) */}
            <div className="relative z-10 rotate-[4deg]">
              <IPhone width="w-[280px]">
                <HomeMockup />
              </IPhone>
            </div>
          </div>
        </div>
      </section>

      {/* Feature intro */}
      <section id="features" className="mx-auto max-w-4xl px-6 py-24 text-center">
        <span className="inline-block rounded-full bg-[#ff6b6b]/10 px-4 py-1 text-xs font-bold uppercase tracking-wide text-[#ff6b6b]">
          Everything Leyla does
        </span>
        <h2 className="mt-6 text-4xl sm:text-5xl font-black tracking-tight">
          Small, frequent moments of{" "}
          <span
            className="text-[#ff6b6b]"
            style={{ fontFamily: '"Chopin Script", "Snell Roundhand", cursive' }}
          >
            closeness
          </span>
          .
        </h2>
        <p className="mt-5 text-lg text-neutral-500">
          Every screen is designed to feel native on iPhone — quiet, private,
          and built around the two of you.
        </p>
      </section>

      {/* Feature: Miss You */}
      <Feature
        eyebrow="Miss You"
        title="One tap. Straight to their home screen."
        body="A single button sends a warm little heart to your partner's phone — and their widget. It's small, it's specific, and it makes distance shrink."
        mockup={
          <IPhone>
            <HomeMockup />
          </IPhone>
        }
        bg="bg-transparent"
      />

      {/* Feature: Widgets */}
      <Feature
        eyebrow="Home & Lock-screen widgets"
        title="They're right there, all day."
        body="A photo widget, a distance counter, a countdown to your next reunion. Glance at your phone — see them. Long-press to swap themes any time."
        mockup={
          <IPhone>
            <WidgetsMockup />
          </IPhone>
        }
        reverse
      />

      {/* Feature: Games */}
      <Feature
        eyebrow="Play together"
        title="A whole room of tiny games."
        body="Daily questions. Quiz cards across 14 categories. Tic-Tac-Toe, HWDYKM, debates, and daily photo snaps — the little dares that keep your inside jokes going."
        mockup={
          <IPhone>
            <GamesMockup />
          </IPhone>
        }
        bg="bg-transparent"
      />

      {/* Feature: Draw */}
      <Feature
        eyebrow="Shared live canvas"
        title="Doodle in real time."
        body="Open a shared board and draw together — even a continent apart. Little hearts, silly faces, a rough map of tonight's plans. When you're done, send it."
        mockup={
          <IPhone>
            <DrawMockup />
          </IPhone>
        }
        reverse
      />

      {/* Feature: Map */}
      <Feature
        eyebrow="Partner map"
        title="Know where they are — only if you both want."
        body="Opt-in, private, and pausable anytime. See each other on Apple Maps with dual time zones and live distance. No third parties, ever."
        mockup={
          <IPhone>
            <MapMockup />
          </IPhone>
        }
        bg="bg-transparent"
      />

      {/* Feature: Gallery */}
      <Feature
        eyebrow="Shared gallery"
        title="Your own little photo album."
        body="A private feed of the two of you. Drop a photo and it lands on their widget. Save what matters — nothing goes to a public cloud."
        mockup={
          <IPhone>
            <GalleryMockup />
          </IPhone>
        }
        reverse
      />

      {/* Feature: Journal */}
      <Feature
        eyebrow="Journal & Open-when letters"
        title="Little notes, at the right moment."
        body="Write memories together, mark anniversaries and milestones, and leave 'open when' letters — for when they're sad, missing you, or celebrating."
        mockup={
          <IPhone>
            <JournalMockup />
          </IPhone>
        }
        bg="bg-transparent"
      />

      {/* Feature: Cycle */}
      <Feature
        eyebrow="Cycle — together, gently"
        title="Sync only what you choose."
        body="Track your cycle privately, and share only what you want with your partner. Two-way with Apple Health, so the numbers stay yours."
        mockup={
          <IPhone>
            <CycleMockup />
          </IPhone>
        }
        reverse
      />

      {/* Long distance */}
      <section id="long-distance" className="relative overflow-hidden bg-transparent">
        <div className="mx-auto max-w-5xl px-6 py-24 grid gap-12 md:grid-cols-2 items-center">
          <div>
            <span className="inline-block rounded-full bg-white/60 px-3 py-1 text-xs font-bold uppercase tracking-wide text-[#ff6b6b]">
              Made for long-distance
            </span>
            <h2 className="mt-4 text-4xl sm:text-5xl font-black tracking-tight">
              The miles feel smaller.
            </h2>
            <p className="mt-4 text-lg text-neutral-700">
              Dual time zones so you always know their morning from their night.
              Distance and a partner map. A big countdown to the next time you&apos;re
              together. And &ldquo;open when&rdquo; letters for the moments in between.
            </p>
            <div className="mt-6 grid grid-cols-3 gap-3 max-w-md">
              {[
                { n: "7,340", u: "km apart" },
                { n: "6h", u: "time diff" },
                { n: "7d", u: "till reunion" },
              ].map((s) => (
                <div key={s.u} className="rounded-2xl bg-white/70 backdrop-blur-md p-4 text-center shadow-sm">
                  <p className="text-2xl font-black text-[#ff6b6b]">{s.n}</p>
                  <p className="text-xs font-semibold text-neutral-500">{s.u}</p>
                </div>
              ))}
            </div>
          </div>
          <div className="relative flex justify-center">
            <div className="rotate-[-3deg]">
              <IPhone>
                <MapMockup />
              </IPhone>
            </div>
          </div>
        </div>
      </section>

      {/* Pricing */}
      <section id="pricing" className="mx-auto max-w-5xl px-6 py-24">
        <div className="text-center">
          <span className="inline-block rounded-full bg-[#ff6b6b]/10 px-4 py-1 text-xs font-bold uppercase tracking-wide text-[#ff6b6b]">
            Pricing
          </span>
          <h2 className="mt-4 text-4xl sm:text-5xl font-black tracking-tight">Completely freemium</h2>
          <p className="mt-3 text-lg text-neutral-500">
            Every feature is free — Premium just raises the limits.
          </p>
        </div>
        <div className="mt-14 grid gap-6 md:grid-cols-2">
          <div className="rounded-3xl border border-black/5 bg-white p-8 shadow-sm">
            <h3 className="text-xl font-bold">Free</h3>
            <p className="mt-1 text-4xl font-black">€0</p>
            <p className="mt-1 text-sm text-neutral-500">Everything you need to feel close.</p>
            <ul className="mt-6 space-y-3 text-neutral-700">
              {freePerks.map((p) => (
                <li key={p} className="flex gap-3">
                  <span className="text-[#ff6b6b] font-bold">✓</span>
                  {p}
                </li>
              ))}
            </ul>
          </div>
          <div className="relative rounded-3xl border-2 border-[#ff6b6b] bg-gradient-to-br from-white to-[#fff5f0] p-8 shadow-lg">
            <span className="absolute -top-3 left-8 rounded-full bg-[#ff6b6b] px-3 py-1 text-xs font-bold uppercase tracking-wide text-white">
              Best for two
            </span>
            <h3 className="text-xl font-bold">Premium</h3>
            <p className="mt-1 text-4xl font-black">
              €2.99
              <span className="text-lg font-medium text-neutral-400">/month</span>
            </p>
            <p className="mt-1 text-sm text-neutral-500">One plan covers both of you.</p>
            <ul className="mt-6 space-y-3 text-neutral-700">
              {premiumPerks.map((p) => (
                <li key={p} className="flex gap-3">
                  <span className="text-[#ff6b6b] font-bold">✓</span>
                  {p}
                </li>
              ))}
            </ul>
          </div>
        </div>
      </section>

      {/* Final CTA */}
      <section className="relative overflow-hidden bg-transparent">
        <div className="mx-auto max-w-4xl px-6 py-24 text-center text-white">
          <h2
            className="text-6xl sm:text-7xl font-black tracking-tight"
            style={{ fontFamily: '"Chopin Script", "Snell Roundhand", cursive' }}
          >
            Two people, one little world.
          </h2>
          <p className="mx-auto mt-6 max-w-lg text-lg text-white/90">
            Leyla is private by design and built for the long haul — from the first
            date to the tenth anniversary.
          </p>
          <div className="mt-10 flex flex-col items-center gap-4">
            <AppStoreBadge className="h-14" />
            <p className="text-sm text-white/80">Free — Premium €2.99/month.</p>
          </div>
        </div>
      </section>

      <Footer />
    </main>
  );
}
