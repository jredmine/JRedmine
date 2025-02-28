package com.redmine.jredmine.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String login;

    @Column(name = "hashed_password")
    private String hashedPassword;

    @Column(nullable = false)
    private String firstname;

    @Column(nullable = false)
    private String lastname;

    private Boolean admin = false;

    private Integer status = 1;

    @Column(name = "last_login_on")
    private Date lastLoginOn;

    private String language = "";

    @Column(name = "auth_source_id")
    private Integer authSourceId;

    @Column(name = "created_on")
    private Date createdOn;

    @Column(name = "updated_on")
    private Date updatedOn;

    private String type;

    @Column(name = "mail_notification")
    private String mailNotification = "";

    private String salt;

    @Column(name = "must_change_passwd")
    private Boolean mustChangePasswd = false;

    @Column(name = "passwd_changed_on")
    private Date passwdChangedOn;

    private String twofaScheme;

    private String twofaTotpKey;

    @Column(name = "twofa_totp_last_used_at")
    private Integer twofaTotpLastUsedAt;

    @Column(name = "twofa_required")
    private Boolean twofaRequired = false;
}
