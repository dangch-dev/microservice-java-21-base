package pl.co.identity.mapper;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import pl.co.identity.dto.AdminUserResponse;
import pl.co.identity.dto.ProfileResponse;
import pl.co.identity.entity.User;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-12-19T21:19:52+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public ProfileResponse toProfile(User user) {
        if ( user == null ) {
            return null;
        }

        ProfileResponse.ProfileResponseBuilder profileResponse = ProfileResponse.builder();

        profileResponse.id( user.getId() );
        profileResponse.email( user.getEmail() );
        profileResponse.fullName( user.getFullName() );
        profileResponse.phoneNumber( user.getPhoneNumber() );
        profileResponse.avatarUrl( user.getAvatarUrl() );
        profileResponse.address( user.getAddress() );
        profileResponse.status( user.getStatus() );

        profileResponse.roles( toRoleNames(user.getRoles()) );

        return profileResponse.build();
    }

    @Override
    public AdminUserResponse toAdmin(User user) {
        if ( user == null ) {
            return null;
        }

        AdminUserResponse.AdminUserResponseBuilder adminUserResponse = AdminUserResponse.builder();

        adminUserResponse.id( user.getId() );
        adminUserResponse.email( user.getEmail() );
        adminUserResponse.fullName( user.getFullName() );
        adminUserResponse.phoneNumber( user.getPhoneNumber() );
        adminUserResponse.avatarUrl( user.getAvatarUrl() );
        adminUserResponse.address( user.getAddress() );
        adminUserResponse.status( user.getStatus() );

        adminUserResponse.roles( toRoleNames(user.getRoles()) );

        return adminUserResponse.build();
    }
}
