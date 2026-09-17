package com.payhub.subscription.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A subscription tier and its limits. Reference data seeded by Flyway; the code
 * ({@link PlanCode}) is the natural primary key. A limit of {@code -1} means
 * unlimited.
 */
@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "code", length = 32)
    private PlanCode code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "max_projects", nullable = false)
    private int maxProjects;

    @Column(name = "max_seats", nullable = false)
    private int maxSeats;

    @Column(name = "stripe_price_id")
    private String stripePriceId;

    protected Plan() {
        // for JPA
    }

    public Plan(PlanCode code, String name, String description, int priceCents,
                String currency, int maxProjects, int maxSeats) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.priceCents = priceCents;
        this.currency = currency;
        this.maxProjects = maxProjects;
        this.maxSeats = maxSeats;
    }

    public PlanCode getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getPriceCents() {
        return priceCents;
    }

    public String getCurrency() {
        return currency;
    }

    public int getMaxProjects() {
        return maxProjects;
    }

    public int getMaxSeats() {
        return maxSeats;
    }

    public String getStripePriceId() {
        return stripePriceId;
    }
}
