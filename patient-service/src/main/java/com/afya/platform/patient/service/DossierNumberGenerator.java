package com.afya.platform.patient.service;

import com.afya.platform.patient.model.PatientDossierSequence;
import com.afya.platform.patient.repository.PatientDossierSequenceRepository;
import com.afya.platform.patient.repository.PatientRepository;
import com.afya.platform.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class DossierNumberGenerator {

    /** Format : {@code D2026AAAA001} — D + année (4) + bloc lettres (4) + séquence (3). */
    private static final int MAX_SEQUENCE_PER_BLOCK = 999;

    private final PatientDossierSequenceRepository sequenceRepository;
    private final PatientRepository patientRepository;

    public DossierNumberGenerator(
            PatientDossierSequenceRepository sequenceRepository,
            PatientRepository patientRepository
    ) {
        this.sequenceRepository = sequenceRepository;
        this.patientRepository = patientRepository;
    }

    @Transactional
    public String generate() {
        int year = LocalDate.now().getYear();
        PatientDossierSequence sequence = sequenceRepository.findByYearForUpdate(year)
                .orElseGet(() -> {
                    PatientDossierSequence created = new PatientDossierSequence();
                    created.setSequenceYear(year);
                    created.setLetterBlock("AAAA");
                    created.setSequenceNumber(0);
                    return created;
                });

        int next = sequence.getSequenceNumber() + 1;
        String letters = sequence.getLetterBlock();
        if (next > MAX_SEQUENCE_PER_BLOCK) {
            next = 1;
            letters = incrementLetters(letters);
        }
        sequence.setSequenceNumber(next);
        sequence.setLetterBlock(letters);
        sequenceRepository.save(sequence);

        String candidate = formatDossierNumber(year, letters, next);
        if (patientRepository.existsByDossierNumber(candidate)) {
            throw new ConflictException("Collision sur le numéro de dossier : " + candidate);
        }
        return candidate;
    }

    static String formatDossierNumber(int year, String letterBlock, int sequenceNumber) {
        return "D" + year + letterBlock + String.format("%03d", sequenceNumber);
    }

    private static String incrementLetters(String block) {
        char[] chars = block.toCharArray();
        for (int i = chars.length - 1; i >= 0; i--) {
            if (chars[i] < 'Z') {
                chars[i]++;
                return new String(chars);
            }
            chars[i] = 'A';
        }
        return "AAAA";
    }
}
