package com.college.icrs.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RegisterUserDto {

    private String username;     // Full name
    private String studentId;    // Student ID
    private String department;   // Department (e.g., IT, CSE, ME)
    private String email;        // Email used for login
    private String password;     // Password
}
