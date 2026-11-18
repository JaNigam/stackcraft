import type { ReactNode } from 'react';
import { Character, type CharacterKind, type Mood } from './Character';

/** The analogy strip: a row of characters, each optionally saying something. */
export function Scene({ children, label }: { children: ReactNode; label?: string }) {
  return (
    <div className="k-scene" role="group" aria-label={label ?? 'Analogy scene'}>
      {children}
    </div>
  );
}

export function Actor({ kind, mood, label, says, size }: { kind: CharacterKind; mood?: Mood; label: string; says?: ReactNode; size?: number }) {
  return (
    <div className="k-actor">
      {says && <div className="k-speech">{says}</div>}
      <Character kind={kind} mood={mood} label={label} size={size ?? 96} />
    </div>
  );
}
