package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.sql.PreparedStatement;

/**
 * Обновляет password_hash у всех пользователей на корректный BCrypt-хеш для пароля "password123".
 * Решает проблему входа "Неверный логин или пароль" из-за неверного хеша в seed (V1).
 */
public class V3__Fix_passwords extends BaseJavaMigration {

    private static final String PASSWORD = "password123";
    private static final int BCRYPT_ROUNDS = 12;

    @Override
    public void migrate(Context context) throws Exception {
        String hash = BCrypt.hashpw(PASSWORD, BCrypt.gensalt(BCRYPT_ROUNDS));
        String sql = "UPDATE users SET password_hash = ?";
        try (PreparedStatement ps = context.getConnection().prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.executeUpdate();
        }
    }
}
