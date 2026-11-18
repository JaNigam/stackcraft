import { motion, useReducedMotion } from 'framer-motion';

export type CharacterKind = 'list' | 'set' | 'map' | 'queue' | 'blob' | 'tree';
export type Mood = 'happy' | 'smug' | 'surprised' | 'sad' | 'nope' | 'sleepy';

export const familyColor: Record<CharacterKind, string> = {
  list: 'var(--mint)',
  set: 'var(--lavender)',
  map: 'var(--amber)',
  queue: 'var(--sky)',
  blob: 'var(--pink)',
  tree: 'var(--mint)',
};

interface Props {
  kind: CharacterKind;
  mood?: Mood;
  label?: string;
  size?: number;
  color?: string;
  /** Change this value to make the character do a little hop (e.g. when something lands in it). */
  bump?: number | string;
}

const INK = '#2C2C2A';
const SW = 3.2;

function Body({ kind, fill }: { kind: CharacterKind; fill: string }) {
  const common = { fill, stroke: INK, strokeWidth: SW, strokeLinejoin: 'round' as const };
  switch (kind) {
    case 'list':
      return (
        <g>
          <rect x="10" y="26" width="100" height="74" rx="16" {...common} />
          <rect x="18" y="82" width="84" height="12" rx="4" fill="#fff" stroke={INK} strokeWidth={2.4} />
          {[39, 60, 81].map((x) => (
            <line key={x} x1={x} y1="82" x2={x} y2="94" stroke={INK} strokeWidth={2.4} />
          ))}
        </g>
      );
    case 'set':
      return (
        <g>
          <circle cx="60" cy="62" r="42" {...common} />
          <path d="M48 98 L60 92 L72 98 L72 108 L60 102 L48 108 Z" fill="var(--coral)" stroke={INK} strokeWidth={2.4} strokeLinejoin="round" />
        </g>
      );
    case 'map':
      return (
        <g>
          <rect x="14" y="24" width="84" height="78" rx="14" {...common} />
          <path d="M98 50 L110 50" stroke={INK} strokeWidth={SW} />
          <circle cx="112" cy="60" r="7" fill="#fff" stroke={INK} strokeWidth={2.4} />
          <path d="M112 67 L112 82 M112 76 L117 76" stroke={INK} strokeWidth={2.4} strokeLinecap="round" />
        </g>
      );
    case 'queue':
      return (
        <g>
          <rect x="8" y="34" width="96" height="62" rx="31" {...common} />
          <path d="M104 65 L116 65 M110 58 L117 65 L110 72" stroke={INK} strokeWidth={2.6} fill="none" strokeLinecap="round" strokeLinejoin="round" />
        </g>
      );
    case 'tree':
      return (
        <g>
          <path d="M60 18 L104 94 Q104 100 98 100 L22 100 Q16 100 16 94 Z" {...common} />
          <rect x="52" y="100" width="16" height="12" rx="3" fill="#b88a5a" stroke={INK} strokeWidth={2.4} />
        </g>
      );
    default:
      return <path d="M24 44 Q20 18 52 20 Q80 10 98 30 Q116 52 102 80 Q92 106 60 102 Q22 104 16 76 Q10 58 24 44 Z" {...common} />;
  }
}

function Face({ mood, cy }: { mood: Mood; cy: number }) {
  const eyeY = cy;
  const mouthY = cy + 20;
  const pupil: Record<Mood, [number, number]> = {
    happy: [0, 1.5],
    smug: [3, 2],
    surprised: [0, 0],
    sad: [0, 3],
    nope: [-2, 0],
    sleepy: [0, 3],
  };
  const [px, py] = pupil[mood];
  const eyeR = mood === 'surprised' ? 12 : 10.5;
  const mouths: Record<Mood, React.ReactNode> = {
    happy: <path d={`M47 ${mouthY - 2} Q60 ${mouthY + 11} 73 ${mouthY - 2}`} fill="#fff" stroke={INK} strokeWidth={2.8} strokeLinecap="round" strokeLinejoin="round" />,
    smug: <path d={`M50 ${mouthY} Q63 ${mouthY + 5} 73 ${mouthY - 4}`} fill="none" stroke={INK} strokeWidth={2.8} strokeLinecap="round" />,
    surprised: <ellipse cx="60" cy={mouthY + 1} rx="5" ry="7" fill={INK} />,
    sad: <path d={`M48 ${mouthY + 5} Q60 ${mouthY - 5} 72 ${mouthY + 5}`} fill="none" stroke={INK} strokeWidth={2.8} strokeLinecap="round" />,
    nope: <path d={`M50 ${mouthY + 2} L70 ${mouthY + 2}`} stroke={INK} strokeWidth={3} strokeLinecap="round" />,
    sleepy: <path d={`M54 ${mouthY + 1} Q60 ${mouthY + 5} 66 ${mouthY + 1}`} fill="none" stroke={INK} strokeWidth={2.6} strokeLinecap="round" />,
  };
  return (
    <g>
      {[44, 76].map((ex, i) => (
        <g key={ex}>
          <circle cx={ex} cy={eyeY} r={eyeR} fill="#fff" stroke={INK} strokeWidth={2.6} />
          <circle cx={ex + px} cy={eyeY + py} r={mood === 'surprised' ? 3.6 : 4.6} fill={INK} />
          <circle cx={ex + px + 1.6} cy={eyeY + py - 1.8} r={1.3} fill="#fff" />
          {(mood === 'smug' || mood === 'sleepy') && (
            <path d={`M${ex - eyeR} ${eyeY - 1} Q${ex} ${eyeY - eyeR - 3} ${ex + eyeR} ${eyeY - 1} Z`} fill="var(--lid, #e9dfcc)" stroke={INK} strokeWidth={2.4} />
          )}
          {mood === 'nope' && (
            <path d={i === 0 ? `M${ex - 10} ${eyeY - 16} L${ex + 8} ${eyeY - 11}` : `M${ex - 8} ${eyeY - 11} L${ex + 10} ${eyeY - 16}`} stroke={INK} strokeWidth={3} strokeLinecap="round" />
          )}
          {mood === 'sad' && (
            <path d={i === 0 ? `M${ex - 9} ${eyeY - 13} L${ex + 7} ${eyeY - 17}` : `M${ex - 7} ${eyeY - 17} L${ex + 9} ${eyeY - 13}`} stroke={INK} strokeWidth={2.6} strokeLinecap="round" />
          )}
        </g>
      ))}
      {mood === 'happy' && (
        <>
          <ellipse cx="30" cy={eyeY + 14} rx="6" ry="3.5" fill="var(--pink)" opacity="0.9" />
          <ellipse cx="90" cy={eyeY + 14} rx="6" ry="3.5" fill="var(--pink)" opacity="0.9" />
        </>
      )}
      {mouths[mood]}
    </g>
  );
}

const faceY: Record<CharacterKind, number> = { list: 50, set: 52, map: 52, queue: 58, blob: 54, tree: 66 };

/** Sticker-doodle character. Collections and objects get a body, googly eyes and a mood. */
export function Character({ kind, mood = 'happy', label, size = 112, color, bump }: Props) {
  const reduce = useReducedMotion();
  return (
    <figure className="k-character" style={{ width: size }}>
      <motion.svg
        key={String(bump ?? '')}
        viewBox="0 0 124 116"
        width={size}
        height={(size * 116) / 124}
        role="img"
        aria-label={`${label ?? kind}, looking ${mood}`}
        className="k-sticker"
        initial={reduce || bump === undefined ? false : { y: 0, scaleY: 1 }}
        animate={reduce || bump === undefined ? undefined : { y: [0, -10, 0], scaleY: [1, 1.05, 0.94, 1] }}
        transition={{ duration: 0.45, ease: 'easeOut' }}
      >
        <Body kind={kind} fill={color ?? familyColor[kind]} />
        <Face mood={mood} cy={faceY[kind]} />
      </motion.svg>
      {label && <figcaption className="k-character__label">{label}</figcaption>}
    </figure>
  );
}
