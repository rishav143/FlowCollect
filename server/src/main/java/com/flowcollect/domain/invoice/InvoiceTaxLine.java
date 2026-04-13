package com.flowcollect.domain.invoice;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single tax or adjustment line on an invoice (e.g. GST 9%, CGST 9%, VAT 20%).
 * Stores a free-text label and a pre-calculated absolute amount — the percentage
 * is a UI concern only; the server works with amounts.
 */
@Entity
@Table(name = "invoice_tax_lines")
public class InvoiceTaxLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    /** Display label, e.g. "GST 9%", "CGST", "VAT". Max 50 chars. */
    @NotBlank
    @Column(nullable = false, length = 50)
    private String label;

    /** Absolute amount (already calculated by the caller). */
    @NotNull
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected InvoiceTaxLine() {}

    public static InvoiceTaxLine of(Invoice invoice, String label, BigDecimal amount, int sortOrder) {
        InvoiceTaxLine line = new InvoiceTaxLine();
        line.invoice   = invoice;
        line.label     = label.trim();
        line.amount    = amount;
        line.sortOrder = sortOrder;
        return line;
    }

    public UUID getId()          { return id; }
    public String getLabel()     { return label; }
    public BigDecimal getAmount(){ return amount; }
    public int getSortOrder()    { return sortOrder; }
}
