import Footer from "./components/Footer";
import Reveal from "./components/Reveal";
import AppStoreBadge from "./components/AppStoreBadge";

const features = [
  { icon: "💜", title: "Miss You", body: "One tap sends a little heart to your partner's phone — and their home screen widget." },
  { icon: "📸", title: "Photo to widget", body: "Send a photo straight to your partner's home screen. A glance, and they're there." },
  { icon: "🗺️", title: "Partner map", body: "See where your partner is on Apple Maps — opt-in, private, and pausable anytime." },
  { icon: "🎨", title: "Draw & play together", body: "A shared live canvas and mini-games to play across any distance." },
  { icon: "⏳", title: "Countdowns", body: "Days together, anniversaries, and a countdown to your next reunion." },
  { icon: "💌", title: "Open-when letters", body: "Leave notes your partner opens when they're sad, missing you, or celebrating." },
];

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

function Eyebrow({ children }: { children: React.ReactNode }) {
  return (
    <span className="inline-flex items-center rounded-full bg-[#ff6b6b]/10 px-3 py-1 text-xs font-semibold uppercase tracking-wider text-[#e85c5c]">
      {children}
    </span>
  );
}

export default function Home() {
  return (
    <main>
      {/* Hero */}
      <section className="grain relative isolate overflow-hidden bg-gradient-to-br from-[#ffb5c2] via-[#ff6b6b] to-[#ffd9ba]">
        <div className="pointer-events-none absolute -left-24 top-36 h-72 w-72 rounded-full bg-white/15 blur-3xl" />
        <div className="pointer-events-none absolute -right-28 -top-20 h-96 w-96 rounded-full bg-[#fff4e8]/25 blur-3xl" />
        <div className="pointer-events-none absolute bottom-0 left-1/2 h-44 w-[38rem] -translate-x-1/2 rounded-full bg-[#ff4d65]/20 blur-3xl" />

        <div className="relative mx-auto max-w-6xl px-6 text-white">
          <nav className="flex items-center justify-between border-b border-white/20 py-5" aria-label="Main navigation">
            <a href="#top" className="font-script text-4xl leading-none">
              Leyla
            </a>
            <div className="hidden items-center gap-8 text-sm font-medium text-white/85 md:flex">
              <a className="transition hover:text-white" href="#features">Features</a>
              <a className="transition hover:text-white" href="#long-distance">Long-distance</a>
              <a className="transition hover:text-white" href="#pricing">Pricing</a>
            </div>
            <span className="hidden rounded-full border border-white/25 px-3 py-1 text-xs font-medium text-white/80 sm:inline-block">
              Built for two
            </span>
          </nav>

          <div id="top" className="grid min-h-[650px] items-center gap-14 py-14 lg:grid-cols-[1fr_0.85fr] lg:gap-20 lg:py-20">
            <div className="max-w-xl text-left">
              <h1 className="font-script text-8xl leading-[0.85] drop-shadow-sm sm:text-9xl">
                Leyla
              </h1>
              <p className="mt-5 max-w-lg text-3xl font-semibold leading-tight sm:text-4xl">Two people, one little world.</p>
              <p className="mt-6 max-w-xl text-lg leading-8 text-white/90">
                The private place for the tiny moments that make a relationship feel close — a tap, a photo, a glance at your widget.
              </p>

              <div className="mt-9 flex flex-wrap items-center gap-4">
                <AppStoreBadge />
                <span className="text-sm text-white/75">Free to use · Premium just €2.99/month</span>
              </div>

              <div className="mt-8 flex flex-wrap items-center gap-x-6 gap-y-3 text-sm text-white/80">
                <span className="flex items-center gap-2"><span className="text-base">💜</span> Made for two</span>
                <span className="flex items-center gap-2"><span className="text-base">🔒</span> Private by default</span>
                <span className="flex items-center gap-2"><span className="text-base">📱</span> Built for iPhone</span>
              </div>
            </div>

            <div className="relative flex items-center justify-center lg:justify-end">
              <div className="absolute h-80 w-80 rounded-full bg-white/20 blur-3xl" />
              <div className="absolute -bottom-6 left-1/2 h-10 w-48 -translate-x-1/2 rounded-full bg-black/25 blur-2xl" />

              <div className="animate-float absolute -left-2 top-14 hidden rounded-2xl border border-white/40 bg-white/90 px-4 py-3 text-left text-sm text-[#513a44] shadow-xl backdrop-blur sm:block lg:-left-12">
                <p className="font-semibold">Miss You sent 💜</p>
                <p className="mt-0.5 text-xs text-[#8f7780]">just now</p>
              </div>
              <div className="animate-float-delayed absolute -right-1 bottom-12 hidden rounded-2xl border border-white/40 bg-white/90 px-4 py-3 text-left text-sm text-[#513a44] shadow-xl backdrop-blur sm:block lg:-right-8">
                <p className="text-xs text-[#8f7780]">Next reunion</p>
                <p className="mt-0.5 font-bold">7 days ✈️</p>
              </div>

              <div className="relative w-[276px] rotate-2 rounded-[3.1rem] border-[7px] border-[#2e2630] bg-[#2e2630] p-1.5 shadow-2xl shadow-[#a83c4f]/40 transition duration-500 hover:rotate-0 sm:w-[304px]">
                <div className="relative overflow-hidden rounded-[2.55rem]">
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img
                    src="/screenshots/home.png"
                    alt="The Leyla app Home screen, showing a Miss You card and a live partner map"
                    width={1206}
                    height={2622}
                    className="block w-full"
                  />
                  <div className="pointer-events-none absolute inset-0 bg-gradient-to-tr from-transparent via-white/0 to-white/25" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="pointer-events-none absolute inset-x-0 bottom-0 h-20 bg-gradient-to-b from-transparent to-white/60" />
      </section>

      {/* Features */}
      <section id="features" className="mx-auto max-w-5xl scroll-mt-8 px-6 py-24">
        <Reveal className="flex flex-col items-center text-center">
          <Eyebrow>Features</Eyebrow>
          <h2 className="mt-4 text-3xl font-bold sm:text-4xl">Everything you need to feel close</h2>
          <p className="mt-3 max-w-xl text-neutral-500">
            Small, frequent moments of connection — designed to feel native on iPhone.
          </p>
        </Reveal>
        <div className="mt-14 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {features.map((f, i) => (
            <Reveal key={f.title} delay={i * 80}>
              <div className="group h-full rounded-3xl border border-black/5 bg-white p-7 shadow-sm transition duration-300 hover:-translate-y-1 hover:border-[#ff6b6b]/25 hover:shadow-lg hover:shadow-[#ff6b6b]/10">
                <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-[#ffe1e6] to-[#ffe9d6] text-2xl transition group-hover:scale-105">
                  {f.icon}
                </div>
                <h3 className="mt-5 text-lg font-semibold">{f.title}</h3>
                <p className="mt-2 text-neutral-500">{f.body}</p>
              </div>
            </Reveal>
          ))}
        </div>
      </section>

      {/* Long distance */}
      <section id="long-distance" className="relative scroll-mt-8 overflow-hidden bg-[#fff5f0]">
        <div className="pointer-events-none absolute -left-32 top-1/2 h-72 w-72 -translate-y-1/2 rounded-full bg-[#ffd9ba]/50 blur-3xl" />
        <div className="pointer-events-none absolute -right-24 bottom-0 h-64 w-64 rounded-full bg-[#ffb5c2]/40 blur-3xl" />

        <div className="relative mx-auto grid max-w-5xl items-center gap-10 px-6 py-24 md:grid-cols-2">
          <Reveal>
            <Eyebrow>Made for long-distance</Eyebrow>
            <h2 className="mt-4 text-3xl font-bold sm:text-4xl">The miles feel smaller.</h2>
            <p className="mt-4 text-lg text-neutral-600">
              Dual time zones so you always know their morning from their night. Distance and a
              partner map. A big countdown to the next time you&apos;re together. And &ldquo;open
              when&rdquo; letters for the moments in between.
            </p>
          </Reveal>
          <Reveal delay={120}>
            <div className="rounded-3xl bg-white p-8 shadow-xl shadow-[#ff9c8f]/10 ring-1 ring-black/5">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-neutral-400">Alex · 4:12 AM</p>
                  <p className="text-lg font-semibold">good night 💜</p>
                </div>
                <div className="text-right">
                  <p className="text-sm text-neutral-400">You · 11:12 PM</p>
                  <p className="text-lg font-semibold">good morning ☀️</p>
                </div>
              </div>
              <div className="mt-6 rounded-2xl bg-gradient-to-br from-[#ffb5c2] to-[#ffd9ba] p-6 text-center text-white shadow-inner">
                <p className="text-sm/relaxed opacity-90">Next reunion in</p>
                <p className="text-4xl font-black">7 days ✈️</p>
              </div>
            </div>
          </Reveal>
        </div>
      </section>

      {/* Pricing */}
      <section id="pricing" className="mx-auto max-w-5xl scroll-mt-8 px-6 py-24">
        <Reveal className="flex flex-col items-center text-center">
          <Eyebrow>Pricing</Eyebrow>
          <h2 className="mt-4 text-3xl font-bold sm:text-4xl">Completely freemium</h2>
          <p className="mt-3 max-w-xl text-neutral-500">
            Every feature is free — Premium just raises the limits.
          </p>
        </Reveal>
        <div className="mt-14 grid gap-8 md:grid-cols-2">
          <Reveal>
            <div className="flex h-full flex-col rounded-3xl border border-black/5 bg-white p-8 shadow-sm">
              <h3 className="text-xl font-bold">Free</h3>
              <p className="mt-1 text-3xl font-black">€0</p>
              <ul className="mt-6 flex-1 space-y-3 text-neutral-600">
                {freePerks.map((p) => (
                  <li key={p} className="flex gap-3">
                    <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-[#ff6b6b]/10 text-xs font-bold text-[#ff6b6b]">✓</span>
                    {p}
                  </li>
                ))}
              </ul>
              <a
                href="https://apps.apple.com/app/leyla/id6793453891"
                target="_blank"
                rel="noreferrer"
                className="mt-8 inline-flex items-center justify-center rounded-full border border-black/10 px-5 py-3 text-sm font-semibold text-[#2e2933] transition hover:border-black/20 hover:bg-black/[0.03]"
              >
                Get started
              </a>
            </div>
          </Reveal>
          <Reveal delay={120}>
            <div className="relative flex h-full flex-col rounded-3xl bg-gradient-to-br from-[#ff6b6b] to-[#ff9d6b] p-[2px] shadow-xl shadow-[#ff6b6b]/25">
              <span className="absolute -top-3 right-8 rounded-full bg-[#2e2933] px-3 py-1 text-xs font-semibold text-white shadow-md">
                Best for two
              </span>
              <div className="flex h-full flex-col rounded-[calc(1.5rem-2px)] bg-white p-8">
                <h3 className="text-xl font-bold">Premium</h3>
                <p className="mt-1 text-3xl font-black">
                  €2.99<span className="text-lg font-medium text-neutral-400">/month</span>
                </p>
                <ul className="mt-6 flex-1 space-y-3 text-neutral-600">
                  {premiumPerks.map((p) => (
                    <li key={p} className="flex gap-3">
                      <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-[#ff6b6b] to-[#ff9d6b] text-xs font-bold text-white">✓</span>
                      {p}
                    </li>
                  ))}
                </ul>
                <a
                  href="https://apps.apple.com/app/leyla/id6793453891"
                  target="_blank"
                  rel="noreferrer"
                  className="mt-8 inline-flex items-center justify-center rounded-full bg-gradient-to-br from-[#ff6b6b] to-[#ff9d6b] px-5 py-3 text-sm font-semibold text-white shadow-lg shadow-[#ff6b6b]/30 transition hover:-translate-y-0.5 hover:shadow-xl"
                >
                  Go Premium
                </a>
              </div>
            </div>
          </Reveal>
        </div>
      </section>

      <Footer />
    </main>
  );
}
