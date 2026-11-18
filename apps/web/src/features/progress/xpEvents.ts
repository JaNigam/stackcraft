/** Tiny pub/sub so any feature can announce "+N XP" without knowing who shows the toast. */
type Listener = (xp: number, label: string) => void;
const listeners = new Set<Listener>();

export function announceXp(xp: number, label: string) {
  if (xp > 0) listeners.forEach((l) => l(xp, label));
}

export function onXp(l: Listener): () => void {
  listeners.add(l);
  return () => listeners.delete(l);
}
