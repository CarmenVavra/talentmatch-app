package de.talentmatch.auth.api;

import de.talentmatch.auth.Role;
import de.talentmatch.auth.UserAccount;

public record UserDto(Long id, String email, String firstName, String lastName, String department, Role role) {

    public static UserDto from(UserAccount user) {
        return new UserDto(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getDepartment(), user.getRole());
    }
}
