/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.product.bond.BillYieldConvention.DISCOUNT;
import static com.opengamma.strata.product.bond.BillYieldConvention.FRANCE_CD;
import static com.opengamma.strata.product.bond.BillYieldConvention.INTEREST_AT_MATURITY;
import static com.opengamma.strata.product.bond.BillYieldConvention.JAPAN_BILLS;

import java.io.Serializable;
import java.time.LocalDate;

import org.joda.beans.ImmutableBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.SecuritizedProduct;
import com.opengamma.strata.product.SecurityId;
import java.util.Map;
import java.util.NoSuchElementException;
import org.joda.beans.Bean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

/**
 * A bill.
 * <p>
 * A bill is a financial instrument that represents a unique fixed payments.
 * 
 * <h4>Price and yield</h4>
 * Strata uses <i>decimal</i> yields and prices for bills in the trade model, pricers and market data.
 * For example, a price of 99.32% is represented in Strata by 0.9932 and a yield of 1.32% is represented by 0.0132.
 */
@BeanDefinition(constructorScope = "package")
public class Bill
    implements SecuritizedProduct, Resolvable<ResolvedBill>, ImmutableBean, Serializable {

  /**
   * The security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final SecurityId securityId;
  /**
   * The adjustable notional payment of the bill notional, the amount must be positive.
   */
  @PropertyDefinition(validate = "notNull")
  private final AdjustablePayment notional;
  /**
   * The day count convention applicable.
   * <p>
   * The conversion from dates to a numerical value is made based on this day count.
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /**
   * Yield convention.
   * <p>
   * The convention defines how to convert from yield to price and inversely.
   */
  @PropertyDefinition(validate = "notNull")
  private final BillYieldConvention yieldConvention;
  /**
   * The legal entity identifier.
   * <p>
   * This identifier is used for the legal entity that issues the bill.
   */
  @PropertyDefinition(validate = "notNull")
  private final StandardId legalEntityId;
  /**
   * The number of days between valuation date and settlement date.
   * <p>
   * It is usually one business day for US and UK bills and two days for Euroland government bills.
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment settlementDateOffset;

  @ImmutableValidator
  private void validate() {
    ArgChecker.isTrue(settlementDateOffset.getDays() >= 0, "The settlement date offset must be non-negative");
    ArgChecker.isTrue(notional.getAmount() > 0, "Notionanl must be strictly positve");
  }

  @Override
  public Currency getCurrency() {
    return notional.getCurrency();
  }

  /**
   * Computes the price from the yield at a given settlement date.
   * 
   * @param yield  the yield
   * @param settlementDate  the settlement date
   * @return the price
   */
  public double priceFromYield(double yield, LocalDate settlementDate) {
    double accrualFactor = dayCount.relativeYearFraction(settlementDate, notional.getDate().getUnadjusted());
    if (yieldConvention.equals(DISCOUNT)) {
      double price = 1.0d - accrualFactor * yield;
      return price;
    }
    if (yieldConvention.equals(INTEREST_AT_MATURITY) || yieldConvention.equals(FRANCE_CD) || yieldConvention.equals(JAPAN_BILLS)) {
      double price = 1.0d / (1.0d + accrualFactor * yield);
      return price;
    }
    throw new UnsupportedOperationException("The convention " + yieldConvention.name() + " is not supported.");
  }

  /**
   * Computes the yield from the price at a given settlement date.
   * 
   * @param price  the price
   * @param settlementDate  the settlement date
   * @return the yield
   */
  public double yieldFromPrice(double price, LocalDate settlementDate) {
    double accrualFactor = dayCount.relativeYearFraction(settlementDate, notional.getDate().getUnadjusted());
    if (yieldConvention.equals(DISCOUNT)) {
      return (1.0d - price) / accrualFactor;
    }
    if (yieldConvention.equals(INTEREST_AT_MATURITY) || yieldConvention.equals(FRANCE_CD) || yieldConvention.equals(JAPAN_BILLS)) {
      return (1.0d / price - 1) / accrualFactor;
    }
    throw new UnsupportedOperationException("The convention " + yieldConvention.name() + " is not supported.");
  }

  //-------------------------------------------------------------------------
  @Override
  public ResolvedBill resolve(ReferenceData refData) {
    return ResolvedBill.builder()
        .securityId(securityId)
        .notional(notional.resolve(refData))
        .dayCount(dayCount)
        .yieldConvention(yieldConvention)
        .legalEntityId(legalEntityId)
        .settlementDateOffset(settlementDateOffset).build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code Bill}.
   * @return the meta-bean, not null
   */
  public static Bill.Meta meta() {
    return Bill.Meta.INSTANCE;
  }

  static {
    MetaBean.register(Bill.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static Bill.Builder builder() {
    return new Bill.Builder();
  }

  /**
   * Restricted constructor.
   * @param builder  the builder to copy from, not null
   */
  protected Bill(Bill.Builder builder) {
    JodaBeanUtils.notNull(builder.securityId, "securityId");
    JodaBeanUtils.notNull(builder.notional, "notional");
    JodaBeanUtils.notNull(builder.dayCount, "dayCount");
    JodaBeanUtils.notNull(builder.yieldConvention, "yieldConvention");
    JodaBeanUtils.notNull(builder.legalEntityId, "legalEntityId");
    JodaBeanUtils.notNull(builder.settlementDateOffset, "settlementDateOffset");
    this.securityId = builder.securityId;
    this.notional = builder.notional;
    this.dayCount = builder.dayCount;
    this.yieldConvention = builder.yieldConvention;
    this.legalEntityId = builder.legalEntityId;
    this.settlementDateOffset = builder.settlementDateOffset;
    validate();
  }

  @Override
  public Bill.Meta metaBean() {
    return Bill.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   * @return the value of the property, not null
   */
  @Override
  public SecurityId getSecurityId() {
    return securityId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the adjustable notional payment of the bill notional, the amount must be positive.
   * @return the value of the property, not null
   */
  public AdjustablePayment getNotional() {
    return notional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention applicable.
   * <p>
   * The conversion from dates to a numerical value is made based on this day count.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets yield convention.
   * <p>
   * The convention defines how to convert from yield to price and inversely.
   * @return the value of the property, not null
   */
  public BillYieldConvention getYieldConvention() {
    return yieldConvention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the legal entity identifier.
   * <p>
   * This identifier is used for the legal entity that issues the bill.
   * @return the value of the property, not null
   */
  public StandardId getLegalEntityId() {
    return legalEntityId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of days between valuation date and settlement date.
   * <p>
   * It is usually one business day for US and UK bills and two days for Euroland government bills.
   * @return the value of the property, not null
   */
  public DaysAdjustment getSettlementDateOffset() {
    return settlementDateOffset;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      Bill other = (Bill) obj;
      return JodaBeanUtils.equal(securityId, other.securityId) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(yieldConvention, other.yieldConvention) &&
          JodaBeanUtils.equal(legalEntityId, other.legalEntityId) &&
          JodaBeanUtils.equal(settlementDateOffset, other.settlementDateOffset);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(securityId);
    hash = hash * 31 + JodaBeanUtils.hashCode(notional);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(yieldConvention);
    hash = hash * 31 + JodaBeanUtils.hashCode(legalEntityId);
    hash = hash * 31 + JodaBeanUtils.hashCode(settlementDateOffset);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("Bill{");
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  protected void toString(StringBuilder buf) {
    buf.append("securityId").append('=').append(JodaBeanUtils.toString(securityId)).append(',').append(' ');
    buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
    buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
    buf.append("yieldConvention").append('=').append(JodaBeanUtils.toString(yieldConvention)).append(',').append(' ');
    buf.append("legalEntityId").append('=').append(JodaBeanUtils.toString(legalEntityId)).append(',').append(' ');
    buf.append("settlementDateOffset").append('=').append(JodaBeanUtils.toString(settlementDateOffset)).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code Bill}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code securityId} property.
     */
    private final MetaProperty<SecurityId> securityId = DirectMetaProperty.ofImmutable(
        this, "securityId", Bill.class, SecurityId.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<AdjustablePayment> notional = DirectMetaProperty.ofImmutable(
        this, "notional", Bill.class, AdjustablePayment.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", Bill.class, DayCount.class);
    /**
     * The meta-property for the {@code yieldConvention} property.
     */
    private final MetaProperty<BillYieldConvention> yieldConvention = DirectMetaProperty.ofImmutable(
        this, "yieldConvention", Bill.class, BillYieldConvention.class);
    /**
     * The meta-property for the {@code legalEntityId} property.
     */
    private final MetaProperty<StandardId> legalEntityId = DirectMetaProperty.ofImmutable(
        this, "legalEntityId", Bill.class, StandardId.class);
    /**
     * The meta-property for the {@code settlementDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> settlementDateOffset = DirectMetaProperty.ofImmutable(
        this, "settlementDateOffset", Bill.class, DaysAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "securityId",
        "notional",
        "dayCount",
        "yieldConvention",
        "legalEntityId",
        "settlementDateOffset");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          return securityId;
        case 1585636160:  // notional
          return notional;
        case 1905311443:  // dayCount
          return dayCount;
        case -1895216418:  // yieldConvention
          return yieldConvention;
        case 866287159:  // legalEntityId
          return legalEntityId;
        case 135924714:  // settlementDateOffset
          return settlementDateOffset;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public Bill.Builder builder() {
      return new Bill.Builder();
    }

    @Override
    public Class<? extends Bill> beanType() {
      return Bill.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code securityId} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<SecurityId> securityId() {
      return securityId;
    }

    /**
     * The meta-property for the {@code notional} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<AdjustablePayment> notional() {
      return notional;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code yieldConvention} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<BillYieldConvention> yieldConvention() {
      return yieldConvention;
    }

    /**
     * The meta-property for the {@code legalEntityId} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<StandardId> legalEntityId() {
      return legalEntityId;
    }

    /**
     * The meta-property for the {@code settlementDateOffset} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<DaysAdjustment> settlementDateOffset() {
      return settlementDateOffset;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          return ((Bill) bean).getSecurityId();
        case 1585636160:  // notional
          return ((Bill) bean).getNotional();
        case 1905311443:  // dayCount
          return ((Bill) bean).getDayCount();
        case -1895216418:  // yieldConvention
          return ((Bill) bean).getYieldConvention();
        case 866287159:  // legalEntityId
          return ((Bill) bean).getLegalEntityId();
        case 135924714:  // settlementDateOffset
          return ((Bill) bean).getSettlementDateOffset();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code Bill}.
   */
  public static class Builder extends DirectFieldsBeanBuilder<Bill> {

    private SecurityId securityId;
    private AdjustablePayment notional;
    private DayCount dayCount;
    private BillYieldConvention yieldConvention;
    private StandardId legalEntityId;
    private DaysAdjustment settlementDateOffset;

    /**
     * Restricted constructor.
     */
    protected Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    protected Builder(Bill beanToCopy) {
      this.securityId = beanToCopy.getSecurityId();
      this.notional = beanToCopy.getNotional();
      this.dayCount = beanToCopy.getDayCount();
      this.yieldConvention = beanToCopy.getYieldConvention();
      this.legalEntityId = beanToCopy.getLegalEntityId();
      this.settlementDateOffset = beanToCopy.getSettlementDateOffset();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          return securityId;
        case 1585636160:  // notional
          return notional;
        case 1905311443:  // dayCount
          return dayCount;
        case -1895216418:  // yieldConvention
          return yieldConvention;
        case 866287159:  // legalEntityId
          return legalEntityId;
        case 135924714:  // settlementDateOffset
          return settlementDateOffset;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          this.securityId = (SecurityId) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (AdjustablePayment) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case -1895216418:  // yieldConvention
          this.yieldConvention = (BillYieldConvention) newValue;
          break;
        case 866287159:  // legalEntityId
          this.legalEntityId = (StandardId) newValue;
          break;
        case 135924714:  // settlementDateOffset
          this.settlementDateOffset = (DaysAdjustment) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Bill build() {
      return new Bill(this);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the security identifier.
     * <p>
     * This identifier uniquely identifies the security within the system.
     * @param securityId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder securityId(SecurityId securityId) {
      JodaBeanUtils.notNull(securityId, "securityId");
      this.securityId = securityId;
      return this;
    }

    /**
     * Sets the adjustable notional payment of the bill notional, the amount must be positive.
     * @param notional  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder notional(AdjustablePayment notional) {
      JodaBeanUtils.notNull(notional, "notional");
      this.notional = notional;
      return this;
    }

    /**
     * Sets the day count convention applicable.
     * <p>
     * The conversion from dates to a numerical value is made based on this day count.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets yield convention.
     * <p>
     * The convention defines how to convert from yield to price and inversely.
     * @param yieldConvention  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder yieldConvention(BillYieldConvention yieldConvention) {
      JodaBeanUtils.notNull(yieldConvention, "yieldConvention");
      this.yieldConvention = yieldConvention;
      return this;
    }

    /**
     * Sets the legal entity identifier.
     * <p>
     * This identifier is used for the legal entity that issues the bill.
     * @param legalEntityId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder legalEntityId(StandardId legalEntityId) {
      JodaBeanUtils.notNull(legalEntityId, "legalEntityId");
      this.legalEntityId = legalEntityId;
      return this;
    }

    /**
     * Sets the number of days between valuation date and settlement date.
     * <p>
     * It is usually one business day for US and UK bills and two days for Euroland government bills.
     * @param settlementDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder settlementDateOffset(DaysAdjustment settlementDateOffset) {
      JodaBeanUtils.notNull(settlementDateOffset, "settlementDateOffset");
      this.settlementDateOffset = settlementDateOffset;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("Bill.Builder{");
      int len = buf.length();
      toString(buf);
      if (buf.length() > len) {
        buf.setLength(buf.length() - 2);
      }
      buf.append('}');
      return buf.toString();
    }

    protected void toString(StringBuilder buf) {
      buf.append("securityId").append('=').append(JodaBeanUtils.toString(securityId)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("yieldConvention").append('=').append(JodaBeanUtils.toString(yieldConvention)).append(',').append(' ');
      buf.append("legalEntityId").append('=').append(JodaBeanUtils.toString(legalEntityId)).append(',').append(' ');
      buf.append("settlementDateOffset").append('=').append(JodaBeanUtils.toString(settlementDateOffset)).append(',').append(' ');
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
