import type { Question } from '@/shared/api/types';
import { newAttemptId } from '@/shared/api/http';
import { Badge } from '@/shared/ui';
import { Markdownish } from '@/shared/ui/Markdownish';
import { useProgress, useSubmitAnswer } from '@/features/progress/hooks';
import { questionRenderers, typeLabel } from './registry';

const TIERS: { id: Question['tier']; title: string; blurb: string }[] = [
  { id: 'warmup', title: 'Warm-up', blurb: 'Auto-graded. Get these instantly right before moving on.' },
  { id: 'interview', title: 'Interview', blurb: 'What you will actually be asked. Predict before you peek.' },
  { id: 'killer', title: 'Killer', blurb: 'The follow-ups that separate senior from mid. Hints cost XP.' },
];

/** Container for one question: wires the renderer to the progress service with a fresh attemptId per submission. */
function QuestionItem({ chapterId, question, index }: { chapterId: string; question: Question; index: number }) {
  const submit = useSubmitAnswer(chapterId, question.id);
  const best = useProgress().data?.bestScores[`${chapterId}/${question.id}`];
  const Renderer = questionRenderers[question.type];
  return (
    <article className="qz-card" aria-labelledby={`q-${question.id}`}>
      <header className="qz-card__head">
        <span className="qz-num">{index}</span>
        <Badge tone="sky">{typeLabel[question.type]}</Badge>
        <Badge tone="amber">{question.xp} XP</Badge>
        {best !== undefined && <Badge tone={best >= 1 ? 'ok' : 'bad'}>{best >= 1 ? 'Done' : `Best ${Math.round(best * 100)}%`}</Badge>}
      </header>
      <div id={`q-${question.id}`} className="qz-prompt">
        <Markdownish text={question.prompt} />
      </div>
      <Renderer question={question} best={best} onSubmit={(answer) => submit.mutateAsync({ ...answer, attemptId: newAttemptId() })} />
    </article>
  );
}

export function QuestionList({ chapterId, questions }: { chapterId: string; questions: Question[] }) {
  let n = 0;
  return (
    <div className="qz-list">
      {TIERS.map((tier) => {
        const qs = questions.filter((q) => q.tier === tier.id);
        if (qs.length === 0) return null;
        return (
          <section key={tier.id} className={`qz-tier qz-tier--${tier.id}`}>
            <h3>
              {tier.title} <small>({qs.length})</small>
            </h3>
            <p className="qz-tier__blurb">{tier.blurb}</p>
            {qs.map((q) => (
              <QuestionItem key={q.id} chapterId={chapterId} question={q} index={++n} />
            ))}
          </section>
        );
      })}
    </div>
  );
}
