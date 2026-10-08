package com.truongtho.be.entity;


import com.truongtho.be.enums.TableStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@jakarta.persistence.Table(name = "dining_tables")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiningTable {
    @Id
            @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String name;
    Integer position;
    Integer capacity;

    @Enumerated(EnumType.STRING)
    TableStatus tableStatus;
}
