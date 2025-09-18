package vn.fs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.fs.entities.LoginIpLock;

import java.util.Optional;

public interface LoginIpLockRepository extends JpaRepository<LoginIpLock, Long> {
    Optional<LoginIpLock> findByIp(String ip);
}
