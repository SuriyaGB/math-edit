package com.gbrit.service;

import com.gbrit.entity.Formula;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public interface FormulaService {
    void addFormulas(Formula product, String collectionName);
    Formula updateFormulas(Formula product, String userName, String collectionName);
    long generateSequence(String sequenceName);
    List<Formula> getFormulas(String orgNameWithId);
}
