import type { Metadata } from "next";
import { bodyFont, scriptFont } from "./fonts";
import "./globals.css";

export const metadata: Metadata = {
  title: "Leyla — Two people, one little world",
  description:
    "A private app for couples, built for long-distance and close relationships. Miss You, shared photos, partner map, countdowns, and more. Free — Premium just €2.99/mo.",
  openGraph: {
    title: "Leyla — Two people, one little world",
    description:
      "A private app for couples. Miss You, shared gallery, partner map, countdowns and more.",
    type: "website",
  },
};

export const viewport = {
  themeColor: "#ff6b6b",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className={`${bodyFont.variable} ${scriptFont.variable}`}>
      <body className="font-sans">{children}</body>
    </html>
  );
}
