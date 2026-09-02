package com.joyboy.polaris_digitech.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Item name is required")
    @Pattern(
            regexp = "^[a-zA-Z0-9_-]+$",
            message = "Name can only contain letters, numbers, hyphen and underscore"
    )
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Weight is required")
    @Min(value = 1, message = "Weight must be at least 1gr")
    @Column(nullable = false)
    private Integer weight;

    @NotBlank(message = "Item code is required")
    @Pattern(
            regexp = "^[A-Z0-9_]+$",
            message = "Code can only contain uppercase letters, numbers and underscore"
    )
    @Column(nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_id")
    private Box box;
}