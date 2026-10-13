import localFont from "next/font/local";
import { Manrope } from "next/font/google";

export const bodyFont = Manrope({
  subsets: ["latin"],
  variable: "--body-font",
  display: "swap",
});

export const scriptFont = localFont({
  src: "./fonts/ChopinScript.otf",
  variable: "--script-font",
  display: "swap",
});
