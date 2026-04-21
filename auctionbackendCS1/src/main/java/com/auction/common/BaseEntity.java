package com.auction.common;

import java.util.UUID;

import jakarta.persistence.*;

@Entity
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    public UUID getId() {
        return id;
    }
}
