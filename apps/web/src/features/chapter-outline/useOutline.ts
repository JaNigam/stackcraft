import { useEffect, useState } from 'react';

export interface OutlineEntry {
  id: string;
  label: string;
}

/**
 * Collects the sections a rendered chapter registered with `data-outline` (theory h2s, the cheat card, question
 * tiers). Theory MDX loads lazily, so it watches the DOM until the entries appear. The last non-empty list is kept
 * when the theory tab unmounts, so the outline does not jump while you are on another tab.
 */
export function useOutlineEntries(rootSelector: string): OutlineEntry[] {
  const [entries, setEntries] = useState<OutlineEntry[]>([]);

  useEffect(() => {
    const read = () => {
      const found = [...document.querySelectorAll<HTMLElement>(`${rootSelector} [data-outline]`)]
        .filter((el) => el.id)
        .map((el) => ({ id: el.id, label: el.dataset.outline ?? el.textContent ?? '' }));
      if (found.length === 0) return;
      setEntries((prev) => (sameEntries(prev, found) ? prev : found));
    };
    read();
    const mo = new MutationObserver(read);
    mo.observe(document.body, { childList: true, subtree: true });
    return () => mo.disconnect();
  }, [rootSelector]);

  return entries;
}

function sameEntries(a: OutlineEntry[], b: OutlineEntry[]) {
  return a.length === b.length && a.every((e, i) => e.id === b[i].id && e.label === b[i].label);
}

/**
 * Scroll-spy: the section whose top has most recently scrolled past the sticky header is the current one. Reading
 * positions on scroll (rAF-throttled) is simpler and steadier than IntersectionObserver for tall, uneven sections.
 */
export function useActiveSection(ids: string[], offset = 140): string | undefined {
  const [active, setActive] = useState<string | undefined>(ids[0]);
  const key = ids.join('|');

  useEffect(() => {
    if (ids.length === 0) return;
    let frame = 0;
    const update = () => {
      frame = 0;
      let current: string | undefined;
      for (const id of ids) {
        const el = document.getElementById(id);
        if (el && el.getBoundingClientRect().top <= offset) current = id;
      }
      // At the very bottom of the page the last section is current even if its top never reached the line.
      if (window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 4) current = ids[ids.length - 1];
      setActive(current ?? ids[0]);
    };
    const onScroll = () => {
      if (!frame) frame = requestAnimationFrame(update);
    };
    update();
    window.addEventListener('scroll', onScroll, { passive: true });
    window.addEventListener('resize', onScroll);
    return () => {
      window.removeEventListener('scroll', onScroll);
      window.removeEventListener('resize', onScroll);
      if (frame) cancelAnimationFrame(frame);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, offset]);

  return active;
}

/**
 * Scrolls to an element once it exists and is visible (tab panels stay mounted but hidden, and theory loads
 * lazily, so the target may need a frame or two after the click). Gives up after ~2 s.
 */
export function scrollToWhenReady(id: string, smooth: boolean) {
  const started = performance.now();
  const tryNow = (): boolean => {
    const el = document.getElementById(id);
    if (!el || el.getClientRects().length === 0) return false;
    // Smooth only for short hops: animating across thousands of pixels feels slow, so long jumps are instant.
    const near = Math.abs(el.getBoundingClientRect().top) < window.innerHeight * 1.5;
    el.scrollIntoView({ behavior: smooth && near ? 'smooth' : 'auto', block: 'start' });
    return true;
  };
  if (tryNow()) return;
  const tick = () => {
    if (!tryNow() && performance.now() - started < 3000) requestAnimationFrame(tick);
  };
  requestAnimationFrame(tick);
}
