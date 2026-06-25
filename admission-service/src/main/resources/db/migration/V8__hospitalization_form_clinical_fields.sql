ALTER TABLE hospitalization_forms ADD COLUMN antecedents_text TEXT;
ALTER TABLE hospitalization_forms ADD COLUMN anamnesis_text TEXT;
ALTER TABLE hospitalization_forms ADD COLUMN physical_exam_pulmonary_text TEXT;
ALTER TABLE hospitalization_forms ADD COLUMN physical_exam_cardiac_text TEXT;
ALTER TABLE hospitalization_forms ADD COLUMN physical_exam_abdominal_text TEXT;
ALTER TABLE hospitalization_forms ADD COLUMN physical_exam_neurological_text TEXT;
ALTER TABLE hospitalization_forms ADD COLUMN physical_exam_misc_text TEXT;
ALTER TABLE hospitalization_forms ADD COLUMN paraclinical_text TEXT;
ALTER TABLE hospitalization_forms ADD COLUMN conclusion_text TEXT;

UPDATE hospitalization_forms SET anamnesis_text = history_text WHERE anamnesis_text IS NULL AND history_text IS NOT NULL;
UPDATE hospitalization_forms SET antecedents_text = allergies WHERE antecedents_text IS NULL AND allergies IS NOT NULL;
