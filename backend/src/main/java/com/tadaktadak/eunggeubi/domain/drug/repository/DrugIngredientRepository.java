package com.tadaktadak.eunggeubi.domain.drug.repository;

import com.tadaktadak.eunggeubi.domain.drug.entity.DrugIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DrugIngredientRepository extends JpaRepository<DrugIngredient, Long> {
}