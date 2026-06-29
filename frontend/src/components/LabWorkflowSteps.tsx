import type { ExamRequestStatus } from '../api/types';
import { examRequestStatusLabels } from '../utils/labDisplay';

const STEPS: ExamRequestStatus[] = ['PENDING', 'SPECIMEN_COLLECTED', 'RESULTS_AVAILABLE'];

type LabWorkflowStepsProps = {
  current: ExamRequestStatus;
};

export function LabWorkflowSteps({ current }: LabWorkflowStepsProps) {
  if (current === 'POSTPONED') {
    return (
      <p className="lab-workflow-banner lab-workflow-banner--muted">
        Demande reportée — reprenez le traitement lorsque le médecin la réactive.
      </p>
    );
  }

  const activeIndex = STEPS.indexOf(current);

  return (
    <ol className="lab-workflow-steps" aria-label="Parcours laboratoire">
      {STEPS.map((step, index) => {
        const state =
          index < activeIndex ? 'done' : index === activeIndex ? 'current' : 'upcoming';
        return (
          <li key={step} className={`lab-workflow-steps__item lab-workflow-steps__item--${state}`}>
            <span className="lab-workflow-steps__index" aria-hidden>
              {index + 1}
            </span>
            <span className="lab-workflow-steps__label">{examRequestStatusLabels[step]}</span>
          </li>
        );
      })}
    </ol>
  );
}
