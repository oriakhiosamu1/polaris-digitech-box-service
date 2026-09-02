package com.joyboy.polaris_digitech.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boxes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Box {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "txref is required")
    @Size(max = 20, message = "txref must not exceed 20 characters")
    @Column(nullable = false, unique = true, length = 20)
    private String txref;

    @NotNull(message = "weightLimit is required")
    @Min(value = 1, message = "weightLimit must be at least 1gr")
    @Max(value = 500, message = "weightLimit must not exceed 500gr")
    @Column(nullable = false)
    private Integer weightLimit;

    @NotNull(message = "batteryCapacity is required")
    @Min(value = 0, message = "batteryCapacity cannot be less than 0")
    @Max(value = 100, message = "batteryCapacity cannot be more than 100")
    @Column(nullable = false)
    private Integer batteryCapacity;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoxState state;

    @OneToMany(mappedBy = "box", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Item> items = new ArrayList<>();

    // Helper method to add item
    public void addItem(Item item) {
        items.add(item);
        item.setBox(this);
    }

    // Helper method to calculate current loaded weight
    public int getCurrentWeight() {
        return items.stream()
                .mapToInt(Item::getWeight)
                .sum();
    }
}