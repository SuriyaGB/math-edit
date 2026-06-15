package com.gbrit.service;

import com.gbrit.entity.Beneficiary;
import org.springframework.stereotype.Service;

@Service
public interface BeneficiaryService {
    void addBeneficiary(Beneficiary beneficiary, String collectionName, String orgNameWithId, String orgName);
    long generateSequence(String seqName);
}
