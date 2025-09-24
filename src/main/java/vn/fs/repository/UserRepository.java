package vn.fs.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.fs.entities.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /* ====== Compat cho code cũ ====== */
    User findByEmail(String email);
    User findByUsername(String username);
    boolean existsByEmailIgnoreCaseAndUserIdNot(String email, Long userId);
    boolean existsByUsernameIgnoreCaseAndUserIdNot(String username, Long userId);

    /* ====== Khuyến nghị dùng Optional/IgnoreCase ====== */
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    /* Kéo roles kèm theo để tránh N+1 */
    @Override
    @EntityGraph(attributePaths = "roles")
    List<User> findAll();

    @Override
    @EntityGraph(attributePaths = "roles")
    Optional<User> findById(Long id);

    /* ====== Field-level updates: không chạm entity => không trigger Bean Validation ====== */

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u " +
            "   set u.failedAttempt = 0, " +
            "       u.lockedUntil   = null, " +
            "       u.lastLoginAt   = :now " +
            " where lower(u.username) = lower(:login) " +
            "    or lower(u.email)    = lower(:login)")
    int resetLoginState(@Param("login") String login,
                        @Param("now")   LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u " +
            "   set u.failedAttempt = u.failedAttempt + 1 " +
            " where (lower(u.username) = lower(:login) " +
            "    or lower(u.email)    = lower(:login)) " +
            "   and (u.lockedUntil is null or u.lockedUntil < :now)")
    int bumpFailureCounter(@Param("login") String login,
                           @Param("now")   LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u " +
            "   set u.lockedUntil = :lockUntil " +
            " where (lower(u.username) = lower(:login) " +
            "    or lower(u.email)    = lower(:login)) " +
            "   and (u.lockedUntil is null or u.lockedUntil < :now) " +
            "   and u.failedAttempt >= :maxFail")
    int applyLockIfExceeded(@Param("login")     String login,
                            @Param("maxFail")   int maxFail,
                            @Param("lockUntil") LocalDateTime lockUntil,
                            @Param("now")       LocalDateTime now);

    /* Đổi mật khẩu & bật status bằng email — không validate toàn entity */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u set u.password = :hashed, u.status = true where lower(u.email) = lower(:email)")
    int updatePasswordByEmail(@Param("email") String email,
                              @Param("hashed") String hashed);

    /* ====== Thống kê user mới theo ngày (dùng backtick cho bảng `user`) ====== */

    @Query(value =
            "select date(u.register_date) d, count(*) v " +
                    "from `user` u " +
                    "where u.register_date >= :from " +
                    "group by date(u.register_date) " +
                    "order by d", nativeQuery = true)
    List<Object[]> newUsersByDateFrom(@Param("from") LocalDate from);

    @Query(value =
            "select count(*) " +
                    "from `user` u " +
                    "where u.register_date >= :from", nativeQuery = true)
    long countNewUsersFrom(@Param("from") LocalDate from);
}
