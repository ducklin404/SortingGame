package group10.common;

import lombok.*;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {
    private java.util.UUID id;
    private String username;
    private String displayName;

    @ToString.Exclude
    private String passwordHash;

    private Timestamp createdAt;
    private Timestamp lastLogin;
}
