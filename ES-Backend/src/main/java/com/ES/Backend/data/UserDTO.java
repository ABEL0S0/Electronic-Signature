package com.ES.Backend.data;

import com.ES.Backend.entity.User;

public class UserDTO {
        public Long id;
        public String firstName;
        public String lastName;
        public String email;
        public String role;
        public boolean verified;

        public static UserDTO fromUser(User user) {
            UserDTO dto = new UserDTO();
            dto.id = user.getId();
            dto.firstName = user.getFirstName();
            dto.lastName = user.getLastName();
            dto.email = user.getEmail();
            dto.role = user.getRole();
            dto.verified = user.isVerified();
            return dto;
        }
}
