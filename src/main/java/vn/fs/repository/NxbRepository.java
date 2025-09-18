package vn.fs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.fs.entities.NXB;

import javax.persistence.Id;

@Repository
public interface NxbRepository extends JpaRepository<NXB, Long> {
}
