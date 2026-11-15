package com.borae.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class MenuProductAddon {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "price_delta_in_cents", nullable = false)
    private long priceDeltaInCents;

    @Column(nullable = false)
    private boolean active;

    protected MenuProductAddon() {
    }

    public MenuProductAddon(String name, long priceDeltaInCents, boolean active) {
        this.name = name;
        this.priceDeltaInCents = priceDeltaInCents;
        this.active = active;
    }

    public String getName() { return name; }
    public long getPriceDeltaInCents() { return priceDeltaInCents; }
    public boolean isActive() { return active; }

    public void update(String name, long priceDeltaInCents, boolean active) {
        this.name = name;
        this.priceDeltaInCents = priceDeltaInCents;
        this.active = active;
    }
}
