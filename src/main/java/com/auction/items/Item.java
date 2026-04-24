package com.auction.items;

import com.auction.users.User;
import jakarta.persistence.*;

@Entity
@Table(name = "items")
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @ManyToOne // One seller many Item
    @JoinColumn(name = "seller_username") // JoinColumn annotation creates a foreign key column
    private User user;

    @Column(name = "title", nullable = false)
    private String title;

    public Item() {
    }

    public Item(User user, String title) {
        this.user = user;
        this.title = title;
    }

    public Long getItemId() {
        return itemId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
