package com.borae.catalog;

import com.borae.identity.RestaurantMedia;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "menu_product")
public class MenuProduct {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private MenuCategory category;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "price_in_cents", nullable = false)
    private long priceInCents;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 16)
    private ProductLifecycleStatus lifecycleStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProductAvailability availability;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinTable(name = "menu_product_image", joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "media_id"))
    private RestaurantMedia image;

    @ElementCollection
    @CollectionTable(name = "menu_product_ingredient", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "display_order")
    @Column(name = "ingredient", nullable = false, length = 120)
    private List<String> ingredients = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "menu_product_addon", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "display_order")
    @Embedded
    private List<MenuProductAddon> addons = new ArrayList<>();

    protected MenuProduct() {
    }

    public MenuProduct(MenuCategory category, String name, String description, long priceInCents,
                       int displayOrder, List<String> ingredients, List<MenuProductAddon> addons) {
        this.id = UUID.randomUUID();
        this.category = category;
        this.name = name;
        this.description = description;
        this.priceInCents = priceInCents;
        this.lifecycleStatus = ProductLifecycleStatus.ACTIVE;
        this.availability = ProductAvailability.AVAILABLE;
        this.displayOrder = displayOrder;
        this.ingredients = new ArrayList<>(ingredients);
        this.addons = new ArrayList<>(addons);
    }

    public UUID getId() { return id; }
    public MenuCategory getCategory() { return category; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public long getPriceInCents() { return priceInCents; }
    public ProductLifecycleStatus getLifecycleStatus() { return lifecycleStatus; }
    public ProductAvailability getAvailability() { return availability; }
    public int getDisplayOrder() { return displayOrder; }
    public List<String> getIngredients() { return List.copyOf(ingredients); }
    public List<MenuProductAddon> getAddons() { return List.copyOf(addons); }
    public RestaurantMedia getImage() { return image; }

    public void setImage(RestaurantMedia image) { this.image = image; }

    public void update(MenuCategory category, String name, String description, long priceInCents,
                       int displayOrder, List<String> ingredients, List<MenuProductAddon> addons) {
        ensureActive();
        this.category = category;
        this.name = name;
        this.description = description;
        this.priceInCents = priceInCents;
        this.displayOrder = displayOrder;
        this.ingredients.clear();
        this.ingredients.addAll(ingredients);
        this.addons.clear();
        this.addons.addAll(addons);
    }

    public void changeAvailability(ProductAvailability availability) {
        ensureActive();
        if (availability == null) {
            throw new IllegalArgumentException("A disponibilidade do produto é obrigatória.");
        }
        this.availability = availability;
    }

    public void archive() {
        this.lifecycleStatus = ProductLifecycleStatus.ARCHIVED;
    }

    public void restore() {
        this.lifecycleStatus = ProductLifecycleStatus.ACTIVE;
    }

    public void changeDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    private void ensureActive() {
        if (lifecycleStatus == ProductLifecycleStatus.ARCHIVED) {
            throw new IllegalStateException("Produto arquivado não pode ser editado.");
        }
    }
}
