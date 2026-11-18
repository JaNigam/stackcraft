import type { AnswerResponse } from '@/shared/api/types';
import { Markdownish } from '@/shared/ui/Markdownish';

export function ResultBanner({ result, explanationLabel = 'Why' }: { result: AnswerResponse; explanationLabel?: string }) {
  const tone = result.correct ? 'ok' : result.score > 0 ? 'partial' : 'bad';
  return (
    <div className={`qz-result qz-result--${tone}`} role="status">
      <div className="qz-result__head">
        <strong>{result.feedback.split('\n')[0]}</strong>
        {result.xpAwarded > 0 && <span className="qz-xp">+{result.xpAwarded} XP</span>}
        {result.duplicate && <span className="qz-dup">already counted</span>}
      </div>
      {result.feedback.includes('\n') && <pre className="qz-result__pre">{result.feedback.split('\n').slice(1).join('\n')}</pre>}
      {result.explanation && (
        <div className="qz-result__why">
          <span className="qz-result__label">{explanationLabel}</span>
          <Markdownish text={result.explanation} />
        </div>
      )}
    </div>
  );
}
