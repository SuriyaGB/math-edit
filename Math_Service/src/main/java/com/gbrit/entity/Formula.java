package com.gbrit.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Formula {
	@Id
	private long id;
	private String formula;
	private String formulaName;
	private String formulaUrl;
	private String tinyUrl;

	private String createdBy;
	private Date createdDate;
	private String updatedBy;
	private Date updatedDate;
	private Date deletedDate;
	private boolean isDeleted;

	public Formula(String formula, String formulaName, String formulaUrl, String tinyUrl) {
		this.formula = formula;
		this.formulaName = formulaName;
		this.formulaUrl = formulaUrl;
		this.tinyUrl = tinyUrl;
	}
}
