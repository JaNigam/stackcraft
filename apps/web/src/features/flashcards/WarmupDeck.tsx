import { useState } from 'react';
import { motion, useReducedMotion } from 'framer-motion';
import { newAttemptId } from '@/shared/api/http';
import { Button, Card, Badge } from '@/shared/ui';
import { useDueFlashcards, useReviewFlashcard } from '@/features/progress/hooks';

/** Daily spaced-repetition warm-up: cards from completed chapters, Leitner boxes 1-5. */
export function WarmupDeck() {
  const due = useDueFlashcards(5);
  const review = useReviewFlashcard();
  const [flipped, setFlipped] = useState(false);
  const [doneKeys, setDoneKeys] = useState<string[]>([]);
  const reduce = useReducedMotion();

  const cards = (due.data ?? []).filter((c) => !doneKeys.includes(c.key));
  const card = cards[0];

  if (due.isLoading) return null;
  if (!card) {
    return (
      <Card className="fc-empty">
        <strong>Warm-up</strong>
        <p>{doneKeys.length > 0 ? 'All caught up for today. See you tomorrow.' : 'Finish a lesson to unlock its flashcards. They come back on a schedule so old units never fade.'}</p>
      </Card>
    );
  }

  const answer = async (remembered: boolean) => {
    await review.mutateAsync({ attemptId: newAttemptId(), cardKey: card.key, remembered });
    setDoneKeys((d) => [...d, card.key]);
    setFlipped(false);
  };

  return (
    <section className="fc" aria-label="Flashcard warm-up">
      <header className="fc__head">
        <strong>Warm-up</strong> <Badge tone="mint">{cards.length} due</Badge> <Badge tone="lavender">Box {card.box}</Badge>
      </header>
      <motion.button
        type="button"
        key={card.key + String(flipped)}
        className={`fc__card${flipped ? ' is-back' : ''}`}
        onClick={() => setFlipped((f) => !f)}
        initial={reduce ? false : { rotateX: 70, opacity: 0.4 }}
        animate={{ rotateX: 0, opacity: 1 }}
        transition={{ duration: 0.22 }}
        aria-label={flipped ? 'Answer side. Tap to see the question.' : 'Question side. Tap to see the answer.'}
      >
        <span className="fc__side">{flipped ? 'Answer' : card.chapterTitle}</span>
        <span className="fc__text">{flipped ? card.back : card.front}</span>
      </motion.button>
      {flipped ? (
        <div className="qz-row">
          <Button onClick={() => answer(false)} disabled={review.isPending}>
            Forgot (back to box 1)
          </Button>
          <Button variant="primary" onClick={() => answer(true)} disabled={review.isPending}>
            Remembered +1 XP
          </Button>
        </div>
      ) : (
        <p className="fc__tip">Answer in your head, then tap the card.</p>
      )}
    </section>
  );
}
