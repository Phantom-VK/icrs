package com.college.icrs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // 👇 Full name (used for display only, not authentication)
    @JsonIgnore // hide this field itself; expose through getDisplayName() instead
    @Column(nullable = false)
    private String username;

    // 👇 Login identity
    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    @JsonIgnore // never send passwords in responses
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @JsonIgnore
    private String verificationCode;

    @JsonIgnore
    private LocalDateTime verificationCodeExpiresAt;

    private boolean enabled;

    private String department;
    private String studentId;

    // Constructor for convenience
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // ✅ Custom getter for frontend JSON serialization
    // Jackson will serialize this as "username" in the response
    @JsonProperty("username")
    public String getDisplayName() {
        return this.username;
    }

    // ✅ Spring Security uses this for authentication (email-based)
    @Override
    @JsonIgnore
    public String getUsername() {
        return this.email;
    }

    // ✅ Security framework methods
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
