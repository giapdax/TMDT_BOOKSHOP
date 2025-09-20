package vn.fs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.fs.entities.NXB;

import java.util.List;

@Repository
public interface NxbRepository extends JpaRepository<NXB, Long> {

    /* Menu: chỉ NXB active và có ít nhất 1 sản phẩm active */
    @Query(value =
            "SELECT n.* FROM nxb n " +
                    "WHERE n.status = 1 " +
                    "  AND EXISTS (SELECT 1 FROM products p WHERE p.nxb_id = n.id AND p.status = 1) " +
                    "ORDER BY n.name",
            nativeQuery = true)
    List<NXB> findActiveForMenu();

    /* Đọc thô theo id */
    @Query(value = "SELECT * FROM nxb WHERE id = ?1", nativeQuery = true)
    NXB findRaw(Long id);
}
