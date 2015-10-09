package org.meveo.model.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class InvoiceCategoryDTO
{
  private String description;
  private String code;
  private BigDecimal amountWithoutTax = BigDecimal.ZERO;
  private BigDecimal amountWithTax = BigDecimal.ZERO;
  LinkedHashMap<String, InvoiceSubCategoryDTO> invoiceSubCategoryDTOMap = new LinkedHashMap<String, InvoiceSubCategoryDTO>();
  
  public String getDescription()
  {
    return this.description;
  }
  
  public void setDescription(String description)
  {
    this.description = description;
  }
  
  public String getCode()
  {
    return this.code;
  }
  
  public void setCode(String code)
  {
    this.code = code;
  }
  
  public BigDecimal getAmountWithoutTax()
  {
    return this.amountWithoutTax.setScale(2, RoundingMode.HALF_UP);
  }
  
  public void setAmountWithoutTax(BigDecimal amountWithoutTax)
  {
    this.amountWithoutTax = amountWithoutTax;
  }
  
  public LinkedHashMap<String, InvoiceSubCategoryDTO> getInvoiceSubCategoryDTOMap()
  {
    return this.invoiceSubCategoryDTOMap;
  }
  
  public void setInvoiceSubCategoryDTOMap(LinkedHashMap<String, InvoiceSubCategoryDTO> invoiceSubCategoryDTOMap)
  {
    this.invoiceSubCategoryDTOMap = invoiceSubCategoryDTOMap;
  }
  
  public List<InvoiceSubCategoryDTO> getInvoiceSubCategoryDTOList()
  {
    return new ArrayList<InvoiceSubCategoryDTO>(this.invoiceSubCategoryDTOMap.values());
  }
  
  public BigDecimal getAmountWithTax()
  {
    return this.amountWithTax;
  }
  
  public void setAmountWithTax(BigDecimal amountWithTax)
  {
    this.amountWithTax = amountWithTax;
  }
  
  public void addAmountWithTax(BigDecimal amountToAdd)
  {
    if (amountToAdd != null)
    {
      if (this.amountWithTax == null) {
        this.amountWithTax = new BigDecimal("0");
      }
      this.amountWithTax = this.amountWithTax.add(amountToAdd);
    }
  }
  
  public void addAmountWithoutTax(BigDecimal amountToAdd)
  {
    if (this.amountWithoutTax == null) {
      this.amountWithoutTax = new BigDecimal("0");
    }
    this.amountWithoutTax = this.amountWithoutTax.add(amountToAdd);
  }
}
